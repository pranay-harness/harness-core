package io.harness.delegate.k8s.exception;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.k8s.model.HarnessAnnotations;

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

  String CANARY_NO_WORKLOADS_FOUND = "Add Deployment (or DeploymentConfig for Openshift) workload in manifest";
  String CANARY_MULTIPLE_WORKLOADS =
      "Mark non-primary workloads with annotation " + HarnessAnnotations.directApply + ": true";
}
