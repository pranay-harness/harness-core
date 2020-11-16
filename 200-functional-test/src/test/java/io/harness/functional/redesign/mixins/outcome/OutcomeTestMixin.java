package io.harness.functional.redesign.mixins.outcome;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(using = OutcomeTestDeserializer.class)
public abstract class OutcomeTestMixin {}
