package com.dailyai.assistant.service;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * In-process single-flight per article. Concurrent readers of the same article
 * share one generation path instead of spending provider budget twice. The
 * guard is intentionally process-local; the database usage ledger remains the
 * cross-instance budget authority.
 */
@Component
public class ArticleAiGenerationGuard {

    private final ConcurrentHashMap<Long, ReentrantLock> locks = new ConcurrentHashMap<>();

    public <T> T run(Long articleId, Supplier<T> action) {
        ReentrantLock lock = locks.computeIfAbsent(articleId, ignored -> new ReentrantLock());
        lock.lock();
        try {
            return action.get();
        } finally {
            lock.unlock();
            if (!lock.hasQueuedThreads()) {
                locks.remove(articleId, lock);
            }
        }
    }
}
