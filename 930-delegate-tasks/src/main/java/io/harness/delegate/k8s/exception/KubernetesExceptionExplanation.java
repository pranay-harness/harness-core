package io.harness.delegate.k8s.exception;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(CDP)
public interface KubernetesExceptionExplanation {
  String K8S_API_IO_EXCEPTION = "Kubernetes API call failed due to I/O error";
  String K8S_API_SOCKET_TIMEOUT_EXCEPTION = "Kubernetes API call failed due to timeout error";
  String K8S_API_GENERIC_NETWORK_EXCEPTION = "Unable to connect to Kubernetes cluster";
  String K8S_API_FORBIDDEN_EXCEPTION = "Kubernetes user is missing required role or permission and is not authorized";
  String K8S_API_UNAUTHORIZED_EXCEPTION = "Unable to authenticate using provided Kubernetes credentials";
  String K8S_API_VALIDATION_ERROR =
      "Some of the provided values in Kubernetes configuration missing or invalid (i.e. namespace, release name)";

  String FETCH_MANIFEST_FAILED = "Failed to fetch deployment manifest";
  String DRY_RUN_MANIFEST_FAILED = "Performed local manifest validation failed";
  String APPLY_MANIFEST_FAILED = "Unable to apply Kubernetes manifest";
  String WAIT_FOR_STEADY_STATE_FAILED = "One of managed workloads failed to rollout";
}
