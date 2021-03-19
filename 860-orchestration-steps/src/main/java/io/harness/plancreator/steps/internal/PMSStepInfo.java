package io.harness.plancreator.steps.internal;

import io.harness.plancreator.steps.barrier.BarrierStepInfo;
import io.harness.steps.approval.step.harness.HarnessApprovalStepInfo;
import io.harness.yaml.core.StepSpecType;

import io.swagger.annotations.ApiModel;

@ApiModel(subTypes = {BarrierStepInfo.class, HarnessApprovalStepInfo.class})
public interface PMSStepInfo extends StepSpecType {}
