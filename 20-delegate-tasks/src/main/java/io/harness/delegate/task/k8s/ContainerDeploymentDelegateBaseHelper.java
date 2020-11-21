package io.harness.delegate.task.k8s;

import static io.harness.data.structure.CollectionUtils.emptyIfNull;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static java.lang.String.format;
import static java.util.Collections.emptyList;

import io.harness.container.ContainerInfo;
import io.harness.exception.InvalidRequestException;
import io.harness.k8s.KubernetesContainerService;
import io.harness.k8s.model.KubernetesConfig;
import io.harness.logging.LogCallback;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Pod;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;

@Singleton
public class ContainerDeploymentDelegateBaseHelper {
  @Inject private KubernetesContainerService kubernetesContainerService;
  @Inject private K8sYamlToDelegateDTOMapper k8sYamlToDelegateDTOMapper;

  @NotNull
  public List<Pod> getExistingPodsByLabels(KubernetesConfig kubernetesConfig, Map<String, String> labels) {
    return emptyIfNull(kubernetesContainerService.getPods(kubernetesConfig, labels));
  }

  private List<ContainerInfo> fetchContainersUsingControllersWhenReady(KubernetesConfig kubernetesConfig,
      LogCallback executionLogCallback, List<? extends HasMetadata> controllers, List<Pod> existingPods) {
    if (isNotEmpty(controllers)) {
      return controllers.stream()
          .filter(controller
              -> !(controller.getKind().equals("ReplicaSet") && controller.getMetadata().getOwnerReferences() != null))
          .flatMap(controller -> {
            boolean isNotVersioned =
                controller.getKind().equals("DaemonSet") || controller.getKind().equals("StatefulSet");
            return kubernetesContainerService
                .getContainerInfosWhenReady(kubernetesConfig, controller.getMetadata().getName(), 0, -1,
                    (int) TimeUnit.MINUTES.toMinutes(30), existingPods, isNotVersioned, executionLogCallback, true, 0,
                    kubernetesConfig.getNamespace())
                .stream();
          })
          .collect(Collectors.toList());
    }
    return emptyList();
  }

  public List<ContainerInfo> getContainerInfosWhenReadyByLabels(
      KubernetesConfig kubernetesConfig, LogCallback logCallback, Map<String, String> labels, List<Pod> existingPods) {
    List<? extends HasMetadata> controllers = kubernetesContainerService.getControllers(kubernetesConfig, labels);

    logCallback.saveExecutionLog(format("Deployed Controllers [%s]:", controllers.size()));
    controllers.forEach(controller
        -> logCallback.saveExecutionLog(String.format("Kind:%s, Name:%s (desired: %s)", controller.getKind(),
            controller.getMetadata().getName(), kubernetesContainerService.getControllerPodCount(controller))));

    return fetchContainersUsingControllersWhenReady(kubernetesConfig, logCallback, controllers, existingPods);
  }

  public int getControllerCountByLabels(KubernetesConfig kubernetesConfig, Map<String, String> labels) {
    List<? extends HasMetadata> controllers = kubernetesContainerService.getControllers(kubernetesConfig, labels);

    return controllers.size();
  }

  public KubernetesConfig createKubernetesConfig(K8sInfraDelegateConfig clusterConfigDTO) {
    if (clusterConfigDTO instanceof DirectK8sInfraDelegateConfig) {
      return k8sYamlToDelegateDTOMapper.createKubernetesConfigFromClusterConfig(
          ((DirectK8sInfraDelegateConfig) clusterConfigDTO).getKubernetesClusterConfigDTO(),
          clusterConfigDTO.getNamespace());
    } else {
      throw new InvalidRequestException("Unhandled K8sInfraDelegateConfig " + clusterConfigDTO.getClass());
    }
  }

  public String getKubeconfigFileContent(K8sInfraDelegateConfig k8sInfraDelegateConfig) {
    return kubernetesContainerService.getConfigFileContent(createKubernetesConfig(k8sInfraDelegateConfig));
  }
}
