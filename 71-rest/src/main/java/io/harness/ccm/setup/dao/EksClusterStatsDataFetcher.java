package io.harness.ccm.setup.dao;

import com.google.inject.Inject;

import graphql.schema.DataFetchingEnvironment;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
import software.wings.beans.ce.CECluster;
import software.wings.graphql.datafetcher.AbstractConnectionV2DataFetcher;
import software.wings.graphql.schema.query.QLPageQueryParameters;
import software.wings.graphql.schema.type.aggregation.QLNoOpSortCriteria;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class EksClusterStatsDataFetcher
    extends AbstractConnectionV2DataFetcher<QLCESetupFilter, QLNoOpSortCriteria, QLEKSClusterData> {
  @Inject private CESetupQueryHelper CESetupQueryHelper;

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.LOGGED_IN)
  protected QLEKSClusterData fetchConnection(
      List<QLCESetupFilter> filters, QLPageQueryParameters pageQueryParameters, List<QLNoOpSortCriteria> sortCriteria) {
    Query<CECluster> query = populateFilters(wingsPersistence, filters, CECluster.class, true);
    final List<CECluster> eksClusterInfo = query.asList();

    int numberOfClusters = eksClusterInfo.size();
    List<QLEKSCluster> outputEKSClusterList = new ArrayList<>();
    for (CECluster ceCluster : eksClusterInfo) {
      outputEKSClusterList.add(populateEKSCluster(ceCluster));
    }

    return QLEKSClusterData.builder().count(numberOfClusters).clusters(outputEKSClusterList).build();
  }

  private QLEKSCluster populateEKSCluster(CECluster ceCluster) {
    return QLEKSCluster.builder()
        .id(ceCluster.getUuid())
        .name(ceCluster.getClusterName())
        .cloudProviderId(ceCluster.getCloudProviderId())
        .infraAccountId(ceCluster.getInfraAccountId())
        .infraMasterAccountId(ceCluster.getInfraMasterAccountId())
        .parentAccountSettingId(ceCluster.getParentAccountSettingId())
        .region(ceCluster.getRegion())
        .build();
  }

  @Override
  protected QLCESetupFilter generateFilter(DataFetchingEnvironment environment, String key, String value) {
    return null;
  }

  @Override
  protected void populateFilters(List<QLCESetupFilter> filters, Query query) {
    CESetupQueryHelper.setQuery(filters, query);
  }
}
