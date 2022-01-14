package io.harness.delegate.task.citasks;

import static io.harness.annotations.dev.HarnessTeam.CI;

import io.harness.annotations.dev.OwnedBy;

import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(CI)
public final class CITaskConstants {
  public static final String INIT_K8 = "INIT_K8";
  public static final String INIT_VM = "INIT_VM";
  public static final String EXECUTE_STEP_K8 = "EXECUTE_STEP_K8";
  public static final String EXECUTE_STEP_VM = "EXECUTE_AWS_VM";
  public static final String CLEANUP_K8 = "CLEANUP_K8";
  public static final String CLEANUP_VM = "CLEANUP_VM";
}