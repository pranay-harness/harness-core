package io.harness.ccm.views.service;

import io.harness.ccm.views.graphql.QLCEViewAggregation;
import io.harness.ccm.views.graphql.QLCEViewEntityStatsDataPoint;
import io.harness.ccm.views.graphql.QLCEViewFilter;
import io.harness.ccm.views.graphql.QLCEViewFilterWrapper;
import io.harness.ccm.views.graphql.QLCEViewGroupBy;
import io.harness.ccm.views.graphql.QLCEViewSortCriteria;
import io.harness.ccm.views.graphql.QLCEViewTrendInfo;

import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.TableResult;
import java.util.List;

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
}
