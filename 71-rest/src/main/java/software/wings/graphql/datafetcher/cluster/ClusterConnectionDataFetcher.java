package software.wings.graphql.datafetcher.cluster;

import graphql.schema.DataFetchingEnvironment;
import io.harness.ccm.cluster.entities.ClusterRecord;
import io.harness.ccm.cluster.entities.ClusterRecord.ClusterRecordKeys;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.Sort;
import software.wings.graphql.datafetcher.AbstractConnectionV2DataFetcher;
import software.wings.graphql.schema.query.QLPageQueryParameters;
import software.wings.graphql.schema.type.QLCluster;
import software.wings.graphql.schema.type.QLCluster.QLClusterBuilder;
import software.wings.graphql.schema.type.QLClusterConnection;
import software.wings.graphql.schema.type.QLClusterConnection.QLClusterConnectionBuilder;
import software.wings.graphql.schema.type.aggregation.QLNoOpSortCriteria;
import software.wings.graphql.schema.type.aggregation.cluster.QLClusterFilter;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;

import java.util.List;

@Slf4j
public class ClusterConnectionDataFetcher
    extends AbstractConnectionV2DataFetcher<QLClusterFilter, QLNoOpSortCriteria, QLClusterConnection> {
  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.LOGGED_IN)
  public QLClusterConnection fetchConnection(List<QLClusterFilter> clusterFilters,
      QLPageQueryParameters pageQueryParameters, List<QLNoOpSortCriteria> sortCriteria) {
    Query<ClusterRecord> query = populateFilters(wingsPersistence, null, ClusterRecord.class, true)
                                     .order(Sort.descending(ClusterRecordKeys.createdAt));

    QLClusterConnectionBuilder connectionBuilder = QLClusterConnection.builder();
    connectionBuilder.pageInfo(utils.populate(pageQueryParameters, query, clusterRecord -> {
      QLClusterBuilder builder = QLCluster.builder();
      ClusterController.populateCluster(clusterRecord.getCluster(), clusterRecord.getUuid(), builder);
      connectionBuilder.node(builder.build());
    }));
    return connectionBuilder.build();
  }

  @Override
  protected void populateFilters(List<QLClusterFilter> filters, Query query) {
    // do nothing
  }

  @Override
  protected QLClusterFilter generateFilter(DataFetchingEnvironment environment, String key, String value) {
    return null;
  }
}