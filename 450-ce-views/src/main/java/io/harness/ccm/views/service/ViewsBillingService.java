package io.harness.ccm.views.service;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ccm.views.graphql.QLCEViewAggregation;
import io.harness.ccm.views.graphql.QLCEViewEntityStatsDataPoint;
import io.harness.ccm.views.graphql.QLCEViewFilterWrapper;
import io.harness.ccm.views.graphql.QLCEViewGridData;
import io.harness.ccm.views.graphql.QLCEViewGroupBy;
import io.harness.ccm.views.graphql.QLCEViewSortCriteria;
import io.harness.ccm.views.graphql.QLCEViewTrendData;
import io.harness.ccm.views.graphql.QLCEViewTrendInfo;

import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.TableResult;
import java.util.List;

@OwnedBy(CE)
public interface ViewsBillingService {
  List<String> getFilterValueStats(BigQuery bigQuery, List<QLCEViewFilterWrapper> filters,
      String cloudProviderTableName, Integer limit, Integer offset);

  List<QLCEViewEntityStatsDataPoint> getEntityStatsDataPoints(BigQuery bigQuery, List<QLCEViewFilterWrapper> filters,
      List<QLCEViewGroupBy> groupBy, List<QLCEViewAggregation> aggregateFunction, List<QLCEViewSortCriteria> sort,
      String cloudProviderTableName, Integer limit, Integer offset);

  TableResult getTimeSeriesStats(BigQuery bigQuery, List<QLCEViewFilterWrapper> filters, List<QLCEViewGroupBy> groupBy,
      List<QLCEViewAggregation> aggregateFunction, List<QLCEViewSortCriteria> sort, String cloudProviderTableName);

  QLCEViewTrendInfo getTrendStatsData(BigQuery bigQuery, List<QLCEViewFilterWrapper> filters,
      List<QLCEViewAggregation> aggregateFunction, String cloudProviderTableName);

  List<String> getColumnsForTable(BigQuery bigQuery, String informationSchemaView, String table);

  boolean isClusterPerspective(List<QLCEViewFilterWrapper> filters);

  // For NG perspective queries
  QLCEViewGridData getEntityStatsDataPointsNg(BigQuery bigQuery, List<QLCEViewFilterWrapper> filters,
      List<QLCEViewGroupBy> groupBy, List<QLCEViewAggregation> aggregateFunction, List<QLCEViewSortCriteria> sort,
      String cloudProviderTableName, Integer limit, Integer offset, String accountId, boolean isUsedByTimeSeriesStats,
      boolean isClusterQuery);

  List<String> getFilterValueStatsNg(BigQuery bigQuery, List<QLCEViewFilterWrapper> filters,
      String cloudProviderTableName, Integer limit, Integer offset, String accountId, boolean isClusterQuery);

  QLCEViewTrendData getTrendStatsDataNg(BigQuery bigQuery, List<QLCEViewFilterWrapper> filters,
      List<QLCEViewAggregation> aggregateFunction, String cloudProviderTableName, String accountId,
      boolean isClusterQuery);

  TableResult getTimeSeriesStatsNg(BigQuery bigQuery, List<QLCEViewFilterWrapper> filters,
      List<QLCEViewGroupBy> groupBy, List<QLCEViewAggregation> aggregateFunction, List<QLCEViewSortCriteria> sort,
      String cloudProviderTableName, String accountId, boolean includeOthers, Integer limit, boolean isClusterQuery);

  QLCEViewTrendInfo getForecastCostData(BigQuery bigQuery, List<QLCEViewFilterWrapper> filters,
      List<QLCEViewAggregation> aggregateFunction, String cloudProviderTableName, String accountId,
      boolean isClusterQuery);
}
