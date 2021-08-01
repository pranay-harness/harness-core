package io.harness.ccm.graphql.query.perspectives;

import static io.harness.annotations.dev.HarnessTeam.CE;
import static io.harness.ccm.commons.utils.BigQueryHelper.UNIFIED_TABLE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ccm.bigQuery.BigQueryService;
import io.harness.ccm.commons.utils.BigQueryHelper;
import io.harness.ccm.graphql.core.perspectives.PerspectiveFieldsHelper;
import io.harness.ccm.graphql.core.perspectives.PerspectiveOverviewStatsHelper;
import io.harness.ccm.graphql.core.perspectives.PerspectiveTimeSeriesHelper;
import io.harness.ccm.graphql.dto.common.StatsInfo;
import io.harness.ccm.graphql.dto.perspectives.PerspectiveData;
import io.harness.ccm.graphql.dto.perspectives.PerspectiveEntityStatsData;
import io.harness.ccm.graphql.dto.perspectives.PerspectiveFieldsData;
import io.harness.ccm.graphql.dto.perspectives.PerspectiveFilterData;
import io.harness.ccm.graphql.dto.perspectives.PerspectiveOverviewStatsData;
import io.harness.ccm.graphql.dto.perspectives.PerspectiveTimeSeriesData;
import io.harness.ccm.graphql.dto.perspectives.PerspectiveTrendStats;
import io.harness.ccm.graphql.utils.GraphQLUtils;
import io.harness.ccm.graphql.utils.annotations.GraphQLApi;
import io.harness.ccm.views.graphql.QLCEViewAggregation;
import io.harness.ccm.views.graphql.QLCEViewFilterWrapper;
import io.harness.ccm.views.graphql.QLCEViewGroupBy;
import io.harness.ccm.views.graphql.QLCEViewSortCriteria;
import io.harness.ccm.views.graphql.QLCEViewTrendData;
import io.harness.ccm.views.graphql.QLCEViewTrendInfo;
import io.harness.ccm.views.service.CEViewService;
import io.harness.ccm.views.service.ViewsBillingService;

import com.google.cloud.bigquery.BigQuery;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLEnvironment;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.execution.ResolutionEnvironment;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
@GraphQLApi
@OwnedBy(CE)
public class PerspectivesQuery {
  @Inject private GraphQLUtils graphQLUtils;
  @Inject ViewsBillingService viewsBillingService;
  @Inject private CEViewService viewService;
  @Inject BigQueryService bigQueryService;
  @Inject BigQueryHelper bigQueryHelper;
  @Inject PerspectiveOverviewStatsHelper perspectiveOverviewStatsHelper;
  @Inject PerspectiveTimeSeriesHelper perspectiveTimeSeriesHelper;
  @Inject PerspectiveFieldsHelper perspectiveFieldsHelper;

  @GraphQLQuery(name = "perspectiveTrendStats", description = "Trend stats for perspective")
  public PerspectiveTrendStats perspectiveTrendStats(
      @GraphQLArgument(name = "filters") List<QLCEViewFilterWrapper> filters,
      @GraphQLArgument(name = "aggregateFunction") List<QLCEViewAggregation> aggregateFunction,
      @GraphQLArgument(name = "isClusterQuery") Boolean isClusterQuery,
      @GraphQLEnvironment final ResolutionEnvironment env) {
    final String accountId = graphQLUtils.getAccountIdentifier(env);
    String cloudProviderTableName = bigQueryHelper.getCloudProviderTableName(accountId, UNIFIED_TABLE);
    BigQuery bigQuery = bigQueryService.get();
    isClusterQuery = isClusterQuery != null && isClusterQuery;

    QLCEViewTrendData trendStatsData = viewsBillingService.getTrendStatsDataNg(
        bigQuery, filters, aggregateFunction, cloudProviderTableName, accountId, isClusterQuery);
    return PerspectiveTrendStats.builder()
        .cost(getStats(trendStatsData.getTotalCost()))
        .idleCost(getStats(trendStatsData.getIdleCost()))
        .unallocatedCost(getStats(trendStatsData.getUnallocatedCost()))
        .systemCost(getStats(trendStatsData.getSystemCost()))
        .utilizedCost(getStats(trendStatsData.getUtilizedCost()))
        .efficiencyScoreStats(trendStatsData.getEfficiencyScoreStats())
        .build();
  }

  @GraphQLQuery(name = "perspectiveForecastCost", description = "Forecast cost for perspective")
  public PerspectiveTrendStats perspectiveForecastCost(
      @GraphQLArgument(name = "filters") List<QLCEViewFilterWrapper> filters,
      @GraphQLArgument(name = "aggregateFunction") List<QLCEViewAggregation> aggregateFunction,
      @GraphQLArgument(name = "isClusterQuery") Boolean isClusterQuery,
      @GraphQLEnvironment final ResolutionEnvironment env) {
    final String accountId = graphQLUtils.getAccountIdentifier(env);
    String cloudProviderTableName = bigQueryHelper.getCloudProviderTableName(accountId, UNIFIED_TABLE);
    BigQuery bigQuery = bigQueryService.get();
    isClusterQuery = isClusterQuery != null && isClusterQuery;

    QLCEViewTrendInfo forecastCostData = viewsBillingService.getForecastCostData(
        bigQuery, filters, aggregateFunction, cloudProviderTableName, accountId, isClusterQuery);
    return PerspectiveTrendStats.builder()
        .cost(StatsInfo.builder()
                  .statsTrend(forecastCostData.getStatsTrend())
                  .statsLabel(forecastCostData.getStatsLabel())
                  .statsDescription(forecastCostData.getStatsDescription())
                  .statsValue(forecastCostData.getStatsValue())
                  .value(forecastCostData.getValue())
                  .build())
        .build();
  }

  @GraphQLQuery(name = "perspectiveGrid", description = "Table for perspective")
  public PerspectiveEntityStatsData perspectiveGrid(
      @GraphQLArgument(name = "aggregateFunction") List<QLCEViewAggregation> aggregateFunction,
      @GraphQLArgument(name = "filters") List<QLCEViewFilterWrapper> filters,
      @GraphQLArgument(name = "groupBy") List<QLCEViewGroupBy> groupBy,
      @GraphQLArgument(name = "sortCriteria") List<QLCEViewSortCriteria> sortCriteria,
      @GraphQLArgument(name = "limit") Integer limit, @GraphQLArgument(name = "offset") Integer offset,
      @GraphQLArgument(name = "isClusterQuery") Boolean isClusterQuery,
      @GraphQLEnvironment final ResolutionEnvironment env) {
    final String accountId = graphQLUtils.getAccountIdentifier(env);
    String cloudProviderTableName = bigQueryHelper.getCloudProviderTableName(accountId, UNIFIED_TABLE);
    BigQuery bigQuery = bigQueryService.get();
    isClusterQuery = isClusterQuery != null && isClusterQuery;

    return PerspectiveEntityStatsData.builder()
        .data(viewsBillingService
                  .getEntityStatsDataPointsNg(bigQuery, filters, groupBy, aggregateFunction, sortCriteria,
                      cloudProviderTableName, limit, offset, accountId, false, isClusterQuery)
                  .getData())
        .build();
  }

  @GraphQLQuery(name = "perspectiveFilters", description = "Filter values for perspective")
  public PerspectiveFilterData perspectiveFilters(
      @GraphQLArgument(name = "aggregateFunction") List<QLCEViewAggregation> aggregateFunction,
      @GraphQLArgument(name = "filters") List<QLCEViewFilterWrapper> filters,
      @GraphQLArgument(name = "groupBy") List<QLCEViewGroupBy> groupBy,
      @GraphQLArgument(name = "sortCriteria") List<QLCEViewSortCriteria> sortCriteria,
      @GraphQLArgument(name = "limit") Integer limit, @GraphQLArgument(name = "offset") Integer offset,
      @GraphQLArgument(name = "isClusterQuery") Boolean isClusterQuery,
      @GraphQLEnvironment final ResolutionEnvironment env) {
    final String accountId = graphQLUtils.getAccountIdentifier(env);
    String cloudProviderTableName = bigQueryHelper.getCloudProviderTableName(accountId, UNIFIED_TABLE);
    BigQuery bigQuery = bigQueryService.get();
    isClusterQuery = isClusterQuery != null && isClusterQuery;

    return PerspectiveFilterData.builder()
        .values(viewsBillingService.getFilterValueStatsNg(
            bigQuery, filters, cloudProviderTableName, limit, offset, accountId, isClusterQuery))
        .build();
  }

  @GraphQLQuery(name = "perspectiveOverviewStats", description = "Overview stats for perspective")
  public PerspectiveOverviewStatsData perspectiveOverviewStats(@GraphQLEnvironment final ResolutionEnvironment env) {
    final String accountId = graphQLUtils.getAccountIdentifier(env);
    return perspectiveOverviewStatsHelper.fetch(accountId);
  }

  @GraphQLQuery(name = "perspectiveTimeSeriesStats", description = "Table for perspective")
  public PerspectiveTimeSeriesData perspectiveTimeSeriesStats(
      @GraphQLArgument(name = "aggregateFunction") List<QLCEViewAggregation> aggregateFunction,
      @GraphQLArgument(name = "filters") List<QLCEViewFilterWrapper> filters,
      @GraphQLArgument(name = "groupBy") List<QLCEViewGroupBy> groupBy,
      @GraphQLArgument(name = "sortCriteria") List<QLCEViewSortCriteria> sortCriteria,
      @GraphQLArgument(name = "limit") Integer limit, @GraphQLArgument(name = "offset") Integer offset,
      @GraphQLArgument(name = "includeOthers") boolean includeOthers,
      @GraphQLArgument(name = "isClusterQuery") Boolean isClusterQuery,
      @GraphQLEnvironment final ResolutionEnvironment env) {
    final String accountId = graphQLUtils.getAccountIdentifier(env);
    String cloudProviderTableName = bigQueryHelper.getCloudProviderTableName(accountId, UNIFIED_TABLE);
    BigQuery bigQuery = bigQueryService.get();
    long timePeriod = perspectiveTimeSeriesHelper.getTimePeriod(groupBy);
    isClusterQuery = isClusterQuery != null && isClusterQuery;

    PerspectiveTimeSeriesData data = perspectiveTimeSeriesHelper.fetch(
        viewsBillingService.getTimeSeriesStatsNg(bigQuery, filters, groupBy, aggregateFunction, sortCriteria,
            cloudProviderTableName, accountId, includeOthers, limit, isClusterQuery),
        timePeriod);

    return perspectiveTimeSeriesHelper.postFetch(data, limit, includeOthers);
  }

  @GraphQLQuery(name = "perspectiveFields", description = "Fields for perspective explorer")
  public PerspectiveFieldsData perspectiveFields(@GraphQLArgument(name = "filters") List<QLCEViewFilterWrapper> filters,
      @GraphQLEnvironment final ResolutionEnvironment env) {
    final String accountId = graphQLUtils.getAccountIdentifier(env);
    return perspectiveFieldsHelper.fetch(accountId, filters);
  }

  @GraphQLQuery(name = "perspectives", description = "Fetch perspectives for account")
  public PerspectiveData perspectives(@GraphQLEnvironment final ResolutionEnvironment env) {
    final String accountId = graphQLUtils.getAccountIdentifier(env);
    return PerspectiveData.builder().customerViews(viewService.getAllViews(accountId, true)).build();
  }

  private StatsInfo getStats(QLCEViewTrendInfo trendInfo) {
    if (trendInfo == null) {
      return null;
    }
    return StatsInfo.builder()
        .statsTrend(trendInfo.getStatsTrend())
        .statsLabel(trendInfo.getStatsLabel())
        .statsDescription(trendInfo.getStatsDescription())
        .statsValue(trendInfo.getStatsValue())
        .value(trendInfo.getValue())
        .build();
  }
}
