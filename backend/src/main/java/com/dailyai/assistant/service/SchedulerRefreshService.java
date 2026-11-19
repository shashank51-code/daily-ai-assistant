package com.dailyai.assistant.service;

import com.dailyai.assistant.dto.RefreshResultDto;
import com.dailyai.assistant.dto.SchedulerRefreshStatusResponse;
import com.dailyai.assistant.entity.RssRefreshRun;
import com.dailyai.assistant.repository.RssRefreshRunRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Service
public class SchedulerRefreshService {

    private static final Logger log = LoggerFactory.getLogger(SchedulerRefreshService.class);
    private static final Duration STALE_THRESHOLD = Duration.ofMinutes(15);
    private static final String STALE_ERROR = "Stale execution recovered after process restart or timeout";

    private final RssRefreshRunRepository runRepository;
    private final NewsIngestionService ingestionService;
    private final RssRefreshExecutionRunner executionRunner;

    public SchedulerRefreshService(RssRefreshRunRepository runRepository,
                                  NewsIngestionService ingestionService,
                                  RssRefreshExecutionRunner executionRunner) {
        this.runRepository = runRepository;
        this.ingestionService = ingestionService;
        this.executionRunner = executionRunner;
    }

    public record RunAcquisition(RssRefreshRun run, boolean created) {
    }

    /**
     * Registers a RUNNING execution or returns the one already in progress.
     * Each repository call commits on its own so the lock row is visible to other
     * instances before ingestion starts.
     */
    public RunAcquisition tryAcquireRun(String triggerSource) {
        recoverStaleRuns();
        // Fast path: an execution is already registered as RUNNING. The partial
        // unique index is still the authoritative guard for concurrent inserts.
        Optional<RssRefreshRun> active =
                runRepository.findFirstByStatusOrderByStartedAtDesc(RssRefreshRun.STATUS_RUNNING);
        if (active.isPresent()) {
            return new RunAcquisition(active.get(), false);
        }

        RssRefreshRun run = new RssRefreshRun();
        run.setStartedAt(Instant.now());
        run.setStatus(RssRefreshRun.STATUS_RUNNING);
        run.setTriggerSource(triggerSource == null || triggerSource.isBlank() ? "UNKNOWN" : triggerSource);

        try {
            RssRefreshRun saved = runRepository.saveAndFlush(run);
            return new RunAcquisition(saved, true);
        } catch (DataIntegrityViolationException ex) {
            // Another instance inserted RUNNING between our check and our insert.
            RssRefreshRun concurrent = runRepository
                    .findFirstByStatusOrderByStartedAtDesc(RssRefreshRun.STATUS_RUNNING)
                    .orElseThrow(() -> ex);
            return new RunAcquisition(concurrent, false);
        }
    }

    public void recoverStaleRuns() {
        Instant cutoff = Instant.now().minus(STALE_THRESHOLD);
        int recovered = runRepository.recoverStaleRuns(cutoff, Instant.now(), STALE_ERROR);
        if (recovered > 0) {
            log.warn("Recovered {} stale RSS refresh execution(s)", recovered);
        }
    }

    public SchedulerRefreshStatusResponse trigger(String triggerSource) {
        RunAcquisition acquisition = tryAcquireRun(triggerSource);
        if (acquisition.created()) {
            executionRunner.run(this, acquisition.run().getId());
        } else {
            log.info("RSS refresh trigger skipped because execution {} is already running",
                    acquisition.run().getId());
        }
        return toStatus(acquisition.run());
    }

    public void executeRun(long runId) {
        RssRefreshRun run = runRepository.findById(runId).orElse(null);
        if (run == null || !RssRefreshRun.STATUS_RUNNING.equals(run.getStatus())) {
            return;
        }

        try {
            RefreshResultDto result = ingestionService.refreshAll();
            completeRun(runId, result);
        } catch (Exception ex) {
            failRun(runId, ex);
        }
    }

    public void completeRun(long runId, RefreshResultDto result) {
        RssRefreshRun run = runRepository.findById(runId).orElse(null);
        if (run == null) {
            return;
        }
        Instant finishedAt = Instant.now();
        run.setStatus(RssRefreshRun.STATUS_SUCCEEDED);
        run.setFinishedAt(finishedAt);
        run.setFeedsAttempted(result.feedsAttempted());
        run.setFeedsSucceeded(result.feedsSucceeded());
        run.setFeedsFailed(result.feedsFailed());
        run.setItemsRead(result.itemsRead());
        run.setSaved(result.saved());
        run.setDuplicates(result.duplicates());
        run.setSkippedInvalid(result.skippedInvalid());
        run.setDurationMs(Duration.between(run.getStartedAt(), finishedAt).toMillis());
        run.setErrorSummary(null);
        runRepository.save(run);
    }

    public void failRun(long runId, Exception exception) {
        RssRefreshRun run = runRepository.findById(runId).orElse(null);
        if (run == null) {
            return;
        }
        Instant finishedAt = Instant.now();
        run.setStatus(RssRefreshRun.STATUS_FAILED);
        run.setFinishedAt(finishedAt);
        run.setDurationMs(Duration.between(run.getStartedAt(), finishedAt).toMillis());
        run.setErrorSummary(safeMessage(exception));
        runRepository.save(run);
    }

    public RefreshResultDto runSynchronously(String triggerSource) {
        RunAcquisition acquisition = tryAcquireRun(triggerSource);
        if (!acquisition.created()) {
            log.info("RSS refresh skipped because execution {} is already running",
                    acquisition.run().getId());
            return null;
        }

        long runId = acquisition.run().getId();
        try {
            RefreshResultDto result = ingestionService.refreshAll();
            completeRun(runId, result);
            return result;
        } catch (Exception ex) {
            failRun(runId, ex);
            throw ex;
        }
    }

    public SchedulerRefreshStatusResponse status() {
        recoverStaleRuns();
        RssRefreshRun run = runRepository.findTopByOrderByStartedAtDesc().orElse(null);
        return toStatus(run);
    }

    private SchedulerRefreshStatusResponse toStatus(RssRefreshRun run) {
        if (run == null) {
            return new SchedulerRefreshStatusResponse(false, null, null, null, null,
                    0, 0, 0, 0, 0, 0, 0, null, null);
        }
        boolean running = RssRefreshRun.STATUS_RUNNING.equals(run.getStatus());
        return new SchedulerRefreshStatusResponse(
                running,
                run.getId(),
                run.getStartedAt(),
                run.getFinishedAt(),
                run.getStatus(),
                run.getFeedsAttempted(),
                run.getFeedsSucceeded(),
                run.getFeedsFailed(),
                run.getItemsRead(),
                run.getSaved(),
                run.getDuplicates(),
                run.getSkippedInvalid(),
                run.getDurationMs(),
                run.getErrorSummary()
        );
    }

    private String safeMessage(Exception ex) {
        String message = ex.getMessage();
        return message == null || message.isBlank() ? ex.getClass().getSimpleName() : message;
    }
}