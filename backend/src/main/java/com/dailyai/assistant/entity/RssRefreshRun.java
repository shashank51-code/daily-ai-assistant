package com.dailyai.assistant.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "rss_refresh_runs")
public class RssRefreshRun {

    public static final String STATUS_RUNNING = "RUNNING";
    public static final String STATUS_SUCCEEDED = "SUCCEEDED";
    public static final String STATUS_FAILED = "FAILED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "trigger_source", nullable = false, length = 40)
    private String triggerSource;

    @Column(name = "feeds_attempted", nullable = false)
    private int feedsAttempted;

    @Column(name = "feeds_succeeded", nullable = false)
    private int feedsSucceeded;

    @Column(name = "feeds_failed", nullable = false)
    private int feedsFailed;

    @Column(name = "items_read", nullable = false)
    private int itemsRead;

    @Column(name = "saved", nullable = false)
    private int saved;

    @Column(name = "duplicates", nullable = false)
    private int duplicates;

    @Column(name = "skipped_invalid", nullable = false)
    private int skippedInvalid;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "error_summary", columnDefinition = "TEXT")
    private String errorSummary;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(Instant finishedAt) {
        this.finishedAt = finishedAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTriggerSource() {
        return triggerSource;
    }

    public void setTriggerSource(String triggerSource) {
        this.triggerSource = triggerSource;
    }

    public int getFeedsAttempted() {
        return feedsAttempted;
    }

    public void setFeedsAttempted(int feedsAttempted) {
        this.feedsAttempted = feedsAttempted;
    }

    public int getFeedsSucceeded() {
        return feedsSucceeded;
    }

    public void setFeedsSucceeded(int feedsSucceeded) {
        this.feedsSucceeded = feedsSucceeded;
    }

    public int getFeedsFailed() {
        return feedsFailed;
    }

    public void setFeedsFailed(int feedsFailed) {
        this.feedsFailed = feedsFailed;
    }

    public int getItemsRead() {
        return itemsRead;
    }

    public void setItemsRead(int itemsRead) {
        this.itemsRead = itemsRead;
    }

    public int getSaved() {
        return saved;
    }

    public void setSaved(int saved) {
        this.saved = saved;
    }

    public int getDuplicates() {
        return duplicates;
    }

    public void setDuplicates(int duplicates) {
        this.duplicates = duplicates;
    }

    public int getSkippedInvalid() {
        return skippedInvalid;
    }

    public void setSkippedInvalid(int skippedInvalid) {
        this.skippedInvalid = skippedInvalid;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(Long durationMs) {
        this.durationMs = durationMs;
    }

    public String getErrorSummary() {
        return errorSummary;
    }

    public void setErrorSummary(String errorSummary) {
        this.errorSummary = errorSummary;
    }
}