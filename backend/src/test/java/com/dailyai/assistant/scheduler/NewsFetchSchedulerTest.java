package com.dailyai.assistant.scheduler;

import com.dailyai.assistant.dto.RefreshResultDto;
import com.dailyai.assistant.service.SchedulerRefreshService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NewsFetchSchedulerTest {

    @Mock
    private SchedulerRefreshService refreshService;

    private NewsFetchScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new NewsFetchScheduler(refreshService);
    }

    @Test
    void logsAndSkipsWhenAnotherExecutionHoldsTheLock() {
        when(refreshService.runSynchronously("SCHEDULER")).thenReturn(null);

        assertThatCode(() -> scheduler.fetchFeeds()).doesNotThrowAnyException();
        verify(refreshService).runSynchronously("SCHEDULER");
    }

    @Test
    void logsResultOfASuccessfulRun() {
        when(refreshService.runSynchronously("SCHEDULER")).thenReturn(new RefreshResultDto(
                Instant.now(), 1, 1, 0, 5, 2, 0, 0, List.of()));

        assertThatCode(() -> scheduler.fetchFeeds()).doesNotThrowAnyException();
        verify(refreshService).runSynchronously("SCHEDULER");
    }
}