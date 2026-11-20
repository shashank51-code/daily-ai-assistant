package com.dailyai.assistant.scheduler;

import com.dailyai.assistant.service.SchedulerRefreshService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.task.TaskSchedulingAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Verifies that {@code SPRING_TASK_SCHEDULING_ENABLED=false} really stops the periodic RSS
 * refresh, instead of only removing the auto-configured {@code TaskScheduler}.
 *
 * <p>No database and no full application context are involved: these are sliced contexts
 * with a mocked service collaborator.
 */
class NewsFetchSchedulerSchedulingGateTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withBean(SchedulerRefreshService.class, () -> mock(SchedulerRefreshService.class))
            .withUserConfiguration(NewsFetchScheduler.class);

    @Test
    void schedulerIsRegisteredWhenThePropertyIsUnset() {
        runner.run(context -> assertThat(context).hasSingleBean(NewsFetchScheduler.class));
    }

    @Test
    void schedulerIsRegisteredWhenSchedulingIsExplicitlyEnabled() {
        runner.withPropertyValues("spring.task.scheduling.enabled=true")
                .run(context -> assertThat(context).hasSingleBean(NewsFetchScheduler.class));
    }

    @Test
    void schedulerIsAbsentWhenSchedulingIsDisabled() {
        runner.withPropertyValues("spring.task.scheduling.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(NewsFetchScheduler.class));
    }

    /**
     * Pins the reason the bean itself is conditional. {@code @EnableScheduling} registers its
     * own {@code ScheduledAnnotationBeanPostProcessor}, which survives
     * {@code spring.task.scheduling.enabled=false} and installs a fallback scheduler; without
     * the condition on the bean, the migration-validation startup would still refresh feeds.
     */
    @Test
    void autoConfigurationSwitchAloneLeavesScheduledAnnotationProcessingActive() {
        new ApplicationContextRunner()
                .withPropertyValues("spring.task.scheduling.enabled=false")
                .withConfiguration(AutoConfigurations.of(TaskSchedulingAutoConfiguration.class))
                .withUserConfiguration(SchedulingProbeConfiguration.class)
                .run(context -> {
                    assertThat(context)
                            .hasBean("org.springframework.context.annotation.internalScheduledAnnotationProcessor");
                    assertThat(context).hasSingleBean(SchedulingProbe.class);
                });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableScheduling
    static class SchedulingProbeConfiguration {

        @Bean
        SchedulingProbe schedulingProbe() {
            return new SchedulingProbe();
        }
    }

    static class SchedulingProbe {

        @Scheduled(fixedDelay = 60_000)
        void tick() {
            // Never asserted on: presence of the annotation processor is the point.
        }
    }
}
