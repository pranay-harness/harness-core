package software.wings.graphql.datafetcher.cluster;

import com.google.inject.Inject;

import io.harness.ccm.cluster.ClusterRecordService;
import io.harness.ccm.cluster.entities.Cluster;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import software.wings.graphql.datafetcher.AbstractObjectDataFetcher;
import software.wings.graphql.schema.query.QLClusterQueryParameters;
import software.wings.graphql.schema.type.QLCluster;
import software.wings.graphql.schema.type.QLCluster.QLClusterBuilder;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.annotations.AuthRule;

public class ClusterDataFetcher extends AbstractObjectDataFetcher<QLCluster, QLClusterQueryParameters> {
  public static final String CLUSTERRECORD_DOES_NOT_EXIST_MSG = "Cluster record does not exist";
  @Inject ClusterRecordService clusterRecordService;

  @Override
  @AuthRule(permissionType = PermissionType.LOGGED_IN)
  public QLCluster fetch(QLClusterQueryParameters qlQuery, String accountId) {
    Cluster cluster = null;
    String clusterId = qlQuery.getClusterId();
    if (clusterId != null) {
      cluster = clusterRecordService.get(clusterId).getCluster();
    }
    if (cluster == null) {
      throw new InvalidRequestException(CLUSTERRECORD_DOES_NOT_EXIST_MSG, WingsException.USER);
    }
    final QLClusterBuilder builder = QLCluster.builder();
    ClusterController.populateCluster(cluster, clusterId, builder);
    return builder.build();
  }
}
