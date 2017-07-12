package software.wings.cloudprovider.gke;

import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.ReplicationControllerList;
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
   */
  ReplicationController createController(KubernetesConfig kubernetesConfig, ReplicationController definition);

  /**
   * Gets a replication controller.
   */
  ReplicationController getController(KubernetesConfig kubernetesConfig, String name);

  /**
   * Lists replication controllers.
   */
  ReplicationControllerList listControllers(KubernetesConfig kubernetesConfig);

  /**
   * Deletes a replication controller.
   */
  void deleteController(KubernetesConfig kubernetesConfig, String name);

  /**
   * Scales controller to specified number of nodes.
   */
  public List<ContainerInfo> setControllerPodCount(KubernetesConfig kubernetesConfig, String clusterName,
      String replicationControllerName, int number, ExecutionLogCallback executionLogCallback);

  /**
   * Gets the pod count of a replication controller.
   */
  int getControllerPodCount(KubernetesConfig kubernetesConfig, String name);

  /**
   * Creates a service.
   */
  Service createOrReplaceService(KubernetesConfig kubernetesConfig, Service definition);

  /**
   * Gets a service.
   */
  Service getService(KubernetesConfig kubernetesConfig, String name);

  /**
   * Lists services.
   */
  ServiceList listServices(KubernetesConfig kubernetesConfig);

  /**
   * Deletes a service.
   */
  void deleteService(KubernetesConfig kubernetesConfig, String name);
}
