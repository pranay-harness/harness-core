package io.harness.delegate.k8s;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.logging.LogLevel.INFO;
import static java.util.Arrays.asList;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.delegate.task.k8s.K8sTaskHelperBase;
import io.harness.k8s.kubectl.Kubectl;
import io.harness.k8s.model.HarnessLabelValues;
import io.harness.k8s.model.HarnessLabels;
import io.harness.k8s.model.K8sDelegateTaskParams;
import io.harness.k8s.model.K8sPod;
import io.harness.k8s.model.Kind;
import io.harness.k8s.model.KubernetesConfig;
import io.harness.k8s.model.KubernetesResource;
import io.harness.k8s.model.Release;
import io.harness.k8s.model.Release.KubernetesResourceIdRevision;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogCallback;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Singleton
@Slf4j
public class K8sRollingBaseHandler {
  @Inject K8sTaskHelperBase k8sTaskHelperBase;

  @VisibleForTesting
  public List<K8sPod> tagNewPods(List<K8sPod> newPods, List<K8sPod> existingPods) {
    Set<String> existingPodNames = existingPods.stream().map(K8sPod::getName).collect(Collectors.toSet());
    List<K8sPod> allPods = new ArrayList<>(newPods);
    allPods.forEach(pod -> {
      if (!existingPodNames.contains(pod.getName())) {
        pod.setNewPod(true);
      }
    });
    return allPods;
  }

  @VisibleForTesting
  public void updateDeploymentConfigRevision(
      K8sDelegateTaskParams k8sDelegateTaskParams, Release release, Kubectl client) throws Exception {
    List<KubernetesResourceIdRevision> workloads = release.getManagedWorkloads();

    for (KubernetesResourceIdRevision kubernetesResourceIdRevision : workloads) {
      if (Kind.DeploymentConfig.name().equals(kubernetesResourceIdRevision.getWorkload().getKind())) {
        String latestRevision = k8sTaskHelperBase.getLatestRevision(
            client, kubernetesResourceIdRevision.getWorkload(), k8sDelegateTaskParams);
        kubernetesResourceIdRevision.setRevision(latestRevision);
      }
    }
  }

  public void setManagedWorkloadsInRelease(K8sDelegateTaskParams k8sDelegateTaskParams,
      List<KubernetesResource> managedWorkloads, Release release, Kubectl client) throws Exception {
    List<KubernetesResourceIdRevision> kubernetesResourceIdRevisions = new ArrayList<>();

    for (KubernetesResource kubernetesResource : managedWorkloads) {
      String latestRevision =
          k8sTaskHelperBase.getLatestRevision(client, kubernetesResource.getResourceId(), k8sDelegateTaskParams);

      kubernetesResourceIdRevisions.add(KubernetesResourceIdRevision.builder()
                                            .workload(kubernetesResource.getResourceId())
                                            .revision(latestRevision)
                                            .build());
    }

    release.setManagedWorkloads(kubernetesResourceIdRevisions);
  }

  public void setCustomWorkloadsInRelease(List<KubernetesResource> customWorkloads, Release release) {
    if (isNotEmpty(customWorkloads)) {
      release.setCustomWorkloads(customWorkloads);
    }
  }

  public void addLabelsInDeploymentSelectorForCanary(
      boolean inCanaryWorkflow, List<KubernetesResource> managedWorkloads) {
    if (!inCanaryWorkflow) {
      return;
    }

    for (KubernetesResource kubernetesResource : managedWorkloads) {
      if (ImmutableSet.of(Kind.Deployment.name(), Kind.DeploymentConfig.name())
              .contains(kubernetesResource.getResourceId().getKind())) {
        kubernetesResource.addLabelsInDeploymentSelector(
            ImmutableMap.of(HarnessLabels.track, HarnessLabelValues.trackStable));
      }
    }
  }

  public void addLabelsInManagedWorkloadPodSpec(
      boolean inCanaryWorkflow, List<KubernetesResource> managedWorkloads, String releaseName) {
    Map<String, String> podLabels = inCanaryWorkflow
        ? ImmutableMap.of(HarnessLabels.releaseName, releaseName, HarnessLabels.track, HarnessLabelValues.trackStable)
        : ImmutableMap.of(HarnessLabels.releaseName, releaseName);

    for (KubernetesResource kubernetesResource : managedWorkloads) {
      kubernetesResource.addLabelsInPodSpec(podLabels);
    }
  }

  public void updateDestinationRuleWithSubsets(LogCallback executionLogCallback, List<KubernetesResource> resources,
      KubernetesConfig kubernetesConfig) throws IOException {
    k8sTaskHelperBase.updateDestinationRuleManifestFilesWithSubsets(resources,
        asList(HarnessLabelValues.trackCanary, HarnessLabelValues.trackStable), kubernetesConfig, executionLogCallback);
  }

  public void updateVirtualServiceWithRoutes(LogCallback executionLogCallback, List<KubernetesResource> resources,
      KubernetesConfig kubernetesConfig) throws IOException {
    k8sTaskHelperBase.updateVirtualServiceManifestFilesWithRoutesForCanary(
        resources, kubernetesConfig, executionLogCallback);
  }

  public void wrapUp(K8sDelegateTaskParams k8sDelegateTaskParams, LogCallback executionLogCallback, Kubectl client)
      throws Exception {
    executionLogCallback.saveExecutionLog("Wrapping up..\n");

    k8sTaskHelperBase.describe(client, k8sDelegateTaskParams, executionLogCallback);

    executionLogCallback.saveExecutionLog("\nDone.", INFO, CommandExecutionStatus.SUCCESS);
  }

  public List<K8sPod> getPods(long timeoutInMillis, List<KubernetesResource> managedWorkloads,
      KubernetesConfig kubernetesConfig, String releaseName) throws Exception {
    List<K8sPod> k8sPods = new ArrayList<>();

    if (isEmpty(managedWorkloads)) {
      return k8sPods;
    }

    for (KubernetesResource kubernetesResource : managedWorkloads) {
      List<K8sPod> podDetails = k8sTaskHelperBase.getPodDetails(
          kubernetesConfig, kubernetesResource.getResourceId().getNamespace(), releaseName, timeoutInMillis);

      if (isNotEmpty(podDetails)) {
        k8sPods.addAll(podDetails);
      }
    }

    return k8sPods;
  }
}
