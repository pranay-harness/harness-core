package io.harness.delegate.task.k8s.exception;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;

import lombok.experimental.UtilityClass;

@OwnedBy(CDP)
@UtilityClass
public class KubernetesExceptionExplanation {
  public final String K8S_API_GENERIC_NETWORK_EXCEPTION = "Harness delegate is not able to reach Kubernetes API";
  public final String K8S_API_SOCKET_TIMEOUT_EXCEPTION = "Connection timed out while trying to reach Kubernetes API";
  public final String K8S_API_IO_EXCEPTION = "Kubernetes API call failed due to I/O error";
  public final String K8S_API_FORBIDDEN_EXCEPTION =
      "Kubernetes user missing role or permission to use specified resource";
  public final String K8S_API_UNAUTHORIZED_EXCEPTION = "Unable to authenticate using provided Kubernetes credentials";
  public final String K8S_API_VALIDATION_ERROR =
      "Some of the provided values in Kubernetes configuration missing or invalid (i.e. namespace, release name)";

  public final String DRY_RUN_MANIFEST_FAILED = "%s failed with exit code: %d";
  public final String DRY_RUN_MANIFEST_FAILED_OUTPUT = "%s failed with exit code: %d and output: %s";
  public final String APPLY_MANIFEST_FAILED = "%s failed with exit code: %d";
  public final String APPLY_MANIFEST_FAILED_OUTPUT = "%s failed with exit code: %d and output: %s";
  public final String WAIT_FOR_STEADY_STATE_FAILED = "%s failed with exit code: %d";
  public final String WAIT_FOR_STEADY_STATE_FAILED_OUTPUT = "%s failed with exit code: %d and output: %s";
  public final String WAIT_FOR_STEADY_STATE_JOB_FAILED = "Job execution failed";
  public final String WAIT_FOR_STEADY_STATE_CRD_FAILED = "Steady check condition [%s] never resolved to true";
}
