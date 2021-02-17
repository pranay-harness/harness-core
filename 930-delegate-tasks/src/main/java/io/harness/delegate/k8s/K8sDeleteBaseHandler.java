package io.harness.delegate.k8s;

import static io.harness.k8s.model.Kind.Namespace;

import static java.util.stream.Collectors.toList;

import io.harness.delegate.task.k8s.K8sDeleteRequest;
import io.harness.delegate.task.k8s.K8sDeployResponse;
import io.harness.delegate.task.k8s.K8sTaskHelperBase;
import io.harness.k8s.model.KubernetesConfig;
import io.harness.k8s.model.KubernetesResourceId;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogCallback;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class K8sDeleteBaseHandler {
  @Inject K8sTaskHelperBase k8sTaskHelperBase;

  public List<KubernetesResourceId> getResourceIdsForDeletion(K8sDeleteRequest k8sDeleteRequest,
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
