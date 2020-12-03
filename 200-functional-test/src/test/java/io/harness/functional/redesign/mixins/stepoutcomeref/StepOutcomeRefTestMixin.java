package io.harness.functional.redesign.mixins.stepoutcomeref;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(using = StepOutcomeRefTestDeserializer.class)
public abstract class StepOutcomeRefTestMixin {}
