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

  public final String CANARY_NO_WORKLOADS_FOUND = "Missing managed workload in kubernetes manifest";
  public final String CANARY_MULTIPLE_WORKLOADS = "More than one workloads found in the manifests";
}
