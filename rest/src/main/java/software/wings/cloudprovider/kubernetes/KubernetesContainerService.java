package software.wings.cloudprovider.kubernetes;

import java.util.Map;

/**
 * Created by brett on 2/10/17.
 */
public interface KubernetesContainerService {
  /**
   * Create service.
   *
   * @param params map of parameters
   */
  void createService(Map<String, String> params);

  /**
   * Create a replication controller.
   *
   * @param params map of parameters
   */
  void createController(Map<String, String> params);

  /**
   * Scale controller to specified number of nodes.
   *
   * @param name frontend controller name
   * @param number number of nodes
   */
  void setControllerPodCount(String name, int number);
}
