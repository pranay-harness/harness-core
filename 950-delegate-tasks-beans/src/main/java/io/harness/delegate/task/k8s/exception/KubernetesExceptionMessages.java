package io.harness.delegate.task.k8s.exception;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;

import lombok.experimental.UtilityClass;

@OwnedBy(CDP)
@UtilityClass
public class KubernetesExceptionMessages {
  public final String DRY_RUN_MANIFEST_FAILED = "Dry run of manifest failed";
  public final String APPLY_MANIFEST_FAILED = "Apply manifest failed";
  public final String APPLY_NO_FILEPATH_SPECIFIED = "No file specified in the state";
  public final String WAIT_FOR_STEADY_STATE_FAILED = "Wait for steady state failed";

  public final String NO_WORKLOADS_FOUND = "Missing managed workload in kubernetes manifest";
  public final String MULTIPLE_WORKLOADS = "More than one workloads found in the manifests";
  public final String NO_SERVICE_FOUND = "Missing service in kubernetes manifest";
  public final String MULTIPLE_SERVICES = "Multiple unmarked services found in manifest";
  public final String BG_CONFLICTING_SERVICE = "Found conflicting service [%s] in the cluster";
  public final String SCALE_CLI_FAILED = "Failed to scale resource: %s";

  public final String ROLLBACK_CLI_FAILED = "Failed to rollback resource %s in namespace %s to revision %s";
}
