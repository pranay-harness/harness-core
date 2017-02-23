package software.wings.cloudprovider.gke;

import software.wings.cloudprovider.ClusterService;

/**
 * Created by bzane on 2/21/17.
 */
public interface GkeClusterService extends ClusterService {
  /**
   * Sets whether node pool autoscaling is enabled
   */
  void setNodePoolAutoscaling(boolean enabled, int min, int max);

  /**
   * Gets the node pool autoscaling settings
   */
  boolean getNodePoolAutoscaling();
}
