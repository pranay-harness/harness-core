/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.k8s;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.k8s.model.Kind.Namespace;
import static io.harness.k8s.model.KubernetesResourceId.createKubernetesResourceIdsFromKindName;

import static software.wings.beans.LogColor.GrayDark;
import static software.wings.beans.LogHelper.color;
import static software.wings.beans.LogWeight.Bold;

import static java.util.stream.Collectors.toList;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.task.k8s.DeleteResourcesType;
import io.harness.delegate.task.k8s.K8sDeleteRequest;
import io.harness.delegate.task.k8s.K8sDeployResponse;
import io.harness.delegate.task.k8s.K8sTaskHelperBase;
import io.harness.exception.InvalidArgumentsException;
import io.harness.k8s.model.KubernetesConfig;
import io.harness.k8s.model.KubernetesResourceId;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogCallback;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@OwnedBy(CDP)
@Singleton
@Slf4j
public class K8sDeleteBaseHandler {
  @Inject K8sTaskHelperBase k8sTaskHelperBase;

  public List<KubernetesResourceId> getResourceIdsToDelete(K8sDeleteRequest k8sDeleteRequest,
      KubernetesConfig kubernetesConfig, LogCallback executionLogCallback) throws IOException {
    DeleteResourcesType deleteResourcesType = k8sDeleteRequest.getDeleteResourcesType();
    switch (deleteResourcesType) {
      case ReleaseName:
        return getReleaseNameResourceIdsToDelete(k8sDeleteRequest, kubernetesConfig, executionLogCallback);
      case ResourceName:
        return getResourceNameResourceIdsToDelete(k8sDeleteRequest.getResources());
      default:
        throw new UnsupportedOperationException(
            String.format("Delete resource type: [%s]", deleteResourcesType.name()));
    }
  }

  private List<KubernetesResourceId> getReleaseNameResourceIdsToDelete(K8sDeleteRequest k8sDeleteRequest,
      KubernetesConfig kubernetesConfig, LogCallback executionLogCallback) throws IOException {
    executionLogCallback.saveExecutionLog("All Resources are selected for deletion");
    executionLogCallback.saveExecutionLog(color("Delete Namespace is set to: "
            + k8sDeleteRequest.isDeleteNamespacesForRelease() + ", Skipping deleting Namespace resources",
        GrayDark, Bold));
    executionLogCallback.saveExecutionLog(
        "Delete Namespace is set to: " + k8sDeleteRequest.isDeleteNamespacesForRelease());
    return getResourceIdsForDeletion(k8sDeleteRequest, kubernetesConfig, executionLogCallback);
  }

  public List<KubernetesResourceId> getResourceNameResourceIdsToDelete(String resources) {
    if (StringUtils.isEmpty(resources)) {
      return Collections.emptyList();
    }

    if ("*".equals(resources.trim())) {
      throw new InvalidArgumentsException("Invalid resource name. Use release name instead.");
    }

    return createKubernetesResourceIdsFromKindName(resources);
  }

  private List<KubernetesResourceId> getResourceIdsForDeletion(K8sDeleteRequest k8sDeleteRequest,
      KubernetesConfig kubernetesConfig, LogCallback executionLogCallback) throws IOException {
    List<KubernetesResourceId> kubernetesResourceIds = k8sTaskHelperBase.fetchAllResourcesForRelease(
        k8sDeleteRequest.getReleaseName(), kubernetesConfig, executionLogCallback);

    // If namespace deletion is NOT selected,remove all Namespace resources from deletion list
    if (!k8sDeleteRequest.isDeleteNamespacesForRelease()) {
      kubernetesResourceIds =
          kubernetesResourceIds.stream()
              .filter(kubernetesResourceId -> !Namespace.name().equals(kubernetesResourceId.getKind()))
              .collect(toList());
    }

    return k8sTaskHelperBase.arrangeResourceIdsInDeletionOrder(kubernetesResourceIds);
  }

  public K8sDeployResponse getSuccessResponse() {
    return K8sDeployResponse.builder().commandExecutionStatus(CommandExecutionStatus.SUCCESS).build();
  }
}
