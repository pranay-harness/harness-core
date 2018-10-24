package software.wings.cloudprovider.gke;

import com.google.api.services.container.model.NodePoolAutoscaling;

import software.wings.beans.GcpConfig;
import software.wings.beans.KubernetesConfig;
import software.wings.beans.SettingAttribute;
import software.wings.security.encryption.EncryptedDataDetail;

import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * Created by bzane on 2/21/17.
 */
public interface GkeClusterService {
  /**
   * Creates a new cluster unless a cluster with the given name already exists
   */
  KubernetesConfig createCluster(SettingAttribute computeProviderSetting,
      List<EncryptedDataDetail> encryptedDataDetails, String zoneClusterName, String namespace,
      Map<String, String> params);

  /**
   * Deletes the given cluster
   */
  boolean deleteCluster(
      SettingAttribute computeProviderSetting, List<EncryptedDataDetail> encryptedDataDetails, String zoneClusterName);

  /**
   * Gets the details about a cluster
   */
  KubernetesConfig getCluster(SettingAttribute computeProviderSetting, List<EncryptedDataDetail> encryptedDataDetails,
      String zoneClusterName, String namespace);

  KubernetesConfig getCluster(GcpConfig gcpConfig, List<EncryptedDataDetail> encryptedDataDetails,
      String locationClusterName, String namespace);

  /**
   * Lists the available clusters
   */
  List<String> listClusters(SettingAttribute computeProviderSetting, List<EncryptedDataDetail> encryptedDataDetails);

  /**
   * Sets whether node pool autoscaling is enabled
   */
  boolean setNodePoolAutoscaling(SettingAttribute computeProviderSetting,
      List<EncryptedDataDetail> encryptedDataDetails, String zoneClusterName, @Nullable String nodePoolId,
      boolean enabled, int min, int max);

  /**
   * Gets the node pool autoscaling settings
   */
  NodePoolAutoscaling getNodePoolAutoscaling(SettingAttribute computeProviderSetting,
      List<EncryptedDataDetail> encryptedDataDetails, String zoneClusterName, @Nullable String nodePoolId);
}
