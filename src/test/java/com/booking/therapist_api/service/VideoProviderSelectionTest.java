package com.booking.therapist_api.service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards which video provider the {@code @ConditionalOnProperty} wiring selects.
 *
 * <p>Until 2026-07-30 this was backwards in two places at once: {@code application.yml} defaulted
 * {@code video.provider} to {@code zoom}, and {@link ZoomVideoServiceImpl} additionally declared
 * {@code matchIfMissing = true}. Jitsi — the only provider actually in use — required an explicit
 * {@code VIDEO_PROVIDER=jitsi}. A single missing environment variable on a rebuilt VM would
 * therefore have booted the dormant Zoom path, which returns {@code 404} for every therapist
 * without a {@code therapist_zoom_credentials} row, and it would have looked like bad therapist
 * data rather than bad configuration.
 *
 * <p>These run on {@link ApplicationContextRunner}, so they assert the conditional wiring directly
 * without a database, a container or the {@code .env} file that would otherwise supply
 * {@code VIDEO_PROVIDER} and mask the default being tested.
 */
class VideoProviderSelectionTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
        .withPropertyValues("zoom.app-key=test-key", "zoom.app-secret=test-secret")
        .withUserConfiguration(VideoProviderConfiguration.class);

    @Test
    void defaultsToJitsiWhenProviderPropertyIsAbsent() {
        runner.run(context -> assertThat(context)
            .hasSingleBean(VideoConsultationProvider.class)
            .getBean(VideoConsultationProvider.class)
            .isInstanceOf(JitsiVideoServiceImpl.class));
    }

    @Test
    void selectsJitsiWhenRequestedExplicitly() {
        runner.withPropertyValues("video.provider=jitsi")
            .run(context -> assertThat(context)
                .hasSingleBean(VideoConsultationProvider.class)
                .getBean(VideoConsultationProvider.class)
                .isInstanceOf(JitsiVideoServiceImpl.class));
    }

    @Test
    void selectsZoomOnlyWhenRequestedExplicitly() {
        runner.withPropertyValues("video.provider=zoom")
            .run(context -> assertThat(context)
                .hasSingleBean(VideoConsultationProvider.class)
                .getBean(VideoConsultationProvider.class)
                .isInstanceOf(ZoomVideoServiceImpl.class));
    }

    @Configuration(proxyBeanMethods = false)
    @Import({JitsiVideoServiceImpl.class, ZoomVideoServiceImpl.class})
    static class VideoProviderConfiguration {
    }
}
