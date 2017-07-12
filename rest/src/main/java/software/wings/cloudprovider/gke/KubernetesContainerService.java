package software.wings.cloudprovider.gke;

import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.ReplicationControllerList;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceList;
import software.wings.beans.KubernetesConfig;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.cloudprovider.ContainerInfo;

import java.util.List;

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
  ReplicationController createController(KubernetesConfig kubernetesConfig, ReplicationController definition);

  /**
   * Gets a replication controller.
   *
   * @param kubernetesConfig the kubernetes config
   * @param name             the name
   * @return the controller
   */
  ReplicationController getController(KubernetesConfig kubernetesConfig, String name);

  /**
   * Lists replication controllers.
   *
   * @param kubernetesConfig the kubernetes config
   * @return the replication controller list
   */
  ReplicationControllerList listControllers(KubernetesConfig kubernetesConfig);

  /**
   * Deletes a replication controller.
   *
   * @param kubernetesConfig the kubernetes config
   * @param name             the name
   */
  void deleteController(KubernetesConfig kubernetesConfig, String name);

  /**
   * Scales controller to specified number of nodes.
   *
   * @param kubernetesConfig          the kubernetes config
   * @param clusterName               the cluster name
   * @param replicationControllerName the replication controller name
   * @param number                    the number
   * @param executionLogCallback      the execution log callback
   * @return the controller pod count
   */
  public List<ContainerInfo> setControllerPodCount(KubernetesConfig kubernetesConfig, String clusterName,
      String replicationControllerName, int number, ExecutionLogCallback executionLogCallback);

  /**
   * Gets the pod count of a replication controller.
   *
   * @param kubernetesConfig the kubernetes config
   * @param name             the name
   * @return the controller pod count
   */
  int getControllerPodCount(KubernetesConfig kubernetesConfig, String name);

  /**
   * Creates a service.
   *
   * @param kubernetesConfig the kubernetes config
   * @param definition       the definition
   * @return the service
   */
  Service createService(KubernetesConfig kubernetesConfig, Service definition);

  /**
   * Gets a service.
   *
   * @param kubernetesConfig the kubernetes config
   * @param name             the name
   * @return the service
   */
  Service getService(KubernetesConfig kubernetesConfig, String name);

  /**
   * Lists services.
   *
   * @param kubernetesConfig the kubernetes config
   * @return the service list
   */
  ServiceList listServices(KubernetesConfig kubernetesConfig);

  /**
   * Deletes a service.
   *
   * @param kubernetesConfig the kubernetes config
   * @param name             the name
   */
  void deleteService(KubernetesConfig kubernetesConfig, String name);

  /**
   * Gets secret.
   *
   * @param kubernetesConfig the kubernetes config
   * @param secretName       the secret name
   * @return the secret
   */
  Secret getSecret(KubernetesConfig kubernetesConfig, String secretName);

  /**
   * Create secret secret.
   *
   * @param kubernetesConfig the kubernetes config
   * @param secret           the secret
   * @return the secret
   */
  Secret createSecret(KubernetesConfig kubernetesConfig, Secret secret);
}
