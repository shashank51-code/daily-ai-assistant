package com.dailyai.assistant.service;

import com.dailyai.assistant.config.AppProperties;
import com.dailyai.assistant.dto.DailyPlanDto;
import com.dailyai.assistant.dto.DailyTaskDto;
import com.dailyai.assistant.dto.LearningTopicDto;
import com.dailyai.assistant.dto.ProgressDto;
import com.dailyai.assistant.dto.ProgressRequest;
import com.dailyai.assistant.dto.RoadmapDto;
import com.dailyai.assistant.entity.DailyTask;
import com.dailyai.assistant.entity.DailyTaskProgress;
import com.dailyai.assistant.entity.LearningProgress;
import com.dailyai.assistant.entity.LearningTopic;
import com.dailyai.assistant.entity.User;
import com.dailyai.assistant.exception.ResourceNotFoundException;
import com.dailyai.assistant.repository.DailyTaskProgressRepository;
import com.dailyai.assistant.repository.DailyTaskRepository;
import com.dailyai.assistant.repository.LearningProgressRepository;
import com.dailyai.assistant.repository.LearningTopicRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

@Service
public class LearningService {

    private final LearningTopicRepository topicRepository;
    private final LearningProgressRepository progressRepository;
    private final DailyTaskRepository dailyTaskRepository;
    private final DailyTaskProgressRepository dailyTaskProgressRepository;
    private final CurrentUserService currentUserService;
    private final AppProperties properties;

    public LearningService(LearningTopicRepository topicRepository,
                           LearningProgressRepository progressRepository,
                           DailyTaskRepository dailyTaskRepository,
                           DailyTaskProgressRepository dailyTaskProgressRepository,
                           CurrentUserService currentUserService,
                           AppProperties properties) {
        this.topicRepository = topicRepository;
        this.progressRepository = progressRepository;
        this.dailyTaskRepository = dailyTaskRepository;
        this.dailyTaskProgressRepository = dailyTaskProgressRepository;
        this.currentUserService = currentUserService;
        this.properties = properties;
    }

    @Transactional(readOnly = true)
    public RoadmapDto roadmap() {
        List<LearningTopic> topics = topicRepository.findAllByOrderByLevelAscOrderIndexAsc();
        Map<Long, LearningProgress> progress = progressMap();

        Map<Integer, List<LearningTopic>> byLevel = new TreeMap<>();
        for (LearningTopic topic : topics) {
            byLevel.computeIfAbsent(topic.getLevel(), key -> new ArrayList<>()).add(topic);
        }

        List<RoadmapDto.Level> levels = new ArrayList<>();
        int totalCompleted = 0;
        for (Map.Entry<Integer, List<LearningTopic>> entry : byLevel.entrySet()) {
            List<LearningTopicDto> topicDtos = entry.getValue().stream()
                    .map(topic -> LearningTopicDto.from(topic, progress.get(topic.getId())))
                    .toList();
            int completed = (int) topicDtos.stream().filter(t -> "COMPLETED".equals(t.status())).count();
            totalCompleted += completed;
            levels.add(new RoadmapDto.Level(entry.getKey(), entry.getValue().get(0).getLevelName(),
                    topicDtos, completed, topicDtos.size()));
        }

        int total = topics.size();
        int percent = total == 0 ? 0 : Math.round(totalCompleted * 100f / total);
        return new RoadmapDto(levels, totalCompleted, total, percent);
    }

    @Transactional(readOnly = true)
    public List<LearningTopicDto> topics(Integer level) {
        Map<Long, LearningProgress> progress = progressMap();
        List<LearningTopic> topics = level == null
                ? topicRepository.findAllByOrderByLevelAscOrderIndexAsc()
                : topicRepository.findByLevelOrderByOrderIndexAsc(level);
        return topics.stream()
                .map(topic -> LearningTopicDto.from(topic, progress.get(topic.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public LearningTopicDto topic(Long id) {
        LearningTopic topic = topicRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Learning topic " + id + " was not found"));
        LearningProgress progress = progressRepository
                .findByUserIdAndTopicId(currentUserService.getCurrentUserId(), id)
                .orElse(null);
        return LearningTopicDto.from(topic, progress);
    }

    @Transactional
    public ProgressDto updateProgress(ProgressRequest request) {
        User user = currentUserService.getCurrentUser();
        LearningTopic topic = topicRepository.findById(request.topicId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Learning topic " + request.topicId() + " was not found"));

        LearningProgress progress = progressRepository
                .findByUserIdAndTopicId(user.getId(), topic.getId())
                .orElseGet(() -> {
                    LearningProgress created = new LearningProgress();
                    created.setUser(user);
                    created.setTopic(topic);
                    return created;
                });

        progress.setStatus(request.status());
        progress.setUpdatedAt(Instant.now());
        if (request.status() == LearningProgress.Status.COMPLETED) {
            if (progress.getCompletedAt() == null) {
                progress.setCompletedAt(Instant.now());
            }
        } else {
            progress.setCompletedAt(null);
        }
        LearningProgress saved = progressRepository.save(progress);

        return new ProgressDto(topic.getId(), topic.getTitle(), saved.getStatus().name(),
                saved.getCompletedAt(), saved.getUpdatedAt());
    }

    @Transactional(readOnly = true)
    public List<ProgressDto> progress() {
        Long userId = currentUserService.getCurrentUserId();
        return progressRepository.findByUserId(userId).stream()
                .sorted(Comparator.comparing(LearningProgress::getUpdatedAt).reversed())
                .map(progress -> new ProgressDto(
                        progress.getTopic().getId(),
                        progress.getTopic().getTitle(),
                        progress.getStatus().name(),
                        progress.getCompletedAt(),
                        progress.getUpdatedAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public DailyPlanDto dailyPlan() {
        List<DailyTask> tasks = dailyTaskRepository.findAllByOrderByDayNumberAscOrderIndexAsc();
        Set<Long> completed = completedTaskIds();

        Map<Integer, List<DailyTask>> byDay = new LinkedHashMap<>();
        for (DailyTask task : tasks) {
            byDay.computeIfAbsent(task.getDayNumber(), key -> new ArrayList<>()).add(task);
        }

        List<DailyPlanDto.Day> days = new ArrayList<>();
        int completedTotal = 0;
        for (Map.Entry<Integer, List<DailyTask>> entry : byDay.entrySet()) {
            List<DailyTaskDto> taskDtos = entry.getValue().stream()
                    .map(task -> DailyTaskDto.from(task, completed.contains(task.getId())))
                    .toList();
            int completedInDay = (int) taskDtos.stream().filter(DailyTaskDto::completedInPlan).count();
            completedTotal += completedInDay;
            days.add(new DailyPlanDto.Day(entry.getKey(), taskDtos, completedInDay, taskDtos.size()));
        }

        int total = tasks.size();
        int percent = total == 0 ? 0 : Math.round(completedTotal * 100f / total);
        return new DailyPlanDto(days, completedTotal, total, percent);
    }

    @Transactional
    public DailyTaskDto completeDailyTask(Long taskId, boolean completed) {
        User user = currentUserService.getCurrentUser();
        DailyTask task = dailyTaskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Daily task " + taskId + " was not found"));

        if (completed) {
            dailyTaskProgressRepository.findByUserIdAndTaskId(user.getId(), taskId)
                    .orElseGet(() -> {
                        DailyTaskProgress progress = new DailyTaskProgress();
                        progress.setUser(user);
                        progress.setTask(task);
                        progress.setCompletedAt(Instant.now());
                        return dailyTaskProgressRepository.save(progress);
                    });
        } else {
            dailyTaskProgressRepository.deleteByUserIdAndTaskId(user.getId(), taskId);
        }
        return DailyTaskDto.from(task, completed);
    }

    @Transactional(readOnly = true)
    public DailyTaskDto todaysTask() {
        List<DailyTask> tasks = dailyTaskRepository.findAllByOrderByDayNumberAscOrderIndexAsc();
        if (tasks.isEmpty()) {
            return null;
        }
        Set<Long> completed = completedTaskIds();
        DailyTask selected = tasks.stream()
                .filter(task -> !completed.contains(task.getId()))
                .findFirst()
                .orElse(tasks.get(tasks.size() - 1));
        return DailyTaskDto.from(selected, completed.contains(selected.getId()));
    }

    @Transactional(readOnly = true)
    public LearningTopicDto continueLearning() {
        Long userId = currentUserService.getCurrentUserId();
        Map<Long, LearningProgress> progress = progressMap();
        List<LearningTopic> topics = topicRepository.findAllByOrderByLevelAscOrderIndexAsc();

        Optional<LearningTopic> inProgress = progress.values().stream()
                .filter(p -> p.getStatus() == LearningProgress.Status.IN_PROGRESS)
                .sorted(Comparator.comparing(LearningProgress::getUpdatedAt).reversed())
                .map(LearningProgress::getTopic)
                .findFirst();
        if (inProgress.isPresent()) {
            return LearningTopicDto.from(inProgress.get(), progress.get(inProgress.get().getId()));
        }

        for (LearningTopic topic : topics) {
            LearningProgress row = progress.get(topic.getId());
            if (row == null || row.getStatus() != LearningProgress.Status.COMPLETED) {
                return LearningTopicDto.from(topic, row);
            }
        }
        return null;
    }

    /** Consecutive days (Asia/Kolkata) with at least one completed topic or daily task. */
    @Transactional(readOnly = true)
    public int streakDays() {
        Long userId = currentUserService.getCurrentUserId();
        ZoneId zone = ZoneId.of(properties.getTimezone());
        Set<LocalDate> activeDays = new HashSet<>();

        progressRepository.findByUserIdAndCompletedAtIsNotNullAndCompletedAtAfter(
                        userId, Instant.now().minusSeconds(60L * 60 * 24 * 365))
                .forEach(progress -> activeDays.add(progress.getCompletedAt().atZone(zone).toLocalDate()));
        dailyTaskProgressRepository.findByUserId(userId)
                .forEach(progress -> activeDays.add(progress.getCompletedAt().atZone(zone).toLocalDate()));

        if (activeDays.isEmpty()) {
            return 0;
        }
        LocalDate cursor = LocalDate.now(zone);
        if (!activeDays.contains(cursor)) {
            cursor = cursor.minusDays(1);
        }
        int streak = 0;
        while (activeDays.contains(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }
        return streak;
    }

    @Transactional(readOnly = true)
    public int planProgressPercent() {
        return dailyPlan().progressPercent();
    }

    private Map<Long, LearningProgress> progressMap() {
        Map<Long, LearningProgress> map = new LinkedHashMap<>();
        progressRepository.findByUserId(currentUserService.getCurrentUserId())
                .forEach(progress -> map.put(progress.getTopic().getId(), progress));
        return map;
    }

    private Set<Long> completedTaskIds() {
        Set<Long> ids = new HashSet<>();
        dailyTaskProgressRepository.findByUserId(currentUserService.getCurrentUserId())
                .forEach(progress -> ids.add(progress.getTask().getId()));
        return ids;
    }
}
