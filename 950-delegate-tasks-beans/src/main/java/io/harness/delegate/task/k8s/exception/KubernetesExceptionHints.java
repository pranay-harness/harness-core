package io.harness.delegate.task.k8s.exception;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.k8s.model.HarnessAnnotations;

import lombok.experimental.UtilityClass;

@OwnedBy(CDP)
@UtilityClass
public class KubernetesExceptionHints {
  public final String K8S_API_GENERIC_NETWORK_EXCEPTION =
      "Verify network connection between Kubernetes cluster and Harness delegate";
  public final String K8S_API_SOCKET_TIMEOUT_EXCEPTION =
      "Verify if Harness delegate is whitelisted to access Kubernetes API";
  public final String K8S_API_FORBIDDEN_EXCEPTION =
      "Check configured Kubernetes user permissions and authorization policies";
  public final String K8S_API_UNAUTHORIZED_EXCEPTION = "Check Kubernetes connector credentials";
  public final String K8S_API_VALIDATION_ERROR = "Validate Kubernetes infrastructure configuration";

  public final String DRY_RUN_MANIFEST_FAILED =
      "Check manifest output for invalid fields name, types or empty manifest";
  public final String APPLY_MANIFEST_FAILED =
      "Manifest could contain invalid values, one of resources name may not match kubernetes requirements or missing permissions to create or update specific kubernetes resources";
  public final String APPLY_NO_FILEPATH_SPECIFIED = "Please specify a valid file path to manifests from repository";

  public final String WAIT_FOR_STEADY_STATE_FAILED =
      "Check deployment pods probe checks, nodes availability or image pull secrets";
  public final String WAIT_FOR_STEADY_STATE_CLI_FAILED =
      "Resource may be deleted in process or may be related to an intermittent connection issue";
  public final String WAIT_FOR_STEADY_STATE_JOB_FAILED = "Verify job logs or output for the failure reason";
  public final String WAIT_FOR_STEADY_STATE_CRD_FAILED_CHECK_CONDITION = "The steady check condition [%s] is wrong";
  public final String WAIT_FOR_STEADY_STATE_CRD_FAILED_CHECK_CONTROLLER = "Check if custom controller is running";

  public final String CANARY_NO_WORKLOADS_FOUND =
      "Add Deployment, DeploymentConfig (Openshift) or StatefulSet workload in manifest";

  public final String CANARY_MULTIPLE_WORKLOADS =
      "Mark non-primary workloads with annotation " + HarnessAnnotations.directApply + ": true";
  public final String BG_NO_WORKLOADS_FOUND =
      "Add Deployment, DeploymentConfig (Openshift) or StatefulSet workload in manifest";
  public final String BG_MULTIPLE_WORKLOADS =
      "Mark non-primary workloads with annotation " + HarnessAnnotations.directApply + ": true";
  public final String BG_NO_SERVICE_FOUND =
      "Add at least one service in manifest. Two services [i.e. primary and stage] can be specified with annotations "
      + HarnessAnnotations.primaryService + " and " + HarnessAnnotations.stageService;
  public final String BG_MULTIPLE_PRIMARY_SERVICE = "Mark primary and stage service with "
      + HarnessAnnotations.primaryService + " and " + HarnessAnnotations.stageService + " annotations";
  public final String BG_CONFLICTING_SERVICE = "Delete existing service [%s] to proceed";

  public final String SCALE_CLI_FAILED = "Check if resource %s exists";
}
