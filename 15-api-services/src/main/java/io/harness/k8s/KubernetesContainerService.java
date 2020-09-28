package io.harness.k8s;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.HorizontalPodAutoscaler;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;
import io.fabric8.kubernetes.api.model.extensions.Ingress;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.harness.container.ContainerInfo;
import io.harness.k8s.model.KubernetesConfig;
import io.harness.logging.LogCallback;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1Service;
import io.kubernetes.client.openapi.models.VersionInfo;
import me.snowdrop.istio.api.IstioResource;
import me.snowdrop.istio.api.networking.v1alpha3.DestinationRule;
import me.snowdrop.istio.api.networking.v1alpha3.VirtualService;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Created by brett on 2/10/17.
 */
public interface KubernetesContainerService {
  HasMetadata createOrReplaceController(KubernetesConfig kubernetesConfig, HasMetadata definition);

  HasMetadata getController(KubernetesConfig kubernetesConfig, String name);

  @SuppressWarnings("squid:S1452")
  List<? extends HasMetadata> getControllers(KubernetesConfig kubernetesConfig, Map<String, String> labels);

  void validate(KubernetesConfig kubernetesConfig);

  @SuppressWarnings("squid:S1452") List<? extends HasMetadata> listControllers(KubernetesConfig kubernetesConfig);

  void deleteController(KubernetesConfig kubernetesConfig, String name);

  HorizontalPodAutoscaler createOrReplaceAutoscaler(KubernetesConfig kubernetesConfig, String autoscalerYaml);

  HorizontalPodAutoscaler getAutoscaler(KubernetesConfig kubernetesConfig, String name, String apiVersion);

  void deleteAutoscaler(KubernetesConfig kubernetesConfig, String name);

  List<ContainerInfo> setControllerPodCount(KubernetesConfig kubernetesConfig, String clusterName,
      String controllerName, int previousCount, int count, int serviceSteadyStateTimeout, LogCallback logCallback);

  @SuppressWarnings("squid:S00107")
  List<ContainerInfo> getContainerInfosWhenReady(KubernetesConfig kubernetesConfig, String controllerName,
      int previousCount, int desiredCount, int serviceSteadyStateTimeout, List<Pod> originalPods,
      boolean isNotVersioned, LogCallback logCallback, boolean wait, long startTime, String namespace);

  Optional<Integer> getControllerPodCount(KubernetesConfig kubernetesConfig, String name);

  Integer getControllerPodCount(HasMetadata controller);

  PodTemplateSpec getPodTemplateSpec(HasMetadata controller);

  LinkedHashMap<String, Integer> getActiveServiceCounts(KubernetesConfig kubernetesConfig, String containerServiceName);

  LinkedHashMap<String, Integer> getActiveServiceCountsWithLabels(
      KubernetesConfig kubernetesConfig, Map<String, String> labels);

  Map<String, String> getActiveServiceImages(
      KubernetesConfig kubernetesConfig, String containerServiceName, String imagePrefix);

  Service createOrReplaceService(KubernetesConfig kubernetesConfig, Service definition);

  Service getServiceFabric8(KubernetesConfig kubernetesConfig, String name, String namespace);

  Service getServiceFabric8(KubernetesConfig kubernetesConfig, String name);

  V1Service getService(KubernetesConfig kubernetesConfig, String name, String namespace);

  V1Service getService(KubernetesConfig kubernetesConfig, String name);

  List<Service> getServices(KubernetesConfig kubernetesConfig, Map<String, String> labels);

  void deleteService(KubernetesConfig kubernetesConfig, String name);

  Ingress createOrReplaceIngress(KubernetesConfig kubernetesConfig, Ingress definition);

  Ingress getIngress(KubernetesConfig kubernetesConfig, String name);

  void deleteIngress(KubernetesConfig kubernetesConfig, String name);

  ConfigMap createOrReplaceConfigMap(KubernetesConfig kubernetesConfig, ConfigMap definition);

  ConfigMap getConfigMap(KubernetesConfig kubernetesConfig, String name);

  void deleteConfigMap(KubernetesConfig kubernetesConfig, String name);

  DestinationRule getIstioDestinationRule(KubernetesConfig kubernetesConfig, String name);

  IstioResource createOrReplaceIstioResource(KubernetesConfig kubernetesConfig, IstioResource definition);

  void deleteIstioDestinationRule(KubernetesConfig kubernetesConfig, String name);

  int getTrafficPercent(KubernetesConfig kubernetesConfig, String controllerName);

  Map<String, Integer> getTrafficWeights(KubernetesConfig kubernetesConfig, String containerServiceName);

  void createNamespaceIfNotExist(KubernetesConfig kubernetesConfig);

  Secret getSecret(KubernetesConfig kubernetesConfig, String secretName);

  void deleteSecret(KubernetesConfig kubernetesConfig, String name);

  Secret createOrReplaceSecret(KubernetesConfig kubernetesConfig, Secret secret);

  List<Pod> getPods(KubernetesConfig kubernetesConfig, Map<String, String> labels);

  List<Pod> getRunningPods(KubernetesConfig kubernetesConfig, String controllerName);

  void waitForPodsToStop(KubernetesConfig kubernetesConfig, Map<String, String> labels, int serviceSteadyStateTimeout,
      List<Pod> originalPods, long startTime, LogCallback logCallback);

  String fetchReleaseHistory(KubernetesConfig kubernetesConfig, String infraMappingId);

  void saveReleaseHistory(KubernetesConfig kubernetesConfig, String infraMappingxId, String releaseHistory);

  String fetchReleaseHistoryFromSecrets(KubernetesConfig kubernetesConfig, String infraMappingId);

  void saveReleaseHistory(
      KubernetesConfig kubernetesConfig, String infraMappingId, String releaseHistory, boolean storeInSecrets);

  List<Pod> getRunningPodsWithLabelsFabric8(
      KubernetesConfig kubernetesConfig, String namespace, Map<String, String> labels);

  List<V1Pod> getRunningPodsWithLabels(KubernetesConfig kubernetesConfig, String namespace, Map<String, String> labels);

  void deleteIstioVirtualService(KubernetesConfig kubernetesConfig, String name);

  VirtualService getIstioVirtualService(KubernetesConfig kubernetesConfig, String name);

  CustomResourceDefinition getCustomResourceDefinition(KubernetesClient client, IstioResource resource);

  String getVersionAsStringFabric8(KubernetesConfig kubernetesConfig);

  VersionInfo getVersion(KubernetesConfig kubernetesConfig);

  String getVersionAsString(KubernetesConfig kubernetesConfig);

  void validateCEPermissions(KubernetesConfig kubernetesConfig);

  void tryListControllersKubectl(KubernetesConfig kubernetesConfig);

  String getConfigFileContent(KubernetesConfig config);

  HasMetadata getController(KubernetesConfig kubernetesConfig, String name, String namespace);
}
