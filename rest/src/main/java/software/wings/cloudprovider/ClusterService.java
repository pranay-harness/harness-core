package software.wings.cloudprovider;

import software.wings.beans.SettingAttribute;

/**
 * Created by anubhaw on 12/29/16.
 */
public interface ClusterService {
  /**
   * Create cluster.
   *
   * @param cloudProviderSetting the cloud provider setting
   * @param clusterConfiguration the cluster configuration
   */
  void createCluster(SettingAttribute cloudProviderSetting, ClusterConfiguration clusterConfiguration);

  /**
   * Resize cluster.
   *
   * @param cloudProviderSetting the cloud provider setting
   * @param clusterName          the cluster name
   * @param serviceName          the service name
   * @param desiredSize          the desired size
   */
  void resizeCluster(SettingAttribute cloudProviderSetting, String clusterName, String serviceName, Integer desiredSize,
      String autoScalingGroupName);

  /**
   * Destroy cluster.
   *
   * @param cloudProviderSetting the cloud provider setting
   * @param clusterName          the cluster name
   * @param serviceName          the service name
   */
  void destroyCluster(SettingAttribute cloudProviderSetting, String clusterName, String serviceName);
}
