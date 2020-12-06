package io.harness.ccm.views.graphql;

import static software.wings.graphql.datafetcher.billing.CloudBillingHelper.unified;

import io.harness.ccm.billing.bigquery.BigQueryService;
import io.harness.ccm.views.service.ViewsBillingService;

import software.wings.graphql.datafetcher.AbstractStatsDataFetcherWithAggregationListAndLimit;
import software.wings.graphql.datafetcher.billing.CloudBillingHelper;
import software.wings.graphql.schema.type.aggregation.QLData;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;

import com.google.cloud.bigquery.BigQuery;
import com.google.inject.Inject;
import graphql.schema.DataFetchingEnvironment;
import java.util.List;

public class ViewFilterStatsDataFetcher extends AbstractStatsDataFetcherWithAggregationListAndLimit<QLCEViewAggregation,
    QLCEViewFilter, QLCEViewGroupBy, QLCEViewSortCriteria> {
  @Inject ViewsBillingService viewsBillingService;
  @Inject CloudBillingHelper cloudBillingHelper;
  @Inject BigQueryService bigQueryService;

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.LOGGED_IN)
  protected QLData fetch(String accountId, List<QLCEViewAggregation> aggregateFunction, List<QLCEViewFilter> filters,
      List<QLCEViewGroupBy> groupBy, List<QLCEViewSortCriteria> sort, Integer limit, Integer offset) {
    String cloudProviderTableName = cloudBillingHelper.getCloudProviderTableName(accountId, unified);
    BigQuery bigQuery = bigQueryService.get();
    return QLCEViewFilterData.builder()
        .values(viewsBillingService.getFilterValueStats(bigQuery, filters, cloudProviderTableName, limit, offset))
        .build();
  }

  @Override
  protected QLData postFetch(String accountId, List<QLCEViewGroupBy> groupByList,
      List<QLCEViewAggregation> aggregations, List<QLCEViewSortCriteria> sort, QLData qlData, Integer limit,
      boolean includeOthers) {
    return null;
  }

  @Override
  protected QLData fetchSelectedFields(String accountId, List<QLCEViewAggregation> aggregateFunction,
      List<QLCEViewFilter> filters, List<QLCEViewGroupBy> groupBy, List<QLCEViewSortCriteria> sort, Integer limit,
      Integer offset, DataFetchingEnvironment dataFetchingEnvironment) {
    return null;
  }

  @Override
  public String getEntityType() {
    return null;
  }
}
