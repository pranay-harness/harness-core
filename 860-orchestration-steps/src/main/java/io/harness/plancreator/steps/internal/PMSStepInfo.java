package io.harness.plancreator.steps.internal;

import io.harness.plancreator.steps.approval.HarnessApprovalStepInfo;
import io.harness.plancreator.steps.approval.JiraApprovalStepInfo;
import io.harness.yaml.core.StepSpecType;

import io.swagger.annotations.ApiModel;

@ApiModel(subTypes = {BarrierStepInfo.class, HarnessApprovalStepInfo.class, JiraApprovalStepInfo.class})
public interface PMSStepInfo extends StepSpecType {}
