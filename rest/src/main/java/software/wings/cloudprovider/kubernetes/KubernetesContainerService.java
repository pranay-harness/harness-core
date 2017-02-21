package software.wings.cloudprovider.kubernetes;

import software.wings.beans.KubernetesConfig;

import java.util.Map;

/**
 * Created by brett on 2/10/17.
 */
public interface KubernetesContainerService {
  /**
   * Create a replication controller.
   *
   * @param params map of parameters
   */
  void createController(KubernetesConfig config, Map<String, String> params);

  void deleteController(KubernetesConfig config, Map<String, String> params);

  /**
   * Scale controller to specified number of nodes.
   *
   * @param name frontend controller name
   * @param number number of nodes
   */
  void setControllerPodCount(KubernetesConfig config, String name, int number);

  int getControllerPodCount(KubernetesConfig config, String name);

  /**
   * Create service.
   *
   * @param params map of parameters
   */
  void createService(KubernetesConfig config, Map<String, String> params);

  void deleteService(KubernetesConfig config, Map<String, String> params);
}
