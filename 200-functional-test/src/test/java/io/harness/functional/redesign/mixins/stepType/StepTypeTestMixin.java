package io.harness.functional.redesign.mixins.stepType;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(using = StepTypeTestDeserializer.class)
public abstract class StepTypeTestMixin {}
