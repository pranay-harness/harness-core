package software.wings.cloudprovider.gke;

import io.fabric8.kubernetes.api.model.ReplicationController;
import io.fabric8.kubernetes.api.model.Service;
import software.wings.beans.SettingAttribute;

import java.util.Map;

/**
 * Created by brett on 2/10/17.
 */
public interface KubernetesContainerService {
  /**
   * Creates a replication controller.
   */
  ReplicationController createController(SettingAttribute settingAttribute, Map<String, String> params);

  /**
   * Deletes a replication controller.
   */
  void deleteController(SettingAttribute settingAttribute, Map<String, String> params);

  /**
   * Scales controller to specified number of nodes.
   */
  void setControllerPodCount(SettingAttribute settingAttribute, String name, int number);

  /**
   * Gets the pod count of a replication controller.
   */
  int getControllerPodCount(SettingAttribute settingAttribute, String name);

  /**
   * Creates a service.
   */
  Service createService(SettingAttribute settingAttribute, Map<String, String> params);

  /**
   * Deletes a service.
   */
  void deleteService(SettingAttribute settingAttribute, Map<String, String> params);
}
