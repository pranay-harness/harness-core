package software.wings.cloudprovider;

import software.wings.beans.KubernetesConfig;

import java.util.List;
import java.util.Map;

/**
 * Created by bzane on 2/22/17.
 */
public interface ClusterService {
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
}
