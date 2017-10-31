package software.wings.cloudprovider.gke;

import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.ReplicationControllerList;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceList;
import software.wings.beans.KubernetesConfig;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.cloudprovider.ContainerInfo;
import software.wings.security.encryption.EncryptedDataDetail;

import java.util.List;
import java.util.Map;

/**
 * Created by brett on 2/10/17.
 */
public interface KubernetesContainerService {
  /**
   * Creates a replication controller.
   *
   * @param kubernetesConfig the kubernetes config
   * @param definition       the definition
   * @return the replication controller
   */
  ReplicationController createController(KubernetesConfig kubernetesConfig,
      List<EncryptedDataDetail> encryptedDataDetails, ReplicationController definition);

  /**
   * Gets a replication controller.
   *
   * @param kubernetesConfig the kubernetes config
   * @param name             the name
   * @return the controller
   */
  ReplicationController getController(
      KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails, String name);

  ReplicationControllerList getControllers(
      KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails, Map<String, String> labels);

  /**
   * Lists replication controllers.
   *
   * @param kubernetesConfig the kubernetes config
   * @return the replication controller list
   */
  ReplicationControllerList listControllers(
      KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails);

  /**
   * Deletes a replication controller.
   *
   * @param kubernetesConfig the kubernetes config
   * @param name             the name
   */
  void deleteController(KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails, String name);

  /**
   * Scales controller to specified number of nodes.
   *
   * @param kubernetesConfig          the kubernetes config
   * @param clusterName               the cluster name
   * @param replicationControllerName the replication controller name
   * @param previousCount             the previous count
   * @param count                     the count
   * @param executionLogCallback      the execution log callback
   * @return the controller pod count
   */
  List<ContainerInfo> setControllerPodCount(KubernetesConfig kubernetesConfig,
      List<EncryptedDataDetail> encryptedDataDetails, String clusterName, String replicationControllerName,
      int previousCount, int count, ExecutionLogCallback executionLogCallback);

  /**
   * Gets the pod count of a replication controller.
   *
   * @param kubernetesConfig the kubernetes config
   * @param name             the name
   * @return the controller pod count
   */
  int getControllerPodCount(
      KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails, String name);

  /**
   * Creates a service.
   *
   * @param kubernetesConfig the kubernetes config
   * @param definition       the definition
   * @return the service
   */
  Service createOrReplaceService(
      KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails, Service definition);

  /**
   * Gets a service.
   *
   * @param kubernetesConfig the kubernetes config
   * @param name             the name
   * @return the service
   */
  Service getService(KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails, String name);

  ServiceList getServices(
      KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails, Map<String, String> labels);

  /**
   * Lists services.
   *
   * @param kubernetesConfig the kubernetes config
   * @return the service list
   */
  ServiceList listServices(KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails);

  /**
   * Deletes a service.
   *
   * @param kubernetesConfig the kubernetes config
   * @param name             the name
   */
  void deleteService(KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails, String name);

  void createNamespaceIfNotExist(KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails);

  /**
   * Gets secret.
   *
   * @param kubernetesConfig the kubernetes config
   * @param secretName       the secret name
   * @return the secret
   */
  Secret getSecret(
      KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails, String secretName);

  /**
   * Create secret secret.
   *
   * @param kubernetesConfig the kubernetes config
   * @param secret           the secret
   * @return the secret
   */
  Secret createOrReplaceSecret(
      KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails, Secret secret);

  PodList getPods(
      KubernetesConfig kubernetesConfig, List<EncryptedDataDetail> encryptedDataDetails, Map<String, String> labels);
}
