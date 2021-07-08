package io.harness.ccm.views.service.impl;

import static io.harness.annotations.dev.HarnessTeam.CE;
import static io.harness.beans.FeatureName.CE_BILLING_DATA_PRE_AGGREGATION;
import static io.harness.ccm.views.entities.ViewFieldIdentifier.CLUSTER;
import static io.harness.ccm.views.graphql.QLCEViewAggregateOperation.MAX;
import static io.harness.ccm.views.graphql.QLCEViewAggregateOperation.MIN;
import static io.harness.ccm.views.graphql.QLCEViewTimeFilterOperator.AFTER;
import static io.harness.ccm.views.graphql.ViewMetaDataConstants.entityConstantClusterCost;
import static io.harness.ccm.views.graphql.ViewMetaDataConstants.entityConstantCost;
import static io.harness.ccm.views.graphql.ViewMetaDataConstants.entityConstantIdleCost;
import static io.harness.ccm.views.graphql.ViewMetaDataConstants.entityConstantMaxStartTime;
import static io.harness.ccm.views.graphql.ViewMetaDataConstants.entityConstantMinStartTime;
import static io.harness.ccm.views.graphql.ViewMetaDataConstants.entityConstantUnallocatedCost;
import static io.harness.ccm.views.utils.ClusterTableKeys.ACTUAL_IDLE_COST;
import static io.harness.ccm.views.utils.ClusterTableKeys.BILLING_AMOUNT;
import static io.harness.ccm.views.utils.ClusterTableKeys.CLOUD_SERVICE_NAME;
import static io.harness.ccm.views.utils.ClusterTableKeys.CLUSTER_NAME;
import static io.harness.ccm.views.utils.ClusterTableKeys.CLUSTER_TABLE;
import static io.harness.ccm.views.utils.ClusterTableKeys.CLUSTER_TABLE_AGGREGRATED;
import static io.harness.ccm.views.utils.ClusterTableKeys.CLUSTER_TABLE_HOURLY;
import static io.harness.ccm.views.utils.ClusterTableKeys.CLUSTER_TABLE_HOURLY_AGGREGRATED;
import static io.harness.ccm.views.utils.ClusterTableKeys.COST;
import static io.harness.ccm.views.utils.ClusterTableKeys.DEFAULT_STRING_VALUE;
import static io.harness.ccm.views.utils.ClusterTableKeys.EFFECTIVE_CPU_LIMIT;
import static io.harness.ccm.views.utils.ClusterTableKeys.EFFECTIVE_CPU_REQUEST;
import static io.harness.ccm.views.utils.ClusterTableKeys.EFFECTIVE_CPU_UTILIZATION_VALUE;
import static io.harness.ccm.views.utils.ClusterTableKeys.EFFECTIVE_MEMORY_LIMIT;
import static io.harness.ccm.views.utils.ClusterTableKeys.EFFECTIVE_MEMORY_REQUEST;
import static io.harness.ccm.views.utils.ClusterTableKeys.EFFECTIVE_MEMORY_UTILIZATION_VALUE;
import static io.harness.ccm.views.utils.ClusterTableKeys.GROUP_BY_CLUSTER_NAME;
import static io.harness.ccm.views.utils.ClusterTableKeys.GROUP_BY_ECS_LAUNCH_TYPE;
import static io.harness.ccm.views.utils.ClusterTableKeys.GROUP_BY_ECS_LAUNCH_TYPE_ID;
import static io.harness.ccm.views.utils.ClusterTableKeys.GROUP_BY_ECS_SERVICE;
import static io.harness.ccm.views.utils.ClusterTableKeys.GROUP_BY_ECS_SERVICE_ID;
import static io.harness.ccm.views.utils.ClusterTableKeys.GROUP_BY_ECS_TASK;
import static io.harness.ccm.views.utils.ClusterTableKeys.GROUP_BY_ECS_TASK_ID;
import static io.harness.ccm.views.utils.ClusterTableKeys.GROUP_BY_NAMESPACE;
import static io.harness.ccm.views.utils.ClusterTableKeys.GROUP_BY_NAMESPACE_ID;
import static io.harness.ccm.views.utils.ClusterTableKeys.GROUP_BY_PRODUCT;
import static io.harness.ccm.views.utils.ClusterTableKeys.GROUP_BY_WORKLOAD_ID;
import static io.harness.ccm.views.utils.ClusterTableKeys.GROUP_BY_WORKLOAD_NAME;
import static io.harness.ccm.views.utils.ClusterTableKeys.GROUP_BY_WORKLOAD_TYPE;
import static io.harness.ccm.views.utils.ClusterTableKeys.ID_SEPARATOR;
import static io.harness.ccm.views.utils.ClusterTableKeys.INSTANCE_NAME;
import static io.harness.ccm.views.utils.ClusterTableKeys.LAUNCH_TYPE;
import static io.harness.ccm.views.utils.ClusterTableKeys.NAMESPACE;
import static io.harness.ccm.views.utils.ClusterTableKeys.PARENT_INSTANCE_ID;
import static io.harness.ccm.views.utils.ClusterTableKeys.TASK_ID;
import static io.harness.ccm.views.utils.ClusterTableKeys.WORKLOAD_NAME;
import static io.harness.ccm.views.utils.ClusterTableKeys.WORKLOAD_TYPE;

import static java.lang.String.format;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ccm.views.entities.CEView;
import io.harness.ccm.views.entities.ClusterData;
import io.harness.ccm.views.entities.ClusterData.ClusterDataBuilder;
import io.harness.ccm.views.entities.ViewCondition;
import io.harness.ccm.views.entities.ViewField;
import io.harness.ccm.views.entities.ViewFieldIdentifier;
import io.harness.ccm.views.entities.ViewIdCondition;
import io.harness.ccm.views.entities.ViewIdOperator;
import io.harness.ccm.views.entities.ViewRule;
import io.harness.ccm.views.entities.ViewTimeGranularity;
import io.harness.ccm.views.entities.ViewVisualization;
import io.harness.ccm.views.graphql.EfficiencyScoreStats;
import io.harness.ccm.views.graphql.QLCEViewAggregateOperation;
import io.harness.ccm.views.graphql.QLCEViewAggregation;
import io.harness.ccm.views.graphql.QLCEViewDataPoint;
import io.harness.ccm.views.graphql.QLCEViewDataPoint.QLCEViewDataPointBuilder;
import io.harness.ccm.views.graphql.QLCEViewEntityStatsDataPoint;
import io.harness.ccm.views.graphql.QLCEViewEntityStatsDataPoint.QLCEViewEntityStatsDataPointBuilder;
import io.harness.ccm.views.graphql.QLCEViewFieldInput;
import io.harness.ccm.views.graphql.QLCEViewFilter;
import io.harness.ccm.views.graphql.QLCEViewFilterOperator;
import io.harness.ccm.views.graphql.QLCEViewFilterWrapper;
import io.harness.ccm.views.graphql.QLCEViewGridData;
import io.harness.ccm.views.graphql.QLCEViewGroupBy;
import io.harness.ccm.views.graphql.QLCEViewMetadataFilter;
import io.harness.ccm.views.graphql.QLCEViewRule;
import io.harness.ccm.views.graphql.QLCEViewSortCriteria;
import io.harness.ccm.views.graphql.QLCEViewSortType;
import io.harness.ccm.views.graphql.QLCEViewTimeFilter;
import io.harness.ccm.views.graphql.QLCEViewTimeFilterOperator;
import io.harness.ccm.views.graphql.QLCEViewTimeGroupType;
import io.harness.ccm.views.graphql.QLCEViewTimeSeriesData;
import io.harness.ccm.views.graphql.QLCEViewTimeTruncGroupBy;
import io.harness.ccm.views.graphql.QLCEViewTrendInfo;
import io.harness.ccm.views.graphql.ViewCostData;
import io.harness.ccm.views.graphql.ViewCostData.ViewCostDataBuilder;
import io.harness.ccm.views.graphql.ViewsMetaDataFields;
import io.harness.ccm.views.graphql.ViewsQueryBuilder;
import io.harness.ccm.views.graphql.ViewsQueryHelper;
import io.harness.ccm.views.graphql.ViewsQueryMetadata;
import io.harness.ccm.views.service.CEViewService;
import io.harness.ccm.views.service.ViewsBillingService;
import io.harness.exception.InvalidRequestException;
import io.harness.ff.FeatureFlagService;

import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.Field;
import com.google.cloud.bigquery.FieldList;
import com.google.cloud.bigquery.FieldValue;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.Schema;
import com.google.cloud.bigquery.StandardSQLTypeName;
import com.google.cloud.bigquery.TableResult;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.healthmarketscience.sqlbuilder.SelectQuery;
import com.healthmarketscience.sqlbuilder.custom.postgresql.PgLimitClause;
import com.healthmarketscience.sqlbuilder.custom.postgresql.PgOffsetClause;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
@OwnedBy(CE)
public class ViewsBillingServiceImpl implements ViewsBillingService {
  @Inject ViewsQueryBuilder viewsQueryBuilder;
  @Inject CEViewService viewService;
  @Inject ViewsQueryHelper viewsQueryHelper;
  @Inject FeatureFlagService featureFlagService;

  public static final String nullStringValueConstant = "Others";
  private static final String COST_DESCRIPTION = "of %s - %s";
  private static final String COST_VALUE = "$%s";
  private static final String TOTAL_COST_LABEL = "Total Cost";
  private static final String FORECAST_COST_LABEL = "Forecasted Cost";
  private static final String DATE_PATTERN_FOR_CHART = "MMM dd";
  private static final long ONE_DAY_MILLIS = 86400000L;
  private static final Double defaultDoubleValue = 0D;
  private static final String STANDARD_TIME_ZONE = "GMT";
  private static final long OBSERVATION_PERIOD = 29 * ONE_DAY_MILLIS;

  @Override
  public List<String> getFilterValueStats(BigQuery bigQuery, List<QLCEViewFilterWrapper> filters,
      String cloudProviderTableName, Integer limit, Integer offset) {
    return getFilterValueStatsNg(bigQuery, filters, cloudProviderTableName, limit, offset, null);
  }

  @Override
  public List<String> getFilterValueStatsNg(BigQuery bigQuery, List<QLCEViewFilterWrapper> filters,
      String cloudProviderTableName, Integer limit, Integer offset, String accountId) {
    List<ViewRule> viewRuleList = new ArrayList<>();
    Optional<QLCEViewFilterWrapper> viewMetadataFilter = getViewMetadataFilter(filters);
    List<QLCEViewFilter> idFilters = getIdFilters(filters);

    List<QLCEViewRule> rules = getRuleFilters(filters);
    if (!rules.isEmpty()) {
      for (QLCEViewRule rule : rules) {
        viewRuleList.add(convertQLCEViewRuleToViewRule(rule));
      }
    }

    if (viewMetadataFilter.isPresent()) {
      QLCEViewMetadataFilter metadataFilter = viewMetadataFilter.get().getViewMetadataFilter();
      final String viewId = metadataFilter.getViewId();
      if (!metadataFilter.isPreview()) {
        CEView ceView = viewService.get(viewId);
        viewRuleList = ceView.getViewRules();
      }
    }

    // account id is not passed in current gen queries
    if (accountId != null) {
      cloudProviderTableName = getUpdatedCloudProviderTableName(filters, null, null, "", cloudProviderTableName);
    }

    ViewsQueryMetadata viewsQueryMetadata = viewsQueryBuilder.getFilterValuesQuery(
        viewRuleList, idFilters, getTimeFilters(filters), cloudProviderTableName, limit, offset);
    QueryJobConfiguration queryConfig =
        QueryJobConfiguration.newBuilder(viewsQueryMetadata.getQuery().toString()).build();
    TableResult result;
    try {
      result = bigQuery.query(queryConfig);
    } catch (InterruptedException e) {
      log.error("Failed to getViewFilterValueStats. {}", e);
      Thread.currentThread().interrupt();
      return null;
    }
    return convertToFilterValuesData(result, viewsQueryMetadata.getFields());
  }

  @Override
  public List<QLCEViewEntityStatsDataPoint> getEntityStatsDataPoints(BigQuery bigQuery,
      List<QLCEViewFilterWrapper> filters, List<QLCEViewGroupBy> groupBy, List<QLCEViewAggregation> aggregateFunction,
      List<QLCEViewSortCriteria> sort, String cloudProviderTableName, Integer limit, Integer offset) {
    // account id is not required for query builder of current-gen, therefore is passed null
    return getEntityStatsDataPointsNg(
        bigQuery, filters, groupBy, aggregateFunction, sort, cloudProviderTableName, limit, offset, null, true)
        .getData();
  }

  @Override
  public QLCEViewGridData getEntityStatsDataPointsNg(BigQuery bigQuery, List<QLCEViewFilterWrapper> filters,
      List<QLCEViewGroupBy> groupBy, List<QLCEViewAggregation> aggregateFunction, List<QLCEViewSortCriteria> sort,
      String cloudProviderTableName, Integer limit, Integer offset, String accountId, boolean getCostTrend) {
    boolean isClusterPerspective = isClusterPerspective(filters);
    Map<String, ViewCostData> costTrendData = new HashMap<>();
    long startTimeForTrendData = 0L;
    if (getCostTrend) {
      costTrendData = getEntityStatsDataForCostTrend(
          bigQuery, filters, groupBy, aggregateFunction, sort, cloudProviderTableName, limit, offset, accountId);
      startTimeForTrendData = getStartTimeForTrendFilters(filters);
      log.info("Cost trend data for view table : {} ", costTrendData);
    }
    SelectQuery query = getQuery(filters, groupBy, aggregateFunction, sort, cloudProviderTableName, false, accountId);
    query.addCustomization(new PgLimitClause(limit));
    query.addCustomization(new PgOffsetClause(offset));
    QueryJobConfiguration queryConfig = QueryJobConfiguration.newBuilder(query.toString()).build();
    TableResult result;
    try {
      result = bigQuery.query(queryConfig);
    } catch (InterruptedException e) {
      log.error("Failed to getEntityStatsDataPoints. {}", e);
      Thread.currentThread().interrupt();
      return null;
    }
    return convertToEntityStatsData(result, costTrendData, startTimeForTrendData, isClusterPerspective, getCostTrend);
  }

  @Override
  public TableResult getTimeSeriesStats(BigQuery bigQuery, List<QLCEViewFilterWrapper> filters,
      List<QLCEViewGroupBy> groupBy, List<QLCEViewAggregation> aggregateFunction, List<QLCEViewSortCriteria> sort,
      String cloudProviderTableName) {
    SelectQuery query = getQuery(filters, groupBy, aggregateFunction, sort, cloudProviderTableName, true);
    QueryJobConfiguration queryConfig = QueryJobConfiguration.newBuilder(query.toString()).build();
    try {
      return bigQuery.query(queryConfig);
    } catch (InterruptedException e) {
      log.error("Failed to getTimeSeriesStats. {}", e);
      Thread.currentThread().interrupt();
      return null;
    }
  }

  @Override
  public TableResult getTimeSeriesStatsNg(BigQuery bigQuery, List<QLCEViewFilterWrapper> filters,
      List<QLCEViewGroupBy> groupBy, List<QLCEViewAggregation> aggregateFunction, List<QLCEViewSortCriteria> sort,
      String cloudProviderTableName, String accountId, boolean includeOthers, Integer limit) {
    if (!includeOthers) {
      QLCEViewGridData gridData = getEntityStatsDataPointsNg(
          bigQuery, filters, groupBy, aggregateFunction, sort, cloudProviderTableName, limit, 0, accountId, false);
      filters = getModifiedFilters(filters, gridData);
    }
    SelectQuery query = getQuery(filters, groupBy, aggregateFunction, sort, cloudProviderTableName, true, accountId);
    QueryJobConfiguration queryConfig = QueryJobConfiguration.newBuilder(query.toString()).build();
    try {
      return bigQuery.query(queryConfig);
    } catch (InterruptedException e) {
      log.error("Failed to getTimeSeriesStats. {}", e);
      Thread.currentThread().interrupt();
      return null;
    }
  }

  @Override
  public QLCEViewTrendInfo getTrendStatsData(BigQuery bigQuery, List<QLCEViewFilterWrapper> filters,
      List<QLCEViewAggregation> aggregateFunction, String cloudProviderTableName) {
    return getTrendStatsDataNg(bigQuery, filters, aggregateFunction, cloudProviderTableName, null);
  }

  @Override
  public QLCEViewTrendInfo getTrendStatsDataNg(BigQuery bigQuery, List<QLCEViewFilterWrapper> filters,
      List<QLCEViewAggregation> aggregateFunction, String cloudProviderTableName, String accountId) {
    boolean isClusterTableQuery = isClusterTableQuery(filters, accountId);
    List<ViewRule> viewRuleList = new ArrayList<>();
    List<QLCEViewFilter> idFilters = getIdFilters(filters);
    List<QLCEViewTimeFilter> timeFilters = getTimeFilters(filters);
    SelectQuery query = getTrendStatsQuery(
        filters, idFilters, timeFilters, aggregateFunction, viewRuleList, cloudProviderTableName, accountId);

    List<QLCEViewTimeFilter> trendTimeFilters = getTrendFilters(timeFilters);
    SelectQuery prevTrendStatsQuery = getTrendStatsQuery(
        filters, idFilters, trendTimeFilters, aggregateFunction, viewRuleList, cloudProviderTableName, accountId);

    Instant trendStartInstant = Instant.ofEpochMilli(getTimeFilter(trendTimeFilters, AFTER).getValue().longValue());

    ViewCostData costData = getViewTrendStatsCostData(bigQuery, query, isClusterTableQuery);
    ViewCostData prevCostData = getViewTrendStatsCostData(bigQuery, prevTrendStatsQuery, isClusterTableQuery);

    EfficiencyScoreStats efficiencyScoreStats = null;
    if (isClusterTableQuery) {
      efficiencyScoreStats = viewsQueryHelper.getEfficiencyScoreStats(costData, prevCostData);
    }

    return getCostBillingStats(costData, prevCostData, timeFilters, trendStartInstant, efficiencyScoreStats);
  }

  @Override
  public QLCEViewTrendInfo getForecastCostData(BigQuery bigQuery, List<QLCEViewFilterWrapper> filters,
      List<QLCEViewAggregation> aggregateFunction, String cloudProviderTableName, String accountId) {
    Instant endInstantForForecastCost = viewsQueryHelper.getEndInstantForForecastCost(filters);
    ViewCostData currentCostData = getCostData(bigQuery, filters, aggregateFunction, cloudProviderTableName, accountId);
    Double forecastCost = getForecastCost(currentCostData, endInstantForForecastCost);

    return getForecastCostBillingStats(forecastCost, currentCostData.getCost(), getStartInstantForForecastCost(),
        endInstantForForecastCost.plus(1, ChronoUnit.SECONDS));
  }

  @Override
  public List<String> getColumnsForTable(BigQuery bigQuery, String informationSchemaView, String table) {
    SelectQuery query = viewsQueryBuilder.getInformationSchemaQueryForColumns(informationSchemaView, table);
    return getColumnsData(bigQuery, query);
  }

  @Override
  public boolean isClusterPerspective(List<QLCEViewFilterWrapper> filters) {
    Optional<QLCEViewFilterWrapper> viewMetadataFilter = getViewMetadataFilter(filters);
    if (viewMetadataFilter.isPresent()) {
      QLCEViewMetadataFilter metadataFilter = viewMetadataFilter.get().getViewMetadataFilter();
      final String viewId = metadataFilter.getViewId();
      if (!metadataFilter.isPreview()) {
        CEView ceView = viewService.get(viewId);
        List<ViewFieldIdentifier> dataSources;
        try {
          dataSources = ceView.getDataSources();
        } catch (Exception e) {
          dataSources = null;
        }
        return dataSources != null && dataSources.size() == 1 && dataSources.get(0).equals(CLUSTER);
      }
    }
    return false;
  }

  private boolean isClusterTableQuery(List<QLCEViewFilterWrapper> filters, String accountId) {
    return isClusterPerspective(filters) && accountId != null;
  }

  private List<String> getColumnsData(BigQuery bigQuery, SelectQuery query) {
    QueryJobConfiguration queryConfig = QueryJobConfiguration.newBuilder(query.toString()).build();
    TableResult result;
    try {
      result = bigQuery.query(queryConfig);
    } catch (InterruptedException e) {
      log.error("Failed to getTrendStatsData. {}", e);
      Thread.currentThread().interrupt();
      return null;
    }
    return convertToColumnList(result);
  }

  private List<String> convertToColumnList(TableResult result) {
    List<String> columns = new ArrayList<>();
    for (FieldValueList row : result.iterateAll()) {
      columns.add(row.get("column_name").getValue().toString());
    }
    return columns;
  }

  private ViewCostData getViewTrendStatsCostData(BigQuery bigQuery, SelectQuery query, boolean isClusterTableQuery) {
    QueryJobConfiguration queryConfig = QueryJobConfiguration.newBuilder(query.toString()).build();
    TableResult result;
    try {
      result = bigQuery.query(queryConfig);
    } catch (InterruptedException e) {
      log.error("Failed to getTrendStatsData. {}", e);
      Thread.currentThread().interrupt();
      return null;
    }
    return convertToTrendStatsData(result, isClusterTableQuery);
  }

  private ViewCostData convertToTrendStatsData(TableResult result, boolean isClusterTableQuery) {
    Schema schema = result.getSchema();
    FieldList fields = schema.getFields();
    ViewCostDataBuilder viewCostDataBuilder = ViewCostData.builder();
    for (FieldValueList row : result.iterateAll()) {
      for (Field field : fields) {
        switch (field.getName()) {
          case entityConstantMinStartTime:
            viewCostDataBuilder.minStartTime(getTimeStampValue(row, field, isClusterTableQuery));
            break;
          case entityConstantMaxStartTime:
            viewCostDataBuilder.maxStartTime(getTimeStampValue(row, field, isClusterTableQuery));
            break;
          case entityConstantCost:
          case entityConstantClusterCost:
            viewCostDataBuilder.cost(getNumericValue(row, field));
            break;
          case entityConstantIdleCost:
            viewCostDataBuilder.idleCost(getNumericValue(row, field));
            break;
          case entityConstantUnallocatedCost:
            viewCostDataBuilder.unallocatedCost(getNumericValue(row, field));
            break;
          default:
            break;
        }
      }
    }
    return viewCostDataBuilder.build();
  }

  protected QLCEViewTrendInfo getCostBillingStats(ViewCostData costData, ViewCostData prevCostData,
      List<QLCEViewTimeFilter> filters, Instant trendFilterStartTime, EfficiencyScoreStats efficiencyScoreStats) {
    Instant startInstant = Instant.ofEpochMilli(getTimeFilter(filters, AFTER).getValue().longValue());
    Instant endInstant = Instant.ofEpochMilli(costData.getMaxStartTime() / 1000);
    if (costData.getMaxStartTime() == 0) {
      endInstant =
          Instant.ofEpochMilli(getTimeFilter(filters, QLCEViewTimeFilterOperator.BEFORE).getValue().longValue());
    }

    boolean isYearRequired = viewsQueryHelper.isYearRequired(startInstant, endInstant);
    String startInstantFormat = viewsQueryHelper.getTotalCostFormattedDate(startInstant, isYearRequired);
    String endInstantFormat = viewsQueryHelper.getTotalCostFormattedDate(endInstant, isYearRequired);
    String totalCostDescription = format(COST_DESCRIPTION, startInstantFormat, endInstantFormat);
    String totalCostValue =
        format(COST_VALUE, viewsQueryHelper.formatNumber(viewsQueryHelper.getRoundedDoubleValue(costData.getCost())));

    double forecastCost = viewsQueryHelper.getForecastCost(ViewCostData.builder()
                                                               .cost(costData.getCost())
                                                               .minStartTime(costData.getMinStartTime() / 1000)
                                                               .maxStartTime(costData.getMaxStartTime() / 1000)
                                                               .build(),
        Instant.ofEpochMilli(getTimeFilter(filters, QLCEViewTimeFilterOperator.BEFORE).getValue().longValue()));

    return QLCEViewTrendInfo.builder()
        .statsLabel(TOTAL_COST_LABEL)
        .statsDescription(totalCostDescription)
        .statsValue(totalCostValue)
        .statsTrend(
            viewsQueryHelper.getBillingTrend(costData.getCost(), forecastCost, prevCostData, trendFilterStartTime))
        .value(costData.getCost())
        .efficiencyScoreStats(efficiencyScoreStats)
        .build();
  }

  protected QLCEViewTrendInfo getForecastCostBillingStats(
      Double forecastCost, Double totalCost, Instant startInstant, Instant endInstant) {
    String forecastCostDescription = "";
    String forecastCostValue = "";
    Double statsTrend = 0.0;

    if (forecastCost != null) {
      boolean isYearRequired = viewsQueryHelper.isYearRequired(startInstant, endInstant);
      String startInstantFormat = viewsQueryHelper.getTotalCostFormattedDate(startInstant, isYearRequired);
      String endInstantFormat = viewsQueryHelper.getTotalCostFormattedDate(endInstant, isYearRequired);
      forecastCostDescription = format(COST_DESCRIPTION, startInstantFormat, endInstantFormat);
      forecastCostValue =
          format(COST_VALUE, viewsQueryHelper.formatNumber(viewsQueryHelper.getRoundedDoubleValue(forecastCost)));
      statsTrend = viewsQueryHelper.getRoundedDoubleValue((forecastCost - totalCost / totalCost) * 100);
    } else {
      forecastCost = 0.0;
    }
    return QLCEViewTrendInfo.builder()
        .statsLabel(FORECAST_COST_LABEL)
        .statsTrend(statsTrend)
        .statsDescription(forecastCostDescription)
        .statsValue(forecastCostValue)
        .value(forecastCost)
        .build();
  }

  protected List<QLCEViewTimeFilter> getTrendFilters(List<QLCEViewTimeFilter> timeFilters) {
    Instant startInstant = Instant.ofEpochMilli(getTimeFilter(timeFilters, AFTER).getValue().longValue());
    Instant endInstant =
        Instant.ofEpochMilli(getTimeFilter(timeFilters, QLCEViewTimeFilterOperator.BEFORE).getValue().longValue());
    long diffMillis = Duration.between(startInstant, endInstant).toMillis();
    long trendEndTime = startInstant.toEpochMilli() - 1000;
    long trendStartTime = trendEndTime - diffMillis;

    List<QLCEViewTimeFilter> trendFilters = new ArrayList<>();
    trendFilters.add(getTrendBillingFilter(trendStartTime, AFTER));
    trendFilters.add(getTrendBillingFilter(trendEndTime, QLCEViewTimeFilterOperator.BEFORE));
    return trendFilters;
  }

  protected QLCEViewTimeFilter getTrendBillingFilter(Long filterTime, QLCEViewTimeFilterOperator operator) {
    return QLCEViewTimeFilter.builder()
        .field(QLCEViewFieldInput.builder()
                   .fieldId(ViewsMetaDataFields.START_TIME.getFieldName())
                   .fieldName(ViewsMetaDataFields.START_TIME.getFieldName())
                   .identifier(ViewFieldIdentifier.COMMON)
                   .identifierName(ViewFieldIdentifier.COMMON.getDisplayName())
                   .build())
        .operator(operator)
        .value(filterTime)
        .build();
  }

  protected QLCEViewTimeFilter getTimeFilter(
      List<QLCEViewTimeFilter> filters, QLCEViewTimeFilterOperator timeFilterOperator) {
    Optional<QLCEViewTimeFilter> timeFilter =
        filters.stream().filter(filter -> filter.getOperator() == timeFilterOperator).findFirst();
    if (timeFilter.isPresent()) {
      return timeFilter.get();
    } else {
      throw new InvalidRequestException("Time cannot be null");
    }
  }

  private SelectQuery getTrendStatsQuery(List<QLCEViewFilterWrapper> filters, List<QLCEViewFilter> idFilters,
      List<QLCEViewTimeFilter> timeFilters, List<QLCEViewAggregation> aggregateFunction, List<ViewRule> viewRuleList,
      String cloudProviderTableName, String accountId) {
    Optional<QLCEViewFilterWrapper> viewMetadataFilter = getViewMetadataFilter(filters);
    if (viewMetadataFilter.isPresent()) {
      final String viewId = viewMetadataFilter.get().getViewMetadataFilter().getViewId();
      CEView ceView = viewService.get(viewId);
      viewRuleList = ceView.getViewRules();
    }
    // account id is not passed in current gen queries
    if (accountId != null) {
      if (isClusterPerspective(filters)) {
        // Changes column name for cost to billingamount
        aggregateFunction = getModifiedAggregations(aggregateFunction);
      }
      cloudProviderTableName =
          getUpdatedCloudProviderTableName(filters, null, aggregateFunction, "", cloudProviderTableName);
    }
    return viewsQueryBuilder.getQuery(viewRuleList, idFilters, timeFilters, Collections.EMPTY_LIST, aggregateFunction,
        Collections.EMPTY_LIST, cloudProviderTableName);
  }

  // Current gen
  private SelectQuery getQuery(List<QLCEViewFilterWrapper> filters, List<QLCEViewGroupBy> groupBy,
      List<QLCEViewAggregation> aggregateFunction, List<QLCEViewSortCriteria> sort, String cloudProviderTableName,
      boolean isTimeTruncGroupByRequired) {
    return getQuery(
        filters, groupBy, aggregateFunction, sort, cloudProviderTableName, isTimeTruncGroupByRequired, null);
  }

  // Next-gen
  private SelectQuery getQuery(List<QLCEViewFilterWrapper> filters, List<QLCEViewGroupBy> groupBy,
      List<QLCEViewAggregation> aggregateFunction, List<QLCEViewSortCriteria> sort, String cloudProviderTableName,
      boolean isTimeTruncGroupByRequired, String accountId) {
    List<ViewRule> viewRuleList = new ArrayList<>();
    List<QLCEViewGroupBy> modifiedGroupBy = groupBy != null ? new ArrayList<>(groupBy) : new ArrayList<>();

    Optional<QLCEViewFilterWrapper> viewMetadataFilter = getViewMetadataFilter(filters);

    List<QLCEViewRule> rules = getRuleFilters(filters);
    if (!rules.isEmpty()) {
      for (QLCEViewRule rule : rules) {
        viewRuleList.add(convertQLCEViewRuleToViewRule(rule));
      }
    }

    if (viewMetadataFilter.isPresent()) {
      QLCEViewMetadataFilter metadataFilter = viewMetadataFilter.get().getViewMetadataFilter();
      final String viewId = metadataFilter.getViewId();
      if (!metadataFilter.isPreview()) {
        CEView ceView = viewService.get(viewId);
        viewRuleList = ceView.getViewRules();
        if (ceView.getViewVisualization() != null) {
          ViewVisualization viewVisualization = ceView.getViewVisualization();
          ViewField defaultGroupByField = viewVisualization.getGroupBy();
          ViewTimeGranularity defaultTimeGranularity = viewVisualization.getGranularity();
          modifiedGroupBy =
              getModifiedGroupBy(groupBy, defaultGroupByField, defaultTimeGranularity, isTimeTruncGroupByRequired);
        }
      }
    }
    List<QLCEViewFilter> idFilters = getIdFilters(filters);
    List<QLCEViewTimeFilter> timeFilters = getTimeFilters(filters);

    // account id is not passed in current gen queries
    if (accountId != null) {
      if (isClusterPerspective(filters)) {
        modifiedGroupBy = addAdditionalRequiredGroupBy(modifiedGroupBy);
        // Changes column name for cost to billingamount
        aggregateFunction = getModifiedAggregations(aggregateFunction);
        sort = getModifiedSort(sort);
      }
      cloudProviderTableName =
          getUpdatedCloudProviderTableName(filters, modifiedGroupBy, aggregateFunction, "", cloudProviderTableName);
    }

    return viewsQueryBuilder.getQuery(
        viewRuleList, idFilters, timeFilters, modifiedGroupBy, aggregateFunction, sort, cloudProviderTableName);
  }

  private ViewRule convertQLCEViewRuleToViewRule(QLCEViewRule rule) {
    List<ViewCondition> conditionsList = new ArrayList<>();
    for (QLCEViewFilter filter : rule.getConditions()) {
      conditionsList.add(ViewIdCondition.builder()
                             .values(Arrays.asList(filter.getValues()))
                             .viewField(getViewField(filter.getField()))
                             .viewOperator(mapQLCEViewFilterOperatorToViewIdOperator(filter.getOperator()))
                             .build());
    }
    return ViewRule.builder().viewConditions(conditionsList).build();
  }

  private ViewIdOperator mapQLCEViewFilterOperatorToViewIdOperator(QLCEViewFilterOperator operator) {
    if (operator.equals(QLCEViewFilterOperator.IN)) {
      return ViewIdOperator.IN;
    } else if (operator.equals(QLCEViewFilterOperator.NOT_IN)) {
      return ViewIdOperator.NOT_IN;
    } else if (operator.equals(QLCEViewFilterOperator.NOT_NULL)) {
      return ViewIdOperator.NOT_NULL;
    } else if (operator.equals(QLCEViewFilterOperator.NULL)) {
      return ViewIdOperator.NULL;
    }
    return null;
  }

  public ViewField getViewField(QLCEViewFieldInput field) {
    return ViewField.builder()
        .fieldId(field.getFieldId())
        .fieldName(field.getFieldName())
        .identifier(field.getIdentifier())
        .identifierName(field.getIdentifier().getDisplayName())
        .build();
  }

  private static Optional<QLCEViewFilterWrapper> getViewMetadataFilter(List<QLCEViewFilterWrapper> filters) {
    return filters.stream().filter(f -> f.getViewMetadataFilter() != null).findFirst();
  }

  private static List<QLCEViewTimeFilter> getTimeFilters(List<QLCEViewFilterWrapper> filters) {
    return filters.stream()
        .filter(f -> f.getTimeFilter() != null)
        .map(f -> f.getTimeFilter())
        .collect(Collectors.toList());
  }

  private static List<QLCEViewFilter> getIdFilters(List<QLCEViewFilterWrapper> filters) {
    return filters.stream().filter(f -> f.getIdFilter() != null).map(f -> f.getIdFilter()).collect(Collectors.toList());
  }

  private static List<QLCEViewRule> getRuleFilters(List<QLCEViewFilterWrapper> filters) {
    return filters.stream()
        .filter(f -> f.getRuleFilter() != null)
        .map(f -> f.getRuleFilter())
        .collect(Collectors.toList());
  }

  /*
   * This method is overriding the Group By passed by the UI with the defaults selected by user while creating the View
   * */
  private List<QLCEViewGroupBy> getModifiedGroupBy(List<QLCEViewGroupBy> groupByList, ViewField defaultGroupByField,
      ViewTimeGranularity defaultTimeGranularity, boolean isTimeTruncGroupByRequired) {
    if (groupByList == null) {
      return new ArrayList<>();
    }
    List<QLCEViewGroupBy> modifiedGroupBy = new ArrayList<>();
    Optional<QLCEViewGroupBy> timeTruncGroupBy =
        groupByList.stream().filter(g -> g.getTimeTruncGroupBy() != null).findFirst();

    List<QLCEViewGroupBy> entityGroupBy =
        groupByList.stream().filter(g -> g.getEntityGroupBy() != null).collect(Collectors.toList());

    if (timeTruncGroupBy.isPresent()) {
      modifiedGroupBy.add(timeTruncGroupBy.get());
    } else if (isTimeTruncGroupByRequired) {
      modifiedGroupBy.add(
          QLCEViewGroupBy.builder()
              .timeTruncGroupBy(QLCEViewTimeTruncGroupBy.builder()
                                    .resolution(viewsQueryBuilder.mapViewTimeGranularityToQLCEViewTimeGroupType(
                                        defaultTimeGranularity))
                                    .build())
              .build());
    }

    if (!entityGroupBy.isEmpty()) {
      modifiedGroupBy.addAll(entityGroupBy);
    } else {
      modifiedGroupBy.add(
          QLCEViewGroupBy.builder().entityGroupBy(viewsQueryBuilder.getViewFieldInput(defaultGroupByField)).build());
    }
    return modifiedGroupBy;
  }

  private QLCEViewGridData convertToEntityStatsData(TableResult result, Map<String, ViewCostData> costTrendData,
      long startTimeForTrend, boolean isClusterPerspective, boolean getCostTrend) {
    if (isClusterPerspective) {
      return convertToEntityStatsDataForCluster(result, costTrendData, startTimeForTrend, getCostTrend);
    }
    Schema schema = result.getSchema();
    FieldList fields = schema.getFields();
    List<String> fieldNames = getFieldNames(fields);

    List<QLCEViewEntityStatsDataPoint> entityStatsDataPoints = new ArrayList<>();
    for (FieldValueList row : result.iterateAll()) {
      QLCEViewEntityStatsDataPointBuilder dataPointBuilder = QLCEViewEntityStatsDataPoint.builder();
      Double cost = null;
      String name = "";
      String id = DEFAULT_STRING_VALUE;
      for (Field field : fields) {
        switch (field.getType().getStandardType()) {
          case STRING:
            name = fetchStringValue(row, field);
            id = getUpdatedId(id, name);
            dataPointBuilder.name(name);
            break;
          case FLOAT64:
            cost = getNumericValue(row, field);
            dataPointBuilder.cost(cost);
            break;
          default:
            break;
        }
      }
      dataPointBuilder.id(id);
      if (getCostTrend) {
        dataPointBuilder.costTrend(getCostTrendForEntity(cost, costTrendData.get(id), startTimeForTrend));
      }
      entityStatsDataPoints.add(dataPointBuilder.build());
    }
    return QLCEViewGridData.builder().data(entityStatsDataPoints).fields(fieldNames).build();
  }

  private QLCEViewGridData convertToEntityStatsDataForCluster(
      TableResult result, Map<String, ViewCostData> costTrendData, long startTimeForTrend, boolean getCostTrend) {
    Schema schema = result.getSchema();
    FieldList fields = schema.getFields();

    List<String> fieldNames = getFieldNames(fields);
    List<QLCEViewEntityStatsDataPoint> entityStatsDataPoints = new ArrayList<>();
    for (FieldValueList row : result.iterateAll()) {
      QLCEViewEntityStatsDataPointBuilder dataPointBuilder = QLCEViewEntityStatsDataPoint.builder();
      ClusterDataBuilder clusterDataBuilder = ClusterData.builder();
      Double cost = null;
      String name = DEFAULT_STRING_VALUE;
      String entityId = DEFAULT_STRING_VALUE;

      Map<String, java.lang.reflect.Field> builderFields = new HashMap<>();
      for (java.lang.reflect.Field builderField : clusterDataBuilder.getClass().getDeclaredFields()) {
        builderFields.put(builderField.getName().toLowerCase(), builderField);
      }

      for (Field field : fields) {
        java.lang.reflect.Field builderField = builderFields.get(field.getName().toLowerCase());
        try {
          if (builderField != null) {
            builderField.setAccessible(true);
            if (builderField.getType().equals(String.class)) {
              builderField.set(clusterDataBuilder, fetchStringValue(row, field));
            } else {
              builderField.set(clusterDataBuilder, getNumericValue(row, field));
            }
          } else {
            switch (field.getName().toLowerCase()) {
              case BILLING_AMOUNT:
                cost = getNumericValue(row, field);
                clusterDataBuilder.totalCost(cost);
                break;
              case ACTUAL_IDLE_COST:
                clusterDataBuilder.idleCost(getNumericValue(row, field));
                break;
              default:
                break;
            }
          }
        } catch (Exception e) {
          log.error("Exception in convertToEntityStatsDataForCluster: {}", e.toString());
        }

        if (field.getType().getStandardType() == StandardSQLTypeName.STRING) {
          name = fetchStringValue(row, field);
          entityId = getUpdatedId(entityId, name);
        }
      }
      clusterDataBuilder.id(entityId);
      clusterDataBuilder.name(name);
      ClusterData clusterData = clusterDataBuilder.build();
      // Calculating efficiency score
      if (cost > 0) {
        clusterDataBuilder.efficiencyScore(viewsQueryHelper.calculateEfficiencyScore(
            clusterData.getTotalCost(), clusterData.getIdleCost(), clusterData.getUnallocatedCost()));
      }
      dataPointBuilder.cost(cost);
      if (getCostTrend) {
        dataPointBuilder.costTrend(getCostTrendForEntity(cost, costTrendData.get(entityId), startTimeForTrend));
      }
      dataPointBuilder.clusterData(clusterDataBuilder.build());
      dataPointBuilder.isClusterPerspective(true);
      dataPointBuilder.id(entityId);
      dataPointBuilder.name(name);
      entityStatsDataPoints.add(dataPointBuilder.build());
    }
    return QLCEViewGridData.builder().data(entityStatsDataPoints).fields(fieldNames).build();
  }

  private List<String> getFieldNames(FieldList fields) {
    List<String> fieldNames = new ArrayList<>();
    for (Field field : fields) {
      if (field.getType().getStandardType() == StandardSQLTypeName.STRING) {
        fieldNames.add(field.getName());
      }
    }
    return fieldNames;
  }

  private String getUpdatedId(String id, String newField) {
    return id.equals(DEFAULT_STRING_VALUE) ? newField : id + ID_SEPARATOR + newField;
  }

  public List<String> convertToFilterValuesData(TableResult result, List<QLCEViewFieldInput> viewFieldList) {
    List<String> filterValues = new ArrayList<>();
    for (FieldValueList row : result.iterateAll()) {
      for (QLCEViewFieldInput field : viewFieldList) {
        final String filterStringValue = fetchStringValue(row, field);
        if (!filterStringValue.equals(nullStringValueConstant)) {
          filterValues.add(fetchStringValue(row, field));
        }
      }
    }
    return filterValues;
  }

  private long getTimeStampValue(FieldValueList row, Field field) {
    return getTimeStampValue(row, field, false);
  }

  private long getTimeStampValue(FieldValueList row, Field field, boolean isClusterPerspective) {
    FieldValue value = row.get(field.getName());
    if (!value.isNull()) {
      return isClusterPerspective ? value.getLongValue() * 1000 : value.getTimestampValue();
    }
    return 0;
  }

  private double getNumericValue(FieldValueList row, Field field) {
    FieldValue value = row.get(field.getName());
    if (!value.isNull()) {
      return Math.round(value.getNumericValue().doubleValue() * 100D) / 100D;
    }
    return 0;
  }

  private String fetchStringValue(FieldValueList row, Field field) {
    Object value = row.get(field.getName()).getValue();
    if (value != null) {
      return value.toString();
    }
    return nullStringValueConstant;
  }

  private String fetchStringValue(FieldValueList row, QLCEViewFieldInput field) {
    Object value = row.get(viewsQueryBuilder.getAliasFromField(field)).getValue();
    if (value != null) {
      return value.toString();
    }
    return nullStringValueConstant;
  }

  public List<QLCEViewTimeSeriesData> convertToQLViewTimeSeriesData(TableResult result) {
    Schema schema = result.getSchema();
    FieldList fields = schema.getFields();

    Map<Long, List<QLCEViewDataPoint>> timeSeriesDataPointsMap = new HashMap<>();
    for (FieldValueList row : result.iterateAll()) {
      QLCEViewDataPointBuilder billingDataPointBuilder = QLCEViewDataPoint.builder();
      Long startTimeTruncatedTimestamp = null;
      Double value = Double.valueOf(0);
      for (Field field : fields) {
        switch (field.getType().getStandardType()) {
          case TIMESTAMP:
            startTimeTruncatedTimestamp = row.get(field.getName()).getTimestampValue() / 1000;
            break;
          case STRING:
            String stringValue = fetchStringValue(row, field);
            billingDataPointBuilder.name(stringValue).id(stringValue);
            break;
          case FLOAT64:
            value += getNumericValue(row, field);
            break;
          default:
            break;
        }
      }

      billingDataPointBuilder.value(value);
      List<QLCEViewDataPoint> dataPoints = new ArrayList<>();
      if (timeSeriesDataPointsMap.containsKey(startTimeTruncatedTimestamp)) {
        dataPoints = timeSeriesDataPointsMap.get(startTimeTruncatedTimestamp);
      }
      dataPoints.add(billingDataPointBuilder.build());
      timeSeriesDataPointsMap.put(startTimeTruncatedTimestamp, dataPoints);
    }

    return convertTimeSeriesPointsMapToList(timeSeriesDataPointsMap);
  }

  public List<QLCEViewTimeSeriesData> convertTimeSeriesPointsMapToList(
      Map<Long, List<QLCEViewDataPoint>> timeSeriesDataPointsMap) {
    return timeSeriesDataPointsMap.entrySet()
        .stream()
        .map(e
            -> QLCEViewTimeSeriesData.builder()
                   .time(e.getKey())
                   .date(getFormattedDate(Instant.ofEpochMilli(e.getKey()), DATE_PATTERN_FOR_CHART))
                   .values(e.getValue())
                   .build())
        .sorted(Comparator.comparing(QLCEViewTimeSeriesData::getTime))
        .collect(Collectors.toList());
  }

  protected String getFormattedDate(Instant instant, String datePattern) {
    return instant.atZone(ZoneId.of("GMT")).format(DateTimeFormatter.ofPattern(datePattern));
  }

  private Map<String, ViewCostData> getEntityStatsDataForCostTrend(BigQuery bigQuery,
      List<QLCEViewFilterWrapper> filters, List<QLCEViewGroupBy> groupBy, List<QLCEViewAggregation> aggregateFunction,
      List<QLCEViewSortCriteria> sort, String cloudProviderTableName, Integer limit, Integer offset, String accountId) {
    boolean isClusterTableQuery = isClusterTableQuery(filters, accountId);
    SelectQuery query = getQuery(getFiltersForEntityStatsCostTrend(filters), groupBy,
        getAggregationsForEntityStatsCostTrend(aggregateFunction), sort, cloudProviderTableName, false, accountId);
    query.addCustomization(new PgLimitClause(limit));
    query.addCustomization(new PgOffsetClause(offset));
    QueryJobConfiguration queryConfig = QueryJobConfiguration.newBuilder(query.toString()).build();
    TableResult result;
    try {
      result = bigQuery.query(queryConfig);
    } catch (InterruptedException e) {
      log.error("Failed to getEntityStatsDataForCostTrend. {}", e);
      Thread.currentThread().interrupt();
      return null;
    }
    return convertToEntityStatsCostTrendData(result, isClusterTableQuery);
  }

  private Map<String, ViewCostData> convertToEntityStatsCostTrendData(TableResult result, boolean isClusterTableQuery) {
    Schema schema = result.getSchema();
    FieldList fields = schema.getFields();
    Map<String, ViewCostData> costTrendData = new HashMap<>();
    for (FieldValueList row : result.iterateAll()) {
      String name = "";
      String id = DEFAULT_STRING_VALUE;
      ViewCostDataBuilder viewCostDataBuilder = ViewCostData.builder();
      for (Field field : fields) {
        switch (field.getType().getStandardType()) {
          case STRING:
            name = fetchStringValue(row, field);
            id = getUpdatedId(id, name);
            break;
          case FLOAT64:
            if (field.getName().equalsIgnoreCase(BILLING_AMOUNT) || field.getName().equalsIgnoreCase(COST)) {
              viewCostDataBuilder.cost(getNumericValue(row, field));
            }
            break;
          default:
            switch (field.getName()) {
              case entityConstantMinStartTime:
                viewCostDataBuilder.minStartTime(getTimeStampValue(row, field, isClusterTableQuery));
                break;
              case entityConstantMaxStartTime:
                viewCostDataBuilder.maxStartTime(getTimeStampValue(row, field, isClusterTableQuery));
                break;
              default:
                break;
            }
            break;
        }
      }
      costTrendData.put(id, viewCostDataBuilder.build());
    }
    return costTrendData;
  }

  private List<QLCEViewFilterWrapper> getFiltersForEntityStatsCostTrend(List<QLCEViewFilterWrapper> filters) {
    List<QLCEViewFilterWrapper> trendFilters =
        filters.stream().filter(f -> f.getTimeFilter() == null).collect(Collectors.toList());
    List<QLCEViewTimeFilter> timeFilters = getTimeFilters(filters);
    List<QLCEViewTimeFilter> trendTimeFilters = getTrendFilters(timeFilters);
    trendTimeFilters.forEach(
        timeFilter -> trendFilters.add(QLCEViewFilterWrapper.builder().timeFilter(timeFilter).build()));

    return trendFilters;
  }

  private List<QLCEViewAggregation> getAggregationsForEntityStatsCostTrend(List<QLCEViewAggregation> aggregations) {
    List<QLCEViewAggregation> trendAggregations = new ArrayList<>(aggregations);
    trendAggregations.add(QLCEViewAggregation.builder().operationType(MAX).columnName("startTime").build());
    trendAggregations.add(QLCEViewAggregation.builder().operationType(MIN).columnName("startTime").build());
    return trendAggregations;
  }

  private long getStartTimeForTrendFilters(List<QLCEViewFilterWrapper> filters) {
    List<QLCEViewTimeFilter> timeFilters = getTimeFilters(filters);
    List<QLCEViewTimeFilter> trendTimeFilters = getTrendFilters(timeFilters);

    Optional<QLCEViewTimeFilter> startTimeFilter =
        trendTimeFilters.stream().filter(filter -> filter.getOperator() == AFTER).findFirst();
    if (startTimeFilter.isPresent()) {
      return startTimeFilter.get().getValue().longValue();
    } else {
      throw new InvalidRequestException("Start time cannot be null");
    }
  }

  private double getCostTrendForEntity(Double currentCost, ViewCostData prevCostData, long startTimeForTrend) {
    if (prevCostData != null && prevCostData.getCost() != 0 && currentCost != null) {
      Double prevCost = prevCostData.getCost();
      long startTimeForPrevCost = prevCostData.getMinStartTime() / 1000;
      double costDifference = currentCost - prevCost;
      if (startTimeForTrend + ONE_DAY_MILLIS > startTimeForPrevCost) {
        return Math.round((costDifference * 100 / prevCost) * 100D) / 100D;
      }
    }
    return 0;
  }

  private List<QLCEViewGroupBy> addAdditionalRequiredGroupBy(List<QLCEViewGroupBy> groupByList) {
    List<QLCEViewGroupBy> modifiedGroupBy = new ArrayList<>();
    groupByList.forEach(groupBy -> {
      if (groupBy.getEntityGroupBy() != null) {
        switch (groupBy.getEntityGroupBy().getFieldName()) {
          case GROUP_BY_WORKLOAD_ID:
            modifiedGroupBy.add(getGroupBy(GROUP_BY_CLUSTER_NAME, CLUSTER_NAME, CLUSTER));
            modifiedGroupBy.add(getGroupBy(GROUP_BY_NAMESPACE, NAMESPACE, CLUSTER));
            modifiedGroupBy.add(getGroupBy(GROUP_BY_WORKLOAD_TYPE, WORKLOAD_TYPE, CLUSTER));
            modifiedGroupBy.add(getGroupBy(GROUP_BY_WORKLOAD_NAME, WORKLOAD_NAME, CLUSTER));
            break;
          case GROUP_BY_NAMESPACE_ID:
            modifiedGroupBy.add(getGroupBy(GROUP_BY_CLUSTER_NAME, CLUSTER_NAME, CLUSTER));
            modifiedGroupBy.add(getGroupBy(GROUP_BY_NAMESPACE, NAMESPACE, CLUSTER));
            break;
          case GROUP_BY_ECS_SERVICE_ID:
            modifiedGroupBy.add(getGroupBy(GROUP_BY_CLUSTER_NAME, CLUSTER_NAME, CLUSTER));
            modifiedGroupBy.add(getGroupBy(GROUP_BY_ECS_LAUNCH_TYPE, LAUNCH_TYPE, CLUSTER));
            modifiedGroupBy.add(getGroupBy(GROUP_BY_ECS_SERVICE, CLOUD_SERVICE_NAME, CLUSTER));
            break;
          case GROUP_BY_ECS_LAUNCH_TYPE_ID:
            modifiedGroupBy.add(getGroupBy(GROUP_BY_CLUSTER_NAME, CLUSTER_NAME, CLUSTER));
            modifiedGroupBy.add(getGroupBy(GROUP_BY_ECS_LAUNCH_TYPE, LAUNCH_TYPE, CLUSTER));
            break;
          case GROUP_BY_ECS_TASK_ID:
            modifiedGroupBy.add(getGroupBy(GROUP_BY_CLUSTER_NAME, CLUSTER_NAME, CLUSTER));
            modifiedGroupBy.add(getGroupBy(GROUP_BY_ECS_LAUNCH_TYPE, LAUNCH_TYPE, CLUSTER));
            modifiedGroupBy.add(getGroupBy(GROUP_BY_ECS_SERVICE, CLOUD_SERVICE_NAME, CLUSTER));
            modifiedGroupBy.add(getGroupBy(GROUP_BY_ECS_TASK, TASK_ID, CLUSTER));
            break;
          case GROUP_BY_PRODUCT:
            modifiedGroupBy.add(getGroupBy(GROUP_BY_CLUSTER_NAME, CLUSTER_NAME, CLUSTER));
            break;
          default:
            modifiedGroupBy.add(groupBy);
        }
      } else {
        modifiedGroupBy.add(groupBy);
      }
    });

    return modifiedGroupBy;
  }

  private QLCEViewGroupBy getGroupBy(String fieldName, String fieldId, ViewFieldIdentifier identifier) {
    return QLCEViewGroupBy.builder()
        .entityGroupBy(
            QLCEViewFieldInput.builder().fieldId(fieldId).fieldName(fieldName).identifier(identifier).build())
        .build();
  }

  // Methods for determining table
  public String getUpdatedCloudProviderTableName(List<QLCEViewFilterWrapper> filters, List<QLCEViewGroupBy> groupBy,
      List<QLCEViewAggregation> aggregateFunction, String accountId, String cloudProviderTableName) {
    if (!isClusterPerspective(filters)) {
      return cloudProviderTableName;
    }
    String[] tableNameSplit = cloudProviderTableName.split("\\.");
    String tableName;
    List<QLCEViewFieldInput> entityGroupBy = new ArrayList<>();
    if (groupBy != null) {
      entityGroupBy = groupBy.stream()
                          .filter(entry -> entry.getEntityGroupBy() != null)
                          .map(QLCEViewGroupBy::getEntityGroupBy)
                          .collect(Collectors.toList());
    }
    if (aggregateFunction == null) {
      aggregateFunction = new ArrayList<>();
    }

    if (featureFlagService.isEnabled(CE_BILLING_DATA_PRE_AGGREGATION, accountId)
        && areAggregationsValidForPreAggregation(aggregateFunction) && isValidGroupByForPreAggregation(entityGroupBy)) {
      tableName = isGroupByHour(groupBy) || shouldUseHourlyData(getTimeFilters(filters))
          ? CLUSTER_TABLE_HOURLY_AGGREGRATED
          : CLUSTER_TABLE_AGGREGRATED;
    } else {
      tableName =
          isGroupByHour(groupBy) || shouldUseHourlyData(getTimeFilters(filters)) ? CLUSTER_TABLE_HOURLY : CLUSTER_TABLE;
    }
    return format("%s.%s.%s", tableNameSplit[0], tableNameSplit[1], tableName);
  }

  private boolean isGroupByHour(List<QLCEViewGroupBy> groupBy) {
    QLCEViewTimeTruncGroupBy groupByTime = viewsQueryBuilder.getGroupByTime(groupBy);
    return groupByTime != null && groupByTime.getResolution() != null
        && groupByTime.getResolution() == QLCEViewTimeGroupType.HOUR;
  }

  private boolean shouldUseHourlyData(List<QLCEViewTimeFilter> timeFilters) {
    if (!timeFilters.isEmpty()) {
      QLCEViewTimeFilter startTimeFilter =
          timeFilters.stream().filter(timeFilter -> timeFilter.getOperator().equals(AFTER)).findFirst().orElse(null);

      if (startTimeFilter != null) {
        long startTime = startTimeFilter.getValue().longValue();
        ZoneId zoneId = ZoneId.of(STANDARD_TIME_ZONE);
        LocalDate today = LocalDate.now(zoneId);
        ZonedDateTime zdtStart = today.atStartOfDay(zoneId);
        long cutoffTime = zdtStart.toEpochSecond() * 1000 - 7 * ONE_DAY_MILLIS;
        return startTime >= cutoffTime;
      }
    }
    return false;
  }

  private boolean areAggregationsValidForPreAggregation(List<QLCEViewAggregation> aggregateFunctions) {
    if (aggregateFunctions.isEmpty()) {
      return true;
    }
    return !aggregateFunctions.stream().anyMatch(aggregationFunction
        -> aggregationFunction.getColumnName().equalsIgnoreCase(EFFECTIVE_CPU_LIMIT)
            || aggregationFunction.getColumnName().equalsIgnoreCase(EFFECTIVE_CPU_REQUEST)
            || aggregationFunction.getColumnName().equalsIgnoreCase(EFFECTIVE_MEMORY_LIMIT)
            || aggregationFunction.getColumnName().equalsIgnoreCase(EFFECTIVE_MEMORY_REQUEST)
            || aggregationFunction.getColumnName().equalsIgnoreCase(EFFECTIVE_CPU_UTILIZATION_VALUE)
            || aggregationFunction.getColumnName().equalsIgnoreCase(EFFECTIVE_MEMORY_UTILIZATION_VALUE));
  }

  // Check for pod/pv/cloudservicename/taskid/launchtype
  private boolean isValidGroupByForPreAggregation(List<QLCEViewFieldInput> groupByList) {
    if (groupByList.isEmpty()) {
      return true;
    }
    return !groupByList.stream().anyMatch(groupBy
        -> groupBy.getFieldId().equals(CLOUD_SERVICE_NAME) || groupBy.getFieldId().equals(TASK_ID)
            || groupBy.getFieldId().equals(LAUNCH_TYPE));
  }

  private boolean areFiltersValidForPreAggregation(List<QLCEViewFilter> filters) {
    if (filters.isEmpty()) {
      return true;
    }
    return !filters.stream().anyMatch(filter
        -> filter.getField().getFieldId().equalsIgnoreCase(INSTANCE_NAME)
            || filter.getField().getFieldId().equalsIgnoreCase(TASK_ID)
            || filter.getField().getFieldId().equalsIgnoreCase(LAUNCH_TYPE)
            || filter.getField().getFieldId().equalsIgnoreCase(CLOUD_SERVICE_NAME)
            || filter.getField().getFieldId().equalsIgnoreCase(PARENT_INSTANCE_ID));
  }

  private List<QLCEViewFilterWrapper> getModifiedFilters(
      List<QLCEViewFilterWrapper> filters, QLCEViewGridData gridData) {
    if (filters == null) {
      filters = new ArrayList<>();
    }

    List<String> fields = gridData.getFields();
    Map<String, java.lang.reflect.Field> clusterDataFields = new HashMap<>();
    for (java.lang.reflect.Field clusterField : ClusterData.class.getDeclaredFields()) {
      clusterDataFields.put(clusterField.getName().toLowerCase(), clusterField);
    }

    for (String field : fields) {
      Set<String> values = new HashSet<>();
      try {
        for (QLCEViewEntityStatsDataPoint dataPoint : gridData.getData()) {
          ClusterData clusterData = dataPoint.getClusterData();
          java.lang.reflect.Field clusterField = clusterDataFields.get(field.toLowerCase());
          clusterField.setAccessible(true);
          values.add((String) clusterField.get(clusterData));
        }
      } catch (Exception e) {
        log.error("Unable to fetch field values for filter: {}", e.toString());
      }

      if (!values.isEmpty()) {
        filters.add(QLCEViewFilterWrapper.builder()
                        .idFilter(QLCEViewFilter.builder()
                                      .field(QLCEViewFieldInput.builder()
                                                 .fieldId(field.toLowerCase())
                                                 .fieldName(field.toLowerCase())
                                                 .identifier(CLUSTER)
                                                 .build())
                                      .operator(QLCEViewFilterOperator.IN)
                                      .values(values.toArray(new String[0]))
                                      .build())
                        .build());
      }
    }
    return filters;
  }

  private List<QLCEViewAggregation> getModifiedAggregations(List<QLCEViewAggregation> aggregateFunctions) {
    List<QLCEViewAggregation> modifiedAggregations;
    if (aggregateFunctions == null) {
      return new ArrayList<>();
    }
    boolean isCostAggregationPresent =
        aggregateFunctions.stream().anyMatch(function -> function.getColumnName().equals(COST));
    if (isCostAggregationPresent) {
      modifiedAggregations = aggregateFunctions.stream()
                                 .filter(function -> !function.getColumnName().equals(COST))
                                 .collect(Collectors.toList());
      modifiedAggregations.add(QLCEViewAggregation.builder()
                                   .columnName(BILLING_AMOUNT)
                                   .operationType(QLCEViewAggregateOperation.SUM)
                                   .build());
    } else {
      modifiedAggregations = aggregateFunctions;
    }
    return modifiedAggregations;
  }

  private List<QLCEViewSortCriteria> getModifiedSort(List<QLCEViewSortCriteria> sortCriteria) {
    List<QLCEViewSortCriteria> modifiedSortCriteria;
    if (sortCriteria == null) {
      return new ArrayList<>();
    }
    boolean isSortByCostPresent = sortCriteria.stream().anyMatch(sort -> sort.getSortType() == QLCEViewSortType.COST);
    if (isSortByCostPresent) {
      QLCEViewSortCriteria costSort =
          sortCriteria.stream().filter(sort -> sort.getSortType() == QLCEViewSortType.COST).findFirst().get();
      modifiedSortCriteria = sortCriteria.stream()
                                 .filter(sort -> sort.getSortType() != QLCEViewSortType.COST)
                                 .collect(Collectors.toList());
      modifiedSortCriteria.add(QLCEViewSortCriteria.builder()
                                   .sortOrder(costSort.getSortOrder())
                                   .sortType(QLCEViewSortType.CLUSTER_COST)
                                   .build());
    } else {
      modifiedSortCriteria = sortCriteria;
    }
    return modifiedSortCriteria;
  }

  // Methods for calculating forecast cost
  public Double getForecastCost(ViewCostData costData, Instant endInstant) {
    if (costData == null) {
      return null;
    }
    Instant currentTime = Instant.now();
    if (currentTime.isAfter(endInstant)) {
      return null;
    }

    double totalCost = costData.getCost();
    long actualTimeDiffMillis = (endInstant.plus(1, ChronoUnit.SECONDS).toEpochMilli()) - costData.getMaxStartTime();

    long billingTimeDiffMillis = ONE_DAY_MILLIS;
    if (costData.getMaxStartTime() != costData.getMinStartTime()) {
      billingTimeDiffMillis = costData.getMaxStartTime() - costData.getMinStartTime() + ONE_DAY_MILLIS;
    }
    if (billingTimeDiffMillis < OBSERVATION_PERIOD) {
      return null;
    }

    return totalCost
        * (new BigDecimal(actualTimeDiffMillis).divide(new BigDecimal(billingTimeDiffMillis), 2, RoundingMode.HALF_UP))
              .doubleValue();
  }

  private ViewCostData getCostData(BigQuery bigQuery, List<QLCEViewFilterWrapper> filters,
      List<QLCEViewAggregation> aggregateFunction, String cloudProviderTableName, String accountId) {
    boolean isClusterTableQuery = isClusterTableQuery(filters, accountId);
    List<ViewRule> viewRuleList = new ArrayList<>();
    List<QLCEViewFilter> idFilters = getIdFilters(filters);
    List<QLCEViewTimeFilter> timeFilters = getTimeFilters(filters);
    SelectQuery query = getTrendStatsQuery(
        filters, idFilters, timeFilters, aggregateFunction, viewRuleList, cloudProviderTableName, accountId);
    return getViewTrendStatsCostData(bigQuery, query, isClusterTableQuery);
  }

  private Instant getStartInstantForForecastCost() {
    return Instant.ofEpochMilli(viewsQueryHelper.getStartOfCurrentDay());
  }
}
