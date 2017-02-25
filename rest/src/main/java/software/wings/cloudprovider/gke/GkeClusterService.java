package software.wings.cloudprovider.gke;

import com.google.api.services.container.model.NodePoolAutoscaling;
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
  boolean deleteCluster(Map<String, String> params);

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
  boolean setNodePoolAutoscaling(boolean enabled, int min, int max, Map<String, String> params);

  /**
   * Gets the node pool autoscaling settings
   */
  NodePoolAutoscaling getNodePoolAutoscaling(Map<String, String> params);
}
