package io.harness.cdng.executionplan;

public enum CDStepDependencyKey {
  SERVICE,
  INFRASTRUCTURE,
  K8S_ROLL_OUT,
  K8S_BLUE_GREEN,
  K8S_APPLY,
  K8S_SCALE,
  K8S_CANARY,
  K8S_DELETE
}
