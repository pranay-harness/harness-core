package io.harness.ccm.views.service;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ccm.views.entities.ViewQueryParams;
import io.harness.ccm.views.graphql.QLCEViewAggregation;
import io.harness.ccm.views.graphql.QLCEViewEntityStatsDataPoint;
import io.harness.ccm.views.graphql.QLCEViewFilterWrapper;
import io.harness.ccm.views.graphql.QLCEViewGridData;
import io.harness.ccm.views.graphql.QLCEViewGroupBy;
import io.harness.ccm.views.graphql.QLCEViewSortCriteria;
import io.harness.ccm.views.graphql.QLCEViewTrendData;
import io.harness.ccm.views.graphql.QLCEViewTrendInfo;
import io.harness.ccm.views.graphql.ViewCostData;

import com.google.cloud.bigquery.TableResult;
import java.util.List;

@OwnedBy(CE)
public interface ViewsBillingService {
  List<String> getFilterValueStats(
      List<QLCEViewFilterWrapper> filters, String cloudProviderTableName, Integer limit, Integer offset);

  List<QLCEViewEntityStatsDataPoint> getEntityStatsDataPoints(List<QLCEViewFilterWrapper> filters,
      List<QLCEViewGroupBy> groupBy, List<QLCEViewAggregation> aggregateFunction, List<QLCEViewSortCriteria> sort,
      String cloudProviderTableName, Integer limit, Integer offset);

  TableResult getTimeSeriesStats(List<QLCEViewFilterWrapper> filters, List<QLCEViewGroupBy> groupBy,
      List<QLCEViewAggregation> aggregateFunction, List<QLCEViewSortCriteria> sort, String cloudProviderTableName);

  QLCEViewTrendInfo getTrendStatsData(
      List<QLCEViewFilterWrapper> filters, List<QLCEViewAggregation> aggregateFunction, String cloudProviderTableName);

  List<String> getColumnsForTable(String informationSchemaView, String table);

  boolean isClusterPerspective(List<QLCEViewFilterWrapper> filters);

  // For NG perspective queries
  QLCEViewGridData getEntityStatsDataPointsNg(List<QLCEViewFilterWrapper> filters, List<QLCEViewGroupBy> groupBy,
      List<QLCEViewAggregation> aggregateFunction, List<QLCEViewSortCriteria> sort, String cloudProviderTableName,
      Integer limit, Integer offset, ViewQueryParams queryParams);

  List<String> getFilterValueStatsNg(List<QLCEViewFilterWrapper> filters, String cloudProviderTableName, Integer limit,
      Integer offset, ViewQueryParams queryParams);

  QLCEViewTrendData getTrendStatsDataNg(List<QLCEViewFilterWrapper> filters,
      List<QLCEViewAggregation> aggregateFunction, String cloudProviderTableName, ViewQueryParams queryParams);

  TableResult getTimeSeriesStatsNg(List<QLCEViewFilterWrapper> filters, List<QLCEViewGroupBy> groupBy,
      List<QLCEViewAggregation> aggregateFunction, List<QLCEViewSortCriteria> sort, String cloudProviderTableName,
      boolean includeOthers, Integer limit, ViewQueryParams queryParams);

  QLCEViewTrendInfo getForecastCostData(List<QLCEViewFilterWrapper> filters,
      List<QLCEViewAggregation> aggregateFunction, String cloudProviderTableName, ViewQueryParams queryParams);

  ViewCostData getCostData(List<QLCEViewFilterWrapper> filters, List<QLCEViewAggregation> aggregateFunction,
      String cloudProviderTableName, ViewQueryParams queryParams);

  Integer getTotalCountForQuery(List<QLCEViewFilterWrapper> filters, List<QLCEViewGroupBy> groupBy,
      String cloudProviderTableName, ViewQueryParams queryParams);
}
