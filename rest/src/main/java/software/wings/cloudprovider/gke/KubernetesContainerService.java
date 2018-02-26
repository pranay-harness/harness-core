package software.wings.cloudprovider.gke;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.HorizontalPodAutoscaler;
import io.fabric8.kubernetes.api.model.NodeList;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceList;
import io.fabric8.kubernetes.api.model.extensions.Ingress;
import software.wings.beans.KubernetesConfig;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.cloudprovider.ContainerInfo;
import software.wings.security.encryption.EncryptedDataDetail;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Created by brett on 2/10/17.
 */
public interface KubernetesContainerService {
  HasMetadata createController(
      KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails, HasMetadata definition);

  HasMetadata getController(
      KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails, String name);

  List<? extends HasMetadata> getControllers(
      KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails, Map<String, String> labels);

  List<? extends HasMetadata> listControllers(
      KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails);

  void deleteController(KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails, String name);

  HorizontalPodAutoscaler createAutoscaler(KubernetesConfig kubernetesConfig,
      List<EncryptedDataDetail> encryptedDataDetails, HorizontalPodAutoscaler definition);

  HorizontalPodAutoscaler getAutoscaler(
      KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails, String name);

  List<HorizontalPodAutoscaler> listAutoscalers(
      KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails);

  void disableAutoscaler(
      KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails, String name);

  void enableAutoscaler(KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails, String name);

  void deleteAutoscaler(KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails, String name);

  List<ContainerInfo> setControllerPodCount(KubernetesConfig kubernetesConfig,
      List<EncryptedDataDetail> encryptedDataDetails, String clusterName, String controllerName, int previousCount,
      int count, int serviceSteadyStateTimeout, ExecutionLogCallback executionLogCallback);

  List<ContainerInfo> getContainerInfosWhenReady(KubernetesConfig kubernetesConfig,
      List<EncryptedDataDetail> encryptedDataDetails, String controllerName, int previousCount, int desiredCount,
      int serviceSteadyStateTimeout, ExecutionLogCallback executionLogCallback);

  Optional<Integer> getControllerPodCount(
      KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails, String name);

  Integer getControllerPodCount(HasMetadata controller);

  Service createOrReplaceService(
      KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails, Service definition);

  Service getService(KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails, String name);

  ServiceList getServices(
      KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails, Map<String, String> labels);

  ServiceList listServices(KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails);

  void deleteService(KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails, String name);

  Ingress createOrReplaceIngress(
      KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails, Ingress definition);

  Ingress getIngress(KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails, String name);

  void deleteIngress(KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails, String name);

  ConfigMap createOrReplaceConfigMap(
      KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails, ConfigMap definition);

  void createNamespaceIfNotExist(KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails);

  Secret getSecret(
      KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails, String secretName);

  Secret createOrReplaceSecret(
      KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails, Secret secret);

  PodList getPods(
      KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails, Map<String, String> labels);

  NodeList getNodes(KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails);

  void waitForPodsToStop(KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails,
      Map<String, String> labels, int serviceSteadyStateTimeout, ExecutionLogCallback executionLogCallback);
}
