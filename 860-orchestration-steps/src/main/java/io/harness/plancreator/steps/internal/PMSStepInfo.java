package io.harness.plancreator.steps.internal;

import io.harness.plancreator.steps.barrier.BarrierStepInfo;
import io.harness.plancreator.steps.http.HttpStepInfo;
import io.harness.steps.approval.step.harness.HarnessApprovalStepInfo;
import io.harness.steps.approval.step.jira.JiraApprovalStepInfo;
import io.harness.steps.jira.create.JiraCreateStep;
import io.harness.yaml.core.StepSpecType;

import io.swagger.annotations.ApiModel;

@ApiModel(subTypes = {BarrierStepInfo.class, HttpStepInfo.class, HarnessApprovalStepInfo.class,
              JiraApprovalStepInfo.class, JiraCreateStep.class})
public interface PMSStepInfo extends StepSpecType {}
