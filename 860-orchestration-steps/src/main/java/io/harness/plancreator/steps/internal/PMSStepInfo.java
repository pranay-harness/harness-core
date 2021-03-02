package io.harness.plancreator.steps.internal;

import io.harness.yaml.core.StepSpecType;

import io.swagger.annotations.ApiModel;

@ApiModel(subTypes = BarrierStepInfo.class)
public interface PMSStepInfo extends StepSpecType {}
