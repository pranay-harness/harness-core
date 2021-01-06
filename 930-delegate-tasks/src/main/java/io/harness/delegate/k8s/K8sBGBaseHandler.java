package io.harness.delegate.k8s;

import static io.harness.govern.Switch.unhandled;
import static io.harness.logging.LogLevel.INFO;

import static software.wings.beans.LogColor.Blue;
import static software.wings.beans.LogColor.Green;
import static software.wings.beans.LogHelper.color;
import static software.wings.beans.LogWeight.Bold;

import static java.util.Arrays.asList;

import io.harness.delegate.task.k8s.K8sTaskHelperBase;
import io.harness.k8s.KubernetesContainerService;
import io.harness.k8s.kubectl.Kubectl;
import io.harness.k8s.model.HarnessLabelValues;
import io.harness.k8s.model.HarnessLabels;
import io.harness.k8s.model.K8sDelegateTaskParams;
import io.harness.k8s.model.K8sPod;
import io.harness.k8s.model.KubernetesConfig;
import io.harness.k8s.model.KubernetesResource;
import io.harness.k8s.model.KubernetesResourceId;
import io.harness.k8s.model.Release;
import io.harness.k8s.model.ReleaseHistory;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.LogCallback;

import software.wings.beans.LogColor;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.kubernetes.client.openapi.models.V1Service;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Singleton
@Slf4j
public class K8sBGBaseHandler {
  @Inject K8sTaskHelperBase k8sTaskHelperBase;
  @Inject KubernetesContainerService kubernetesContainerService;

  private String encodeColor(String color) {
    switch (color) {
      case HarnessLabelValues.colorBlue:
        return color(color, Blue, Bold);
      case HarnessLabelValues.colorGreen:
        return color(color, Green, Bold);
      default:
        unhandled(color);
    }
    return null;
  }

  public LogColor getLogColor(String color) {
    switch (color) {
      case HarnessLabelValues.colorBlue:
        return Blue;
      case HarnessLabelValues.colorGreen:
        return Green;
      default:
        unhandled(color);
    }
    return null;
  }

  public String getInverseColor(String color) {
    switch (color) {
      case HarnessLabelValues.colorBlue:
        return HarnessLabelValues.colorGreen;
      case HarnessLabelValues.colorGreen:
        return HarnessLabelValues.colorBlue;
      default:
        unhandled(color);
    }
    return null;
  }

  private String getColorFromService(V1Service service) {
    if (service.getSpec() == null || service.getSpec().getSelector() == null) {
      return null;
    }

    return service.getSpec().getSelector().get(HarnessLabels.color);
  }

  public void wrapUp(K8sDelegateTaskParams k8sDelegateTaskParams, LogCallback executionLogCallback, Kubectl client)
      throws Exception {
    executionLogCallback.saveExecutionLog("Wrapping up..\n");

    k8sTaskHelperBase.describe(client, k8sDelegateTaskParams, executionLogCallback);

    executionLogCallback.saveExecutionLog("\nDone.", INFO, CommandExecutionStatus.SUCCESS);
  }

  public String getPrimaryColor(
      KubernetesResource primaryService, KubernetesConfig kubernetesConfig, LogCallback executionLogCallback) {
    V1Service primaryServiceInCluster =
        kubernetesContainerService.getService(kubernetesConfig, primaryService.getResourceId().getName());
    if (primaryServiceInCluster == null) {
      executionLogCallback.saveExecutionLog(
          "Primary Service [" + primaryService.getResourceId().getName() + "] not found in cluster.");
    }

    return (primaryServiceInCluster != null) ? getColorFromService(primaryServiceInCluster)
                                             : HarnessLabelValues.colorDefault;
  }

  @VisibleForTesting
  public List<K8sPod> getAllPods(long timeoutInMillis, KubernetesConfig kubernetesConfig,
      KubernetesResource managedWorkload, String primaryColor, String stageColor, String releaseName) throws Exception {
    List<K8sPod> allPods = new ArrayList<>();
    String namespace = managedWorkload.getResourceId().getNamespace();
    final List<K8sPod> stagePods =
        k8sTaskHelperBase.getPodDetailsWithColor(kubernetesConfig, namespace, releaseName, stageColor, timeoutInMillis);
    final List<K8sPod> primaryPods = k8sTaskHelperBase.getPodDetailsWithColor(
        kubernetesConfig, namespace, releaseName, primaryColor, timeoutInMillis);
    stagePods.forEach(pod -> pod.setNewPod(true));
    allPods.addAll(stagePods);
    allPods.addAll(primaryPods);
    return allPods;
  }

  public void cleanupForBlueGreen(K8sDelegateTaskParams k8sDelegateTaskParams, ReleaseHistory releaseHistory,
      LogCallback executionLogCallback, String primaryColor, String stageColor, Release currentRelease, Kubectl client)
      throws Exception {
    if (StringUtils.equals(primaryColor, stageColor)) {
      return;
    }

    executionLogCallback.saveExecutionLog("Primary Service is at color: " + encodeColor(primaryColor));
    executionLogCallback.saveExecutionLog("Stage Service is at color: " + encodeColor(stageColor));

    executionLogCallback.saveExecutionLog("\nCleaning up non primary releases");

    for (int releaseIndex = releaseHistory.getReleases().size() - 1; releaseIndex >= 0; releaseIndex--) {
      Release release = releaseHistory.getReleases().get(releaseIndex);
      if (release.getNumber() != currentRelease.getNumber() && release.getManagedWorkload() != null
          && release.getManagedWorkload().getName().endsWith(stageColor)) {
        for (int resourceIndex = release.getResources().size() - 1; resourceIndex >= 0; resourceIndex--) {
          KubernetesResourceId resourceId = release.getResources().get(resourceIndex);
          if (resourceId.isVersioned()) {
            k8sTaskHelperBase.delete(client, k8sDelegateTaskParams, asList(resourceId), executionLogCallback, true);
          }
        }
      }
    }
    releaseHistory.getReleases().removeIf(release
        -> release.getNumber() != currentRelease.getNumber() && release.getManagedWorkload() != null
            && release.getManagedWorkload().getName().endsWith(stageColor));
  }
}
