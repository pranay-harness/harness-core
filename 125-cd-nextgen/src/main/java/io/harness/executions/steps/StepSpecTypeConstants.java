package io.harness.executions.steps;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(CDP)
public interface StepSpecTypeConstants {
  String K8S_ROLLING_DEPLOY = "K8sRollingDeploy";
  String K8S_ROLLING_ROLLBACK = "K8sRollingRollback";
  String K8S_BLUE_GREEN_DEPLOY = "K8sBlueGreenDeploy";
  String K8S_APPLY = "K8sApply";
  String K8S_SCALE = "K8sScale";
  String K8S_BG_SWAP_SERVICES = "K8sBGSwapServices";
  String K8S_CANARY_DELETE = "K8sCanaryDelete";
  String K8S_CANARY_DEPLOY = "K8sCanaryDeploy";
  String K8S_DELETE = "K8sDelete";

  String TERRAFORM_APPLY = "TerraformApply";
  String TERRAFORM_PLAN = "TerraformPlan";
  String TERRAFORM_DESTROY = "TerraformDestroy";
  String TERRAFORM_ROLLBACK = "TerraformRollback";

  String PLACEHOLDER = "Placeholder";
}
