package software.wings.cloudprovider.gke;

import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.Service;
import software.wings.beans.KubernetesConfig;

import java.util.Map;

/**
 * Created by brett on 2/10/17.
 */
public interface KubernetesContainerService {
  /**
   * Creates a replication controller.
   */
  ReplicationController createController(KubernetesConfig kubernetesConfig, Map<String, String> params);

  /**
   * Deletes a replication controller.
   */
  void deleteController(KubernetesConfig kubernetesConfig, String name);

  /**
   * Gets a replication controller.
   */
  ReplicationController getController(KubernetesConfig kubernetesConfig, String name);

  /**
   * Scales controller to specified number of nodes.
   */
  void setControllerPodCount(KubernetesConfig kubernetesConfig, String name, int number);

  /**
   * Gets the pod count of a replication controller.
   */
  int getControllerPodCount(KubernetesConfig kubernetesConfig, String name);

  /**
   * Creates a service.
   */
  Service createService(KubernetesConfig kubernetesConfig, Map<String, String> params);

  /**
   * Deletes a service.
   */
  void deleteService(KubernetesConfig kubernetesConfig, String name);
}
