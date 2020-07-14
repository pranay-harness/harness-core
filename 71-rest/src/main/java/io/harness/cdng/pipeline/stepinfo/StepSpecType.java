package io.harness.cdng.pipeline.stepinfo;

public interface StepSpecType {
  String HTTP = "Http";
  String K8S_ROLLOUT_DEPLOY = "K8sRolloutDeploy";
  String K8S_ROLLING_ROLLBACK = "K8sRollingRollback";
  String SHELL_SCRIPT = "ShellScript";
}
