package com.dailyai.assistant.service;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class ArticleAiGenerationGuardTest {

    @Test
    void sameArticleRunsOneGenerationAtATime() throws Exception {
        ArticleAiGenerationGuard guard = new ArticleAiGenerationGuard();
        int callers = 6;
        ExecutorService executor = Executors.newFixedThreadPool(callers);
        CountDownLatch ready = new CountDownLatch(callers);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger inside = new AtomicInteger();
        AtomicInteger maximumInside = new AtomicInteger();
        List<Future<Integer>> results = new ArrayList<>();
        try {
            for (int i = 0; i < callers; i++) {
                results.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    return guard.run(42L, () -> {
                        int current = inside.incrementAndGet();
                        maximumInside.accumulateAndGet(current, Math::max);
                        try {
                            Thread.sleep(25);
                        } catch (InterruptedException ex) {
                            Thread.currentThread().interrupt();
                        } finally {
                            inside.decrementAndGet();
                        }
                        return 1;
                    });
                }));
            }
            ready.await();
            start.countDown();
            for (Future<Integer> result : results) {
                assertThat(result.get()).isEqualTo(1);
            }
            assertThat(maximumInside.get()).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void differentArticlesDoNotBlockEachOther() throws Exception {
        ArticleAiGenerationGuard guard = new ArticleAiGenerationGuard();
        CountDownLatch bothInside = new CountDownLatch(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> first = executor.submit(() -> guard.run(1L, () -> {
                bothInside.countDown();
                return awaitBoth(bothInside);
            }));
            Future<Boolean> second = executor.submit(() -> guard.run(2L, () -> {
                bothInside.countDown();
                return awaitBoth(bothInside);
            }));

            assertThat(first.get()).isTrue();
            assertThat(second.get()).isTrue();
        } finally {
            executor.shutdownNow();
        }
    }

    private static boolean awaitBoth(CountDownLatch bothInside) {
        try {
            return bothInside.await(2, java.util.concurrent.TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
