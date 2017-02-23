package software.wings.cloudprovider.gke;

import software.wings.beans.KubernetesConfig;

import java.util.List;
import java.util.Map;

/**
 * Created by bzane on 2/21/17.
 */
public interface GkeClusterService {
  /**
   * Creates a new cluster unless a cluster with the given name already exists
   */
  KubernetesConfig createCluster(Map<String, String> params);

  /**
   * Deletes the given cluster
   */
  void deleteCluster(Map<String, String> params);

  /**
   * Gets the details about a cluster
   */
  KubernetesConfig getCluster(Map<String, String> params);

  /**
   * Lists the available clusters
   */
  List<String> listClusters(Map<String, String> params);
  /**
   * Sets whether node pool autoscaling is enabled
   */
  void setNodePoolAutoscaling(boolean enabled, int min, int max);

  /**
   * Gets the node pool autoscaling settings
   */
  boolean getNodePoolAutoscaling();
}
