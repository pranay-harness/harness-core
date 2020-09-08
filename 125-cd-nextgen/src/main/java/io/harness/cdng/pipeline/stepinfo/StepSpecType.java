package io.harness.cdng.pipeline.stepinfo;

public interface StepSpecType {
  String HTTP = "Http";
  String K8S_ROLLING_DEPLOY = "K8sRollingDeploy";
  String K8S_ROLLING_ROLLBACK = "K8sRollingRollback";
  String SHELL_SCRIPT = "ShellScript";
  String PLACEHOLDER = "Placeholder";
}
