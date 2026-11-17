package com.dailyai.assistant.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "daily_task_progress",
        uniqueConstraints = @UniqueConstraint(name = "uk_daily_task_progress", columnNames = {"user_id", "task_id"}),
        indexes = @Index(name = "idx_daily_task_progress_user", columnList = "user_id, completed_at"))
public class DailyTaskProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    private DailyTask task;

    @Column(name = "completed_at", nullable = false)
    private Instant completedAt = Instant.now();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public DailyTask getTask() {
        return task;
    }

    public void setTask(DailyTask task) {
        this.task = task;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }
}
