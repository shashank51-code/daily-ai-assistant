package com.dailyai.assistant.service;

import com.dailyai.assistant.config.AppProperties;
import com.dailyai.assistant.dto.ProgressDto;
import com.dailyai.assistant.dto.ProgressRequest;
import com.dailyai.assistant.entity.LearningProgress;
import com.dailyai.assistant.entity.LearningTopic;
import com.dailyai.assistant.entity.User;
import com.dailyai.assistant.repository.DailyTaskProgressRepository;
import com.dailyai.assistant.repository.DailyTaskRepository;
import com.dailyai.assistant.repository.LearningProgressRepository;
import com.dailyai.assistant.repository.LearningTopicRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LearningServiceTest {

    @Mock
    private LearningTopicRepository topicRepository;
    @Mock
    private LearningProgressRepository progressRepository;
    @Mock
    private DailyTaskRepository dailyTaskRepository;
    @Mock
    private DailyTaskProgressRepository dailyTaskProgressRepository;
    @Mock
    private CurrentUserService currentUserService;

    private LearningService service;
    private User user;
    private LearningTopic topic;

    @BeforeEach
    void setUp() {
        service = new LearningService(topicRepository, progressRepository, dailyTaskRepository,
                dailyTaskProgressRepository, currentUserService, new AppProperties());

        user = new User("Fresher", "fresher@dailyai.local");
        user.setId(1L);

        topic = new LearningTopic();
        topic.setId(5L);
        topic.setTitle("Introduction to AI");
        topic.setLevel(1);
        topic.setLevelName("Fundamentals");
        topic.setOrderIndex(1);
    }

    @Test
    void completingATopicSetsCompletedAt() {
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(topicRepository.findById(5L)).thenReturn(Optional.of(topic));
        when(progressRepository.findByUserIdAndTopicId(1L, 5L)).thenReturn(Optional.empty());
        when(progressRepository.save(any(LearningProgress.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProgressDto result = service.updateProgress(new ProgressRequest(5L, LearningProgress.Status.COMPLETED));

        assertThat(result.status()).isEqualTo("COMPLETED");
        assertThat(result.completedAt()).isNotNull();
        assertThat(result.topicTitle()).isEqualTo("Introduction to AI");
    }

    @Test
    void reopeningACompletedTopicClearsCompletedAt() {
        LearningProgress existing = new LearningProgress();
        existing.setUser(user);
        existing.setTopic(topic);
        existing.setStatus(LearningProgress.Status.COMPLETED);
        existing.setCompletedAt(java.time.Instant.now());

        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(topicRepository.findById(5L)).thenReturn(Optional.of(topic));
        when(progressRepository.findByUserIdAndTopicId(1L, 5L)).thenReturn(Optional.of(existing));
        when(progressRepository.save(any(LearningProgress.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProgressDto result = service.updateProgress(new ProgressRequest(5L, LearningProgress.Status.IN_PROGRESS));

        assertThat(result.status()).isEqualTo("IN_PROGRESS");
        assertThat(result.completedAt()).isNull();
    }
}
