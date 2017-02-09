package software.wings.cloudprovider;

import software.wings.beans.SettingAttribute;

import java.util.Map;

/**
 * Created by anubhaw on 12/28/16.
 */
public interface ContainerService {
  /**
   * Provision nodes.
   *
   * @param connectorConfig      the connector config
   * @param autoScalingGroupName the auto scaling group name
   * @param clusterSize          the cluster size
   */
  void provisionNodes(SettingAttribute connectorConfig, String autoScalingGroupName, Integer clusterSize);

  /**
   * Provision nodes.
   *
   * @param connectorConfig  the connector config
   * @param clusterSize      the cluster size
   * @param launchConfigName the launch config name
   * @param params           the params
   */
  void provisionNodes(
      SettingAttribute connectorConfig, Integer clusterSize, String launchConfigName, Map<String, Object> params);

  /**
   * Deploy service string.
   *
   * @param connectorConfig   the connector config
   * @param serviceDefinition the service definition
   * @return the string
   */
  String deployService(SettingAttribute connectorConfig, String serviceDefinition);

  /**
   * Delete service.
   *  @param connectorConfig the connector config
   * @param clusterName
   * @param serviceName     the service name
   */
  void deleteService(SettingAttribute connectorConfig, String clusterName, String serviceName);
}
