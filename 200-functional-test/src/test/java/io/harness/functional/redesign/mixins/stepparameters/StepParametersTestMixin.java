package io.harness.functional.redesign.mixins.stepparameters;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(using = StepParametersTestDeserializer.class)
public abstract class StepParametersTestMixin {}
