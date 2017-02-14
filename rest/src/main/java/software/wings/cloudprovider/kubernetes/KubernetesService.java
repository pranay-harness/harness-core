package software.wings.cloudprovider.kubernetes;

import io.fabric8.kubernetes.api.model.Quantity;
import software.wings.cloudprovider.ContainerService;

import java.util.Map;

/**
 * Created by brett on 2/10/17.
 */
public interface KubernetesService extends ContainerService {
  /**
   * Create frontend service.
   *
   * @param params map of parameters
   */
  void createFrontendService(Map<String, String> params);

  /**
   * Create backend service.
   *
   * @param params map of parameters
   */
  void createBackendService(Map<String, String> params);

  /**
   * Create frontend controller.
   *
   * @param requests resource requirement requests
   * @param params map of parameters
   */
  void createFrontendController(Map<String, Quantity> requests, Map<String, String> params);

  /**
   * Scale frontend controller to specified number of nodes.
   *
   * @param name frontend controller name
   * @param number number of nodes
   */
  void scaleFrontendController(String name, int number);

  /**
   * Create backend controller.
   *
   * @param params map of parameters
   */
  void createBackendController(Map<String, String> params);
}
