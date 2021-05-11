package io.harness.delegate.k8s.exception;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(CDP)
public interface KubernetesExceptionHints {
  String K8S_API_GENERIC_NETWORK_EXCEPTION =
      "Verify network connection between Kubernetes cluster and Harness delegate";
  String K8S_API_SOCKET_TIMEOUT_EXCEPTION = "Check if Kubernetes cluster is available from Harness delegate";
  String K8S_API_FORBIDDEN_EXCEPTION = "Check configured Kubernetes user permissions and authorization policies";
  String K8S_API_UNAUTHORIZED_EXCEPTION = "Check Kubernetes connector credentials";
  String K8S_API_VALIDATION_ERROR = "Check Kubernetes infrastructure configuration";

  String DRY_RUN_MANIFEST_FAILED = "Verify manifest output for invalid fields names and types or manifest is not empty";
  String APPLY_MANIFEST_FAILED = "Verify Kubernetes manifest for invalid values or conflicting resources";
  String WAIT_FOR_STEADY_STATE_FAILED = "Check deployment pods probe checks, nodes availability or image pull secrets";

  String PERCENTAGE_CURRENT_REPLICA_NOT_FOUND = "Check workload name in step";
  String SCALE_COMMAND_FAILED_MISSING_WORKLOAD = "Check if workload %s exists";
  String SCALE_INSTANCE_UNIT_TYPE_MISSING = "Select a valid instance unit type in Scale step";
}
