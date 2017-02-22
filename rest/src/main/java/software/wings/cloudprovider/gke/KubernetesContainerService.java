package software.wings.cloudprovider.gke;

import software.wings.beans.KubernetesConfig;

import java.util.Map;

/**
 * Created by brett on 2/10/17.
 */
public interface KubernetesContainerService {
  /**
   * Creates a replication controller.
   */
  void createController(KubernetesConfig config, Map<String, String> params);

  /**
   * Deletes a replication controller.
   */
  void deleteController(KubernetesConfig config, Map<String, String> params);

  /**
   * Scales controller to specified number of nodes.
   */
  void setControllerPodCount(KubernetesConfig config, String name, int number);

  /**
   * Gets the pod count of a replication controller.
   */
  int getControllerPodCount(KubernetesConfig config, String name);

  /**
   * Creates a service.
   */
  void createService(KubernetesConfig config, Map<String, String> params);

  /**
   * Deletes a service.
   */
  void deleteService(KubernetesConfig config, Map<String, String> params);
}
