package software.wings.graphql.datafetcher.cluster;

import io.harness.ccm.cluster.ClusterRecordService;
import io.harness.ccm.cluster.entities.ClusterRecord;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;

import software.wings.graphql.datafetcher.AbstractObjectDataFetcher;
import software.wings.graphql.schema.query.QLClusterQueryParameters;
import software.wings.graphql.schema.type.QLCluster;
import software.wings.graphql.schema.type.QLCluster.QLClusterBuilder;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.annotations.AuthRule;

import com.google.inject.Inject;

public class ClusterDataFetcher extends AbstractObjectDataFetcher<QLCluster, QLClusterQueryParameters> {
  public static final String CLUSTERRECORD_DOES_NOT_EXIST_MSG = "Cluster record does not exist";
  @Inject ClusterRecordService clusterRecordService;

  @Override
  @AuthRule(permissionType = PermissionType.LOGGED_IN)
  public QLCluster fetch(QLClusterQueryParameters qlQuery, String accountId) {
    ClusterRecord clusterRecord = null;
    String clusterId = qlQuery.getClusterId();
    if (clusterId != null) {
      clusterRecord = clusterRecordService.get(clusterId);
    }
    if (clusterRecord == null || !clusterRecord.getAccountId().equals(accountId)) {
      throw new InvalidRequestException(CLUSTERRECORD_DOES_NOT_EXIST_MSG, WingsException.USER);
    }
    final QLClusterBuilder builder = QLCluster.builder();
    ClusterController.populateCluster(clusterRecord.getCluster(), clusterId, builder);
    return builder.build();
  }
}
