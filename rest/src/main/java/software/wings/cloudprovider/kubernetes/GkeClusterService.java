package software.wings.cloudprovider.kubernetes;

import software.wings.beans.KubernetesConfig;

import java.util.List;
import java.util.Map;

/**
 * Created by bzane on 2/21/17.
 */
public interface GkeClusterService {
  KubernetesConfig createCluster(Map<String, String> params);

  void deleteCluster(Map<String, String> params);

  KubernetesConfig getCluster(Map<String, String> params);

  List<String> listClusters(Map<String, String> params);

  void setNodePoolAutoscaling(boolean enabled, int min, int max);

  boolean getNodePoolAutoscaling();
}
