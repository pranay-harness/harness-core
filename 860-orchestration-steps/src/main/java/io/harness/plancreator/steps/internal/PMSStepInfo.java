package io.harness.plancreator.steps.internal;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.plancreator.steps.barrier.BarrierStepInfo;
import io.harness.plancreator.steps.common.WithStepElementParameters;
import io.harness.plancreator.steps.http.HttpStepInfo;
import io.harness.steps.approval.step.harness.HarnessApprovalStepInfo;
import io.harness.steps.approval.step.jira.JiraApprovalStepInfo;
import io.harness.steps.jira.create.JiraCreateStepInfo;
import io.harness.steps.jira.update.JiraUpdateStepInfo;
import io.harness.steps.shellscript.ShellScriptStepInfo;
import io.harness.yaml.core.StepSpecType;

import io.swagger.annotations.ApiModel;

@OwnedBy(PIPELINE)
@ApiModel(subTypes = {BarrierStepInfo.class, HttpStepInfo.class, FlagConfigurationStepInfo.class,
              HarnessApprovalStepInfo.class, JiraApprovalStepInfo.class, JiraCreateStepInfo.class,
              JiraUpdateStepInfo.class, ShellScriptStepInfo.class})
public interface PMSStepInfo extends StepSpecType, WithStepElementParameters {}
