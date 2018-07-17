package software.wings.cloudprovider.gke;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.HorizontalPodAutoscaler;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NodeList;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.extensions.Ingress;
import me.snowdrop.istio.api.model.IstioResource;
import software.wings.beans.KubernetesConfig;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.cloudprovider.ContainerInfo;
import software.wings.security.encryption.EncryptedDataDetail;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Created by brett on 2/10/17.
 */
public interface KubernetesContainerService {
  List<Namespace> listNamespaces(KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails);

  HasMetadata createOrReplaceController(
      KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails, HasMetadata definition);

  HasMetadata getController(
      KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails, String name);

  List<? extends HasMetadata> getControllers(
      KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails, Map<String, String> labels);

  List<? extends HasMetadata> listControllers(
      KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails);

  void deleteController(KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails, String name);

  HorizontalPodAutoscaler createOrReplaceAutoscaler(
      KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails, String autoscalerYaml);

  HorizontalPodAutoscaler getAutoscaler(KubernetesConfig kubernetesConfig,
      List<EncryptedDataDetail> encryptedDataDetails, String name, String apiVersion);

  void deleteAutoscaler(KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails, String name);

  List<ContainerInfo> setControllerPodCount(KubernetesConfig kubernetesConfig,
      List<EncryptedDataDetail> encryptedDataDetails, String clusterName, String controllerName, int previousCount,
      int count, int serviceSteadyStateTimeout, ExecutionLogCallback executionLogCallback);

  List<ContainerInfo> getContainerInfosWhenReady(KubernetesConfig kubernetesConfig,
      List<EncryptedDataDetail> encryptedDataDetails, String controllerName, int previousCount,
      int serviceSteadyStateTimeout, List<Pod> originalPods, boolean isNotVersioned,
      ExecutionLogCallback executionLogCallback, boolean wait, long startTime);

  Optional<Integer> getControllerPodCount(
      KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails, String name);

  Integer getControllerPodCount(HasMetadata controller);

  PodTemplateSpec getPodTemplateSpec(HasMetadata controller);

  LinkedHashMap<String, Integer> getActiveServiceCounts(KubernetesConfig kubernetesConfig,
      List<EncryptedDataDetail> encryptedDataDetails, String containerServiceName, boolean useDashInHostName);

  LinkedHashMap<String, Integer> getActiveServiceCountsWithLabels(
      KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails, Map<String, String> labels);

  Map<String, String> getActiveServiceImages(KubernetesConfig kubernetesConfig,
      List<EncryptedDataDetail> encryptedDataDetails, String containerServiceName, String imagePrefix,
      boolean useDashInHostName);

  Service createOrReplaceService(
      KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails, Service definition);

  Service getService(KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails, String name);

  List<Service> getServices(
      KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails, Map<String, String> labels);

  List<Service> listServices(KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails);

  void deleteService(KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails, String name);

  Ingress createOrReplaceIngress(
      KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails, Ingress definition);

  Ingress getIngress(KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails, String name);

  void deleteIngress(KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails, String name);

  ConfigMap createOrReplaceConfigMap(
      KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails, ConfigMap definition);

  ConfigMap getConfigMap(
      KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails, String name);

  void deleteConfigMap(KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails, String name);

  IstioResource createOrReplaceRouteRule(
      KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails, IstioResource definition);

  IstioResource getRouteRule(
      KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails, String name);

  void deleteRouteRule(KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails, String name);

  int getTrafficPercent(KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails,
      String controllerName, boolean useDashInHostname);

  Map<String, Integer> getTrafficWeights(KubernetesConfig kubernetesConfig,
      List<EncryptedDataDetail> encryptedDataDetails, String containerServiceName, boolean useDashInHostname);

  void createNamespaceIfNotExist(KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails);

  Secret getSecret(
      KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails, String secretName);

  void deleteSecret(KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails, String name);

  Secret createOrReplaceSecret(
      KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails, Secret secret);

  List<Pod> getPods(
      KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails, Map<String, String> labels);

  List<Pod> getRunningPods(
      KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails, String controllerName);

  NodeList getNodes(KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails);

  void waitForPodsToStop(KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails,
      Map<String, String> labels, int serviceSteadyStateTimeout, List<Pod> originalPods, long startTime,
      ExecutionLogCallback executionLogCallback);
}
