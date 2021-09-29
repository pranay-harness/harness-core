package io.harness.delegate.task.k8s.exception;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;

import lombok.experimental.UtilityClass;

@OwnedBy(CDP)
@UtilityClass
public class KubernetesExceptionMessages {
  public final String DRY_RUN_MANIFEST_FAILED = "Dry run of manifest failed";
  public final String APPLY_MANIFEST_FAILED = "Apply manifest failed";
  public final String WAIT_FOR_STEADY_STATE_FAILED = "Wait for steady state failed";
}
