package software.wings.cloudprovider.aws;

import software.wings.beans.SettingAttribute;
import software.wings.cloudprovider.ContainerService;

/**
 * Created by anubhaw on 12/28/16.
 */
public interface EcsService extends ContainerService {
  /**
   * Provision tasks.
   *  @param connectorConfig the connector config
   * @param clusterName
   * @param serviceName     the service name
   * @param desiredCount    the desired count
   */
  void provisionTasks(SettingAttribute connectorConfig, String clusterName, String serviceName, Integer desiredCount);

  /**
   * Deprovision tasks.
   *
   * @param connectorConfig the connector config
   * @param serviceName     the service name
   * @param desiredCount    the desired count
   */
  void deprovisionTasks(SettingAttribute connectorConfig, String serviceName, Integer desiredCount);
}
