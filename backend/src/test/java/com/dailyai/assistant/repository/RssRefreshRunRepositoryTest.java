package com.dailyai.assistant.repository;

import com.dailyai.assistant.entity.RssRefreshRun;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class RssRefreshRunRepositoryTest {

    @Autowired
    private RssRefreshRunRepository runRepository;

    @Test
    void keepsRunHistoryAndFindsTheLatestRun() {
        RssRefreshRun older = run("SUCCEEDED", Instant.parse("2026-11-18T00:30:00Z"));
        RssRefreshRun newer = run("SUCCEEDED", Instant.parse("2026-11-19T00:30:00Z"));
        runRepository.saveAll(List.of(older, newer));

        assertThat(runRepository.count()).isEqualTo(2);
        assertThat(runRepository.findTopByOrderByStartedAtDesc()).get()
                .extracting(RssRefreshRun::getStartedAt)
                .isEqualTo(Instant.parse("2026-11-19T00:30:00Z"));
    }

    @Test
    void recoverStaleRunsOnlyClosesOldRunningExecutions() {
        RssRefreshRun stale = run(RssRefreshRun.STATUS_RUNNING, Instant.now().minusSeconds(3600));
        RssRefreshRun active = run(RssRefreshRun.STATUS_RUNNING, Instant.now().minusSeconds(30));
        runRepository.saveAll(List.of(stale, active));

        int recovered = runRepository.recoverStaleRuns(
                Instant.now().minusSeconds(900), Instant.now(), "stale");

        assertThat(recovered).isEqualTo(1);
        RssRefreshRun reloadedStale = runRepository.findById(stale.getId()).orElseThrow();
        assertThat(reloadedStale.getStatus()).isEqualTo(RssRefreshRun.STATUS_FAILED);
        assertThat(reloadedStale.getFinishedAt()).isNotNull();
        assertThat(reloadedStale.getErrorSummary()).isEqualTo("stale");
        assertThat(runRepository.findById(active.getId()).orElseThrow().getStatus())
                .isEqualTo(RssRefreshRun.STATUS_RUNNING);
    }

    private RssRefreshRun run(String status, Instant startedAt) {
        RssRefreshRun run = new RssRefreshRun();
        run.setStartedAt(startedAt);
        run.setStatus(status);
        run.setTriggerSource("TEST");
        return run;
    }
}