package io.harness.delegate.task.citasks;

import static io.harness.annotations.dev.HarnessTeam.CI;

import io.harness.annotations.dev.OwnedBy;

import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(CI)
public final class CITaskConstants {
  public static final String INIT_K8 = "INIT_K8";
  public static final String INIT_AWS_VM = "INIT_AWS_VM";
  public static final String EXECUTE_STEP_K8 = "EXECUTE_STEP_K8";
  public static final String EXECUTE_STEP_AWS_VM = "EXECUTE_STEP_AWS_VM";
  public static final String CLEANUP_K8 = "CLEANUP_K8";
  public static final String CLEANUP_AWS_VM = "CLEANUP_AWS_VM";
}