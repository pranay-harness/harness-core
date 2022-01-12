/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.task.k8s.exception;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.k8s.model.HarnessLabels;

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
  public final String APPLY_NO_FILEPATH_SPECIFIED = "No file specified in the state";
  public final String WAIT_FOR_STEADY_STATE_FAILED = "%s failed with exit code: %d";
  public final String WAIT_FOR_STEADY_STATE_FAILED_OUTPUT = "%s failed with exit code: %d and output: %s";
  public final String WAIT_FOR_STEADY_STATE_JOB_FAILED = "Job execution failed";
  public final String WAIT_FOR_STEADY_STATE_CRD_FAILED = "Steady check condition [%s] never resolved to true";

  public final String CANARY_NO_WORKLOADS_FOUND =
      "No workload found in the Manifests. Can't do Canary Deployment. Only Deployment, DeploymentConfig (OpenShift) and StatefulSet workloads are supported in Canary workflow type.";
  public final String CANARY_MULTIPLE_WORKLOADS =
      "Found %d workloads in manifest: [%s]. Canary deployment supports only one workload";
  public final String BG_NO_WORKLOADS_FOUND =
      "No workload found in the Manifests. Can't do  Blue/Green Deployment. Only Deployment, DeploymentConfig (OpenShift) and StatefulSet workloads are supported in Blue/Green deployment";
  public final String BG_MULTIPLE_WORKLOADS =
      "Found %d workloads in manifest: [%s]. Blue/Green Workflows support a single Deployment, DeploymentConfig (OpenShift) or StatefulSet workload only";
  public final String BG_NO_SERVICE_FOUND = "Service is required for BlueGreen deployments";
  public final String BG_MULTIPLE_PRIMARY_SERVICE =
      "Found %s services in manifest: [%s]. Could not locate primary service";
  public final String BG_CONFLICTING_SERVICE =
      "For blue/green deployment, the label [" + HarnessLabels.color + "] is required in service selector";
  public final String BG_SWAP_SERVICES_SERVICE_NOT_FOUND = "Service [%s] not found.";

  public final String SCALE_CLI_FAILED = "%s failed with exit code: %d";
  public final String SCALE_CLI_FAILED_OUTPUT = "%s failed with exit code: %d and output: %s";

  public final String ROLLBACK_CR_APPLY_FAILED =
      "Failed while rolling back custom resource by applying previous release manifests";
  public final String ROLLBACK_CLI_FAILED = "%s failed with exit code: %d";
  public final String ROLLBACK_CLI_FAILED_OUTPUT = "%s failed with exit code: %d and output: %s";

  public final String FAILED_TO_READ_FILE = "Could not read file at path [%s].";
  public final String INVALID_RESOURCE_KIND_NAME_FORMAT =
      "Resource name '%s' doesn't match kind/name or namespace/kind/name format";
}
