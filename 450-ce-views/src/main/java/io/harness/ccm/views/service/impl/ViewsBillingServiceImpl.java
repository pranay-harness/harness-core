package io.harness.ccm.views.service.impl;

import static io.harness.ccm.views.graphql.ViewMetaDataConstants.entityConstantCost;
import static io.harness.ccm.views.graphql.ViewMetaDataConstants.entityConstantMaxStartTime;
import static io.harness.ccm.views.graphql.ViewMetaDataConstants.entityConstantMinStartTime;

import io.harness.ccm.views.entities.CEView;
import io.harness.ccm.views.entities.ViewCondition;
import io.harness.ccm.views.entities.ViewField;
import io.harness.ccm.views.entities.ViewFieldIdentifier;
import io.harness.ccm.views.entities.ViewIdCondition;
import io.harness.ccm.views.entities.ViewIdOperator;
import io.harness.ccm.views.entities.ViewRule;
import io.harness.ccm.views.entities.ViewTimeGranularity;
import io.harness.ccm.views.entities.ViewVisualization;
import io.harness.ccm.views.graphql.QLCEViewAggregation;
import io.harness.ccm.views.graphql.QLCEViewDataPoint;
import io.harness.ccm.views.graphql.QLCEViewDataPoint.QLCEViewDataPointBuilder;
import io.harness.ccm.views.graphql.QLCEViewEntityStatsDataPoint;
import io.harness.ccm.views.graphql.QLCEViewEntityStatsDataPoint.QLCEViewEntityStatsDataPointBuilder;
import io.harness.ccm.views.graphql.QLCEViewFieldInput;
import io.harness.ccm.views.graphql.QLCEViewFilter;
import io.harness.ccm.views.graphql.QLCEViewFilterOperator;
import io.harness.ccm.views.graphql.QLCEViewFilterWrapper;
import io.harness.ccm.views.graphql.QLCEViewGroupBy;
import io.harness.ccm.views.graphql.QLCEViewMetadataFilter;
import io.harness.ccm.views.graphql.QLCEViewRule;
import io.harness.ccm.views.graphql.QLCEViewSortCriteria;
import io.harness.ccm.views.graphql.QLCEViewTimeFilter;
import io.harness.ccm.views.graphql.QLCEViewTimeFilterOperator;
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

import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.Field;
import com.google.cloud.bigquery.FieldList;
import com.google.cloud.bigquery.FieldValue;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.Schema;
import com.google.cloud.bigquery.TableResult;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.healthmarketscience.sqlbuilder.SelectQuery;
import com.healthmarketscience.sqlbuilder.custom.postgresql.PgLimitClause;
import com.healthmarketscience.sqlbuilder.custom.postgresql.PgOffsetClause;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class ViewsBillingServiceImpl implements ViewsBillingService {
  @Inject ViewsQueryBuilder viewsQueryBuilder;
  @Inject CEViewService viewService;
  @Inject ViewsQueryHelper viewsQueryHelper;

  public static final String nullStringValueConstant = "Others";
  private static final String COST_DESCRIPTION = "of %s - %s";
  private static final String COST_VALUE = "$%s";
  private static final String TOTAL_COST_LABEL = "Total Cost";
  private static final String DATE_PATTERN_FOR_CHART = "MMM dd";

  @Override
  public List<String> getFilterValueStats(BigQuery bigQuery, List<QLCEViewFilterWrapper> filters,
      String cloudProviderTableName, Integer limit, Integer offset) {
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

    ViewsQueryMetadata viewsQueryMetadata =
        viewsQueryBuilder.getFilterValuesQuery(viewRuleList, idFilters, cloudProviderTableName, limit, offset);
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
    SelectQuery query = getQuery(filters, groupBy, aggregateFunction, sort, cloudProviderTableName, false);
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
    return convertToEntityStatsData(result);
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
  public QLCEViewTrendInfo getTrendStatsData(BigQuery bigQuery, List<QLCEViewFilterWrapper> filters,
      List<QLCEViewAggregation> aggregateFunction, String cloudProviderTableName) {
    List<ViewRule> viewRuleList = new ArrayList<>();
    List<QLCEViewFilter> idFilters = getIdFilters(filters);
    List<QLCEViewTimeFilter> timeFilters = getTimeFilters(filters);
    SelectQuery query =
        getTrendStatsQuery(filters, idFilters, timeFilters, aggregateFunction, viewRuleList, cloudProviderTableName);

    List<QLCEViewTimeFilter> trendTimeFilters = getTrendFilters(timeFilters);
    SelectQuery prevTrendStatsQuery = getTrendStatsQuery(
        filters, idFilters, trendTimeFilters, aggregateFunction, viewRuleList, cloudProviderTableName);

    Instant trendStartInstant =
        Instant.ofEpochMilli(getTimeFilter(trendTimeFilters, QLCEViewTimeFilterOperator.AFTER).getValue().longValue());

    ViewCostData costData = getViewTrendStatsCostData(bigQuery, query);
    ViewCostData prevCostData = getViewTrendStatsCostData(bigQuery, prevTrendStatsQuery);

    return getCostBillingStats(costData, prevCostData, timeFilters, trendStartInstant);
  }

  private ViewCostData getViewTrendStatsCostData(BigQuery bigQuery, SelectQuery query) {
    QueryJobConfiguration queryConfig = QueryJobConfiguration.newBuilder(query.toString()).build();
    TableResult result;
    try {
      result = bigQuery.query(queryConfig);
    } catch (InterruptedException e) {
      log.error("Failed to getTrendStatsData. {}", e);
      Thread.currentThread().interrupt();
      return null;
    }
    return convertToTrendStatsData(result);
  }

  private ViewCostData convertToTrendStatsData(TableResult result) {
    Schema schema = result.getSchema();
    FieldList fields = schema.getFields();
    ViewCostDataBuilder viewCostDataBuilder = ViewCostData.builder();

    for (FieldValueList row : result.iterateAll()) {
      for (Field field : fields) {
        switch (field.getName()) {
          case entityConstantMinStartTime:
            viewCostDataBuilder.minStartTime(getTimeStampValue(row, field));
            break;
          case entityConstantMaxStartTime:
            viewCostDataBuilder.maxStartTime(getTimeStampValue(row, field));
            break;
          case entityConstantCost:
            viewCostDataBuilder.cost(getNumericValue(row, field));
            break;
          default:
            break;
        }
      }
    }
    return viewCostDataBuilder.build();
  }

  protected QLCEViewTrendInfo getCostBillingStats(ViewCostData costData, ViewCostData prevCostData,
      List<QLCEViewTimeFilter> filters, Instant trendFilterStartTime) {
    Instant startInstant =
        Instant.ofEpochMilli(getTimeFilter(filters, QLCEViewTimeFilterOperator.AFTER).getValue().longValue());
    Instant endInstant = Instant.ofEpochMilli(costData.getMaxStartTime() / 1000);
    if (costData.getMaxStartTime() == 0) {
      endInstant =
          Instant.ofEpochMilli(getTimeFilter(filters, QLCEViewTimeFilterOperator.BEFORE).getValue().longValue());
    }

    boolean isYearRequired = viewsQueryHelper.isYearRequired(startInstant, endInstant);
    String startInstantFormat = viewsQueryHelper.getTotalCostFormattedDate(startInstant, isYearRequired);
    String endInstantFormat = viewsQueryHelper.getTotalCostFormattedDate(endInstant, isYearRequired);
    String totalCostDescription = String.format(COST_DESCRIPTION, startInstantFormat, endInstantFormat);
    String totalCostValue = String.format(
        COST_VALUE, viewsQueryHelper.formatNumber(viewsQueryHelper.getRoundedDoubleValue(costData.getCost())));

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
        .build();
  }

  protected List<QLCEViewTimeFilter> getTrendFilters(List<QLCEViewTimeFilter> timeFilters) {
    Instant startInstant =
        Instant.ofEpochMilli(getTimeFilter(timeFilters, QLCEViewTimeFilterOperator.AFTER).getValue().longValue());
    Instant endInstant =
        Instant.ofEpochMilli(getTimeFilter(timeFilters, QLCEViewTimeFilterOperator.BEFORE).getValue().longValue());
    long diffMillis = Duration.between(startInstant, endInstant).toMillis();
    long trendEndTime = startInstant.toEpochMilli() - 1000;
    long trendStartTime = trendEndTime - diffMillis;

    List<QLCEViewTimeFilter> trendFilters = new ArrayList<>();
    trendFilters.add(getTrendBillingFilter(trendStartTime, QLCEViewTimeFilterOperator.AFTER));
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
      String cloudProviderTableName) {
    Optional<QLCEViewFilterWrapper> viewMetadataFilter = getViewMetadataFilter(filters);
    if (viewMetadataFilter.isPresent()) {
      final String viewId = viewMetadataFilter.get().getViewMetadataFilter().getViewId();
      CEView ceView = viewService.get(viewId);
      viewRuleList = ceView.getViewRules();
    }
    return viewsQueryBuilder.getQuery(viewRuleList, idFilters, timeFilters, Collections.EMPTY_LIST, aggregateFunction,
        Collections.EMPTY_LIST, cloudProviderTableName);
  }

  private SelectQuery getQuery(List<QLCEViewFilterWrapper> filters, List<QLCEViewGroupBy> groupBy,
      List<QLCEViewAggregation> aggregateFunction, List<QLCEViewSortCriteria> sort, String cloudProviderTableName,
      boolean isTimeTruncGroupByRequired) {
    List<ViewRule> viewRuleList = new ArrayList<>();
    List<QLCEViewGroupBy> modifiedGroupBy = new ArrayList<>(groupBy);

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
    List<QLCEViewGroupBy> modifiedGroupBy = new ArrayList<>();
    Optional<QLCEViewGroupBy> timeTruncGroupBy =
        groupByList.stream().filter(g -> g.getTimeTruncGroupBy() != null).findFirst();

    Optional<QLCEViewGroupBy> entityGroupBy =
        groupByList.stream().filter(g -> g.getEntityGroupBy() != null).findFirst();

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

    if (entityGroupBy.isPresent()) {
      modifiedGroupBy.add(entityGroupBy.get());
    } else {
      modifiedGroupBy.add(
          QLCEViewGroupBy.builder().entityGroupBy(viewsQueryBuilder.getViewFieldInput(defaultGroupByField)).build());
    }
    return modifiedGroupBy;
  }

  private List<QLCEViewEntityStatsDataPoint> convertToEntityStatsData(TableResult result) {
    Schema schema = result.getSchema();
    FieldList fields = schema.getFields();

    List<QLCEViewEntityStatsDataPoint> entityStatsDataPoints = new ArrayList<>();
    for (FieldValueList row : result.iterateAll()) {
      QLCEViewEntityStatsDataPointBuilder dataPointBuilder = QLCEViewEntityStatsDataPoint.builder();
      for (Field field : fields) {
        switch (field.getType().getStandardType()) {
          case STRING:
            dataPointBuilder.name(fetchStringValue(row, field));
            break;
          case FLOAT64:
            dataPointBuilder.cost(getNumericValue(row, field));
            break;
          default:
            break;
        }
      }

      dataPointBuilder.costTrend(0);
      entityStatsDataPoints.add(dataPointBuilder.build());
    }
    return entityStatsDataPoints;
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
    FieldValue value = row.get(field.getName());
    if (!value.isNull()) {
      return value.getTimestampValue();
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
}
