package software.wings.cloudprovider.gke;

import com.google.api.services.container.model.NodePoolAutoscaling;
import software.wings.beans.KubernetesConfig;
import software.wings.beans.SettingAttribute;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

/**
 * Created by bzane on 2/21/17.
 */
public interface GkeClusterService {
  /**
   * Creates a new cluster unless a cluster with the given name already exists
   */
  KubernetesConfig createCluster(
      SettingAttribute computeProviderSetting, String zoneClusterName, Map<String, String> params);

  /**
   * Deletes the given cluster
   */
  boolean deleteCluster(SettingAttribute computeProviderSetting, String zoneClusterName);

  /**
   * Gets the details about a cluster
   */
  KubernetesConfig getCluster(SettingAttribute computeProviderSetting, String zoneClusterName);

  /**
   * Lists the available clusters
   */
  List<String> listClusters(SettingAttribute computeProviderSetting);
  /**
   * Sets whether node pool autoscaling is enabled
   */
  boolean setNodePoolAutoscaling(SettingAttribute computeProviderSetting, String zoneClusterName,
      @Nullable String nodePoolId, boolean enabled, int min, int max);

  /**
   * Gets the node pool autoscaling settings
   */
  NodePoolAutoscaling getNodePoolAutoscaling(
      SettingAttribute computeProviderSetting, String zoneClusterName, @Nullable String nodePoolId);
}
