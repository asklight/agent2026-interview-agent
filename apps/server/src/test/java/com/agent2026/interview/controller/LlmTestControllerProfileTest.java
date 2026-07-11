package com.agent2026.interview.controller;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Profile;

import static org.assertj.core.api.Assertions.assertThat;

class LlmTestControllerProfileTest {

    @Test
    void controllerIsNotLoadedInProductionProfile() {
        Profile profile = LlmTestController.class.getAnnotation(Profile.class);

        assertThat(profile).isNotNull();
        assertThat(profile.value()).containsExactly("!prod");
    }

    @Test
    void productionApplicationContextDoesNotCreateControllerBean() {
        new ApplicationContextRunner()
                .withPropertyValues("spring.profiles.active=prod")
                .withUserConfiguration(LlmTestController.class)
                .run(context -> assertThat(context).doesNotHaveBean(LlmTestController.class));
    }
}
