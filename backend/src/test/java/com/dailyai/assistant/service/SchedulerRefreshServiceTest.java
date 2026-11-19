package com.dailyai.assistant.service;

import com.dailyai.assistant.dto.RefreshResultDto;
import com.dailyai.assistant.dto.SchedulerRefreshStatusResponse;
import com.dailyai.assistant.entity.RssRefreshRun;
import com.dailyai.assistant.repository.RssRefreshRunRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchedulerRefreshServiceTest {

    @Mock
    private RssRefreshRunRepository runRepository;

    @Mock
    private NewsIngestionService ingestionService;

    @Mock
    private RssRefreshExecutionRunner executionRunner;

    private SchedulerRefreshService service;

    @BeforeEach
    void setUp() {
        service = new SchedulerRefreshService(runRepository, ingestionService, executionRunner);
    }

    @Test
    void tryAcquireRunReturnsCreatedRunWhenInsertSucceeds() {
        RssRefreshRun run = new RssRefreshRun();
        run.setId(1L);
        run.setStatus(RssRefreshRun.STATUS_RUNNING);
        when(runRepository.saveAndFlush(any(RssRefreshRun.class))).thenReturn(run);
        when(runRepository.recoverStaleRuns(any(), any(), any())).thenReturn(0);
        when(runRepository.findFirstByStatusOrderByStartedAtDesc(RssRefreshRun.STATUS_RUNNING))
                .thenReturn(Optional.empty());

        SchedulerRefreshService.RunAcquisition acquisition = service.tryAcquireRun("TEST");

        assertThat(acquisition.created()).isTrue();
        assertThat(acquisition.run().getId()).isEqualTo(1L);
    }

    @Test
    void tryAcquireRunReturnsExistingRunWhenInsertConflicts() {
        RssRefreshRun active = new RssRefreshRun();
        active.setId(2L);
        active.setStatus(RssRefreshRun.STATUS_RUNNING);
        when(runRepository.recoverStaleRuns(any(), any(), any())).thenReturn(0);
        when(runRepository.saveAndFlush(any(RssRefreshRun.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"));
        when(runRepository.findFirstByStatusOrderByStartedAtDesc(RssRefreshRun.STATUS_RUNNING))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(active));

        SchedulerRefreshService.RunAcquisition acquisition = service.tryAcquireRun("TEST");

        assertThat(acquisition.created()).isFalse();
        assertThat(acquisition.run().getId()).isEqualTo(2L);
    }

    @Test
    void tryAcquireRunReusesRunningExecutionWithoutInserting() {
        RssRefreshRun active = new RssRefreshRun();
        active.setId(10L);
        active.setStatus(RssRefreshRun.STATUS_RUNNING);
        when(runRepository.recoverStaleRuns(any(), any(), any())).thenReturn(0);
        when(runRepository.findFirstByStatusOrderByStartedAtDesc(RssRefreshRun.STATUS_RUNNING))
                .thenReturn(Optional.of(active));

        SchedulerRefreshService.RunAcquisition acquisition = service.tryAcquireRun("TEST");

        assertThat(acquisition.created()).isFalse();
        verify(runRepository, never()).saveAndFlush(any(RssRefreshRun.class));
    }

    @Test
    void triggerStartsAsyncExecutionOnlyForNewRun() {
        RssRefreshRun run = new RssRefreshRun();
        run.setId(3L);
        run.setStatus(RssRefreshRun.STATUS_RUNNING);
        run.setStartedAt(Instant.now());
        when(runRepository.recoverStaleRuns(any(), any(), any())).thenReturn(0);
        when(runRepository.findFirstByStatusOrderByStartedAtDesc(RssRefreshRun.STATUS_RUNNING))
                .thenReturn(Optional.empty());
        when(runRepository.saveAndFlush(any(RssRefreshRun.class))).thenReturn(run);

        SchedulerRefreshStatusResponse response = service.trigger("TEST");

        assertThat(response.runId()).isEqualTo(3L);
        verify(executionRunner).run(service, 3L);
    }

    @Test
    void triggerDoesNotStartExecutionForExistingRun() {
        RssRefreshRun active = new RssRefreshRun();
        active.setId(4L);
        active.setStatus(RssRefreshRun.STATUS_RUNNING);
        active.setStartedAt(Instant.now());
        when(runRepository.recoverStaleRuns(any(), any(), any())).thenReturn(0);
        when(runRepository.findFirstByStatusOrderByStartedAtDesc(RssRefreshRun.STATUS_RUNNING))
                .thenReturn(Optional.of(active));

        service.trigger("TEST");

        verify(executionRunner, never()).run(any(), any(long.class));
    }

    @Test
    void executeRunMarksRunSucceeded() {
        RssRefreshRun run = new RssRefreshRun();
        run.setId(5L);
        run.setStatus(RssRefreshRun.STATUS_RUNNING);
        run.setStartedAt(Instant.now());
        when(runRepository.findById(5L)).thenReturn(Optional.of(run));
        RefreshResultDto result = new RefreshResultDto(Instant.now(), 2, 2, 0, 10, 3, 1, 0, List.of());
        when(ingestionService.refreshAll()).thenReturn(result);

        service.executeRun(5L);

        verify(runRepository).save(run);
        assertThat(run.getStatus()).isEqualTo(RssRefreshRun.STATUS_SUCCEEDED);
        assertThat(run.getSaved()).isEqualTo(3);
        assertThat(run.getDuplicates()).isEqualTo(1);
    }

    @Test
    void executeRunMarksRunFailed() {
        RssRefreshRun run = new RssRefreshRun();
        run.setId(6L);
        run.setStatus(RssRefreshRun.STATUS_RUNNING);
        run.setStartedAt(Instant.now());
        when(runRepository.findById(6L)).thenReturn(Optional.of(run));
        when(ingestionService.refreshAll()).thenThrow(new IllegalStateException("boom"));

        service.executeRun(6L);

        verify(runRepository).save(run);
        assertThat(run.getStatus()).isEqualTo(RssRefreshRun.STATUS_FAILED);
        assertThat(run.getErrorSummary()).isEqualTo("boom");
    }

    @Test
    void runSynchronouslyReturnsNullWhenAnotherRunIsActive() {
        RssRefreshRun active = new RssRefreshRun();
        active.setId(7L);
        active.setStatus(RssRefreshRun.STATUS_RUNNING);
        when(runRepository.recoverStaleRuns(any(), any(), any())).thenReturn(0);
        when(runRepository.findFirstByStatusOrderByStartedAtDesc(RssRefreshRun.STATUS_RUNNING))
                .thenReturn(Optional.of(active));

        RefreshResultDto result = service.runSynchronously("SCHEDULER");

        assertThat(result).isNull();
        verify(ingestionService, never()).refreshAll();
    }

    @Test
    void runSynchronouslyRunsIngestionAndCompletesRun() {
        RssRefreshRun run = new RssRefreshRun();
        run.setId(8L);
        run.setStatus(RssRefreshRun.STATUS_RUNNING);
        run.setStartedAt(Instant.now());
        when(runRepository.recoverStaleRuns(any(), any(), any())).thenReturn(0);
        when(runRepository.findFirstByStatusOrderByStartedAtDesc(RssRefreshRun.STATUS_RUNNING))
                .thenReturn(Optional.empty());
        when(runRepository.saveAndFlush(any(RssRefreshRun.class))).thenReturn(run);
        RefreshResultDto ingestionResult = new RefreshResultDto(Instant.now(), 1, 1, 0, 5, 2, 0, 0, List.of());
        when(ingestionService.refreshAll()).thenReturn(ingestionResult);
        when(runRepository.findById(8L)).thenReturn(Optional.of(run));

        RefreshResultDto result = service.runSynchronously("SCHEDULER");

        assertThat(result).isEqualTo(ingestionResult);
        verify(runRepository).save(run);
        assertThat(run.getStatus()).isEqualTo(RssRefreshRun.STATUS_SUCCEEDED);
    }

    @Test
    void statusReturnsLatestRun() {
        RssRefreshRun run = new RssRefreshRun();
        run.setId(9L);
        run.setStatus(RssRefreshRun.STATUS_SUCCEEDED);
        run.setStartedAt(Instant.now());
        when(runRepository.recoverStaleRuns(any(), any(), any())).thenReturn(0);
        when(runRepository.findTopByOrderByStartedAtDesc()).thenReturn(Optional.of(run));

        SchedulerRefreshStatusResponse response = service.status();

        assertThat(response.runId()).isEqualTo(9L);
        assertThat(response.running()).isFalse();
    }

    @Test
    void recoverStaleRunsDelegatesToRepository() {
        when(runRepository.recoverStaleRuns(any(), any(), any())).thenReturn(2);

        service.recoverStaleRuns();

        verify(runRepository).recoverStaleRuns(any(), any(), any());
    }
}