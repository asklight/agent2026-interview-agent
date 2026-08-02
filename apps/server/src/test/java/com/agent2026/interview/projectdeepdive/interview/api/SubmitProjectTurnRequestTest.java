package com.agent2026.interview.projectdeepdive.interview.api;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SubmitProjectTurnRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void questionTurnIdMustBePositiveWhenPresent() {
        var nonPositive = new SubmitProjectTurnRequest("client-2", 0L, "answer", "TEXT");

        assertThat(validator.validate(nonPositive))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly("questionTurnId");
    }

    @Test
    void presentAndLegacyMissingAnchorsPassTransportValidation() {
        var request = new SubmitProjectTurnRequest("client-1", 10L, "answer", "TEXT");
        var legacyReplay = new SubmitProjectTurnRequest("client-2", null, "answer", "TEXT");

        assertThat(validator.validate(request)).isEmpty();
        assertThat(validator.validate(legacyReplay)).isEmpty();
    }
}
