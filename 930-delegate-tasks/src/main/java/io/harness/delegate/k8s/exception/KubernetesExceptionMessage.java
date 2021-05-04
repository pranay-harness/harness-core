package io.harness.delegate.k8s.exception;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(CDP)
public interface KubernetesExceptionMessage {
  String FETCH_MANIFEST_FAILED = "Fetch manifest failed to complete";
  String DRY_RUN_MANIFEST_FAILED = "Manifest dry run failed to complete";
  String APPLY_MANIFEST_FAILED = "Apply manifest failed to complete";
  String WAIT_FOR_STEADY_STATE_FAILED = "Wait for steady state failed";
  String RELEASE_HISTORY_YAML_EXCEPTION = "Corrupted or invalid release history";

  String MISSING_APPLY_FILES_PATH = "No files specified to apply";
}
