package io.harness.ccm.views.service.impl;

import static io.harness.ccm.views.graphql.ViewMetaDataConstants.entityConstantCost;
import static io.harness.ccm.views.graphql.ViewMetaDataConstants.entityConstantMaxStartTime;
import static io.harness.ccm.views.graphql.ViewMetaDataConstants.entityConstantMinStartTime;

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
import io.harness.ccm.views.entities.CEView;
import io.harness.ccm.views.entities.ViewField;
import io.harness.ccm.views.entities.ViewFieldIdentifier;
import io.harness.ccm.views.entities.ViewRule;
import io.harness.ccm.views.entities.ViewTimeGranularity;
import io.harness.ccm.views.entities.ViewVisualization;
import io.harness.ccm.views.graphql.QLCEViewAggregation;
import io.harness.ccm.views.graphql.QLCEViewEntityStatsDataPoint;
import io.harness.ccm.views.graphql.QLCEViewEntityStatsDataPoint.QLCEViewEntityStatsDataPointBuilder;
import io.harness.ccm.views.graphql.QLCEViewFieldInput;
import io.harness.ccm.views.graphql.QLCEViewFilter;
import io.harness.ccm.views.graphql.QLCEViewFilterWrapper;
import io.harness.ccm.views.graphql.QLCEViewGroupBy;
import io.harness.ccm.views.graphql.QLCEViewMetadataFilter;
import io.harness.ccm.views.graphql.QLCEViewSortCriteria;
import io.harness.ccm.views.graphql.QLCEViewTimeFilter;
import io.harness.ccm.views.graphql.QLCEViewTimeFilterOperator;
import io.harness.ccm.views.graphql.QLCEViewTimeTruncGroupBy;
import io.harness.ccm.views.graphql.QLCEViewTrendInfo;
import io.harness.ccm.views.graphql.ViewCostData;
import io.harness.ccm.views.graphql.ViewCostData.ViewCostDataBuilder;
import io.harness.ccm.views.graphql.ViewsMetaDataFields;
import io.harness.ccm.views.graphql.ViewsQueryBuilder;
import io.harness.ccm.views.graphql.ViewsQueryHelper;
import io.harness.ccm.views.graphql.ViewsQueryMetadata;
import io.harness.ccm.views.service.CEViewService;
import io.harness.ccm.views.service.ViewCustomFieldService;
import io.harness.ccm.views.service.ViewsBillingService;
import io.harness.exception.InvalidRequestException;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Singleton
public class ViewsBillingServiceImpl implements ViewsBillingService {
  @Inject ViewsQueryBuilder viewsQueryBuilder;
  @Inject CEViewService viewService;
  @Inject ViewCustomFieldService customFieldService;
  @Inject ViewsQueryHelper viewsQueryHelper;

  public static final String nullStringValueConstant = "Others";
  private static final String COST_DESCRIPTION = "of %s - %s";
  private static final String COST_VALUE = "$%s";
  private static final String TOTAL_COST_LABEL = "Total Cost";

  @Override
  public List<String> getFilterValueStats(
      BigQuery bigQuery, List<QLCEViewFilter> filters, String cloudProviderTableName, Integer limit, Integer offset) {
    ViewsQueryMetadata viewsQueryMetadata =
        viewsQueryBuilder.getFilterValuesQuery(filters, cloudProviderTableName, limit, offset);
    QueryJobConfiguration queryConfig =
        QueryJobConfiguration.newBuilder(viewsQueryMetadata.getQuery().toString()).build();
    TableResult result;
    try {
      result = bigQuery.query(queryConfig);
    } catch (InterruptedException e) {
      logger.error("Failed to getViewFilterValueStats. {}", e);
      Thread.currentThread().interrupt();
      return null;
    }
    return convertToFilterValuesData(result, viewsQueryMetadata.getFields());
  }

  @Override
  public List<QLCEViewEntityStatsDataPoint> getEntityStatsDataPoints(BigQuery bigQuery,
      List<QLCEViewFilterWrapper> filters, List<QLCEViewGroupBy> groupBy, List<QLCEViewAggregation> aggregateFunction,
      List<QLCEViewSortCriteria> sort, String cloudProviderTableName) {
    SelectQuery query = getQuery(filters, groupBy, aggregateFunction, sort, cloudProviderTableName, false);
    QueryJobConfiguration queryConfig = QueryJobConfiguration.newBuilder(query.toString()).build();
    TableResult result;
    try {
      result = bigQuery.query(queryConfig);
    } catch (InterruptedException e) {
      logger.error("Failed to getEntityStatsDataPoints. {}", e);
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
      logger.error("Failed to getTimeSeriesStats. {}", e);
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
      logger.error("Failed to getTrendStatsData. {}", e);
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
        Collections.EMPTY_LIST, Collections.EMPTY_LIST, cloudProviderTableName);
  }

  private SelectQuery getQuery(List<QLCEViewFilterWrapper> filters, List<QLCEViewGroupBy> groupBy,
      List<QLCEViewAggregation> aggregateFunction, List<QLCEViewSortCriteria> sort, String cloudProviderTableName,
      boolean isTimeTruncGroupByRequired) {
    List<ViewRule> viewRuleList = new ArrayList<>();
    List<QLCEViewGroupBy> modifiedGroupBy = new ArrayList<>();
    List<ViewField> customFields = new ArrayList<>();

    Optional<QLCEViewFilterWrapper> viewMetadataFilter = getViewMetadataFilter(filters);

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
      } else {
        modifiedGroupBy = groupBy;
      }
      customFields = customFieldService.getCustomFieldsPerView(viewId);
    }
    List<QLCEViewFilter> idFilters = getIdFilters(filters);
    List<QLCEViewTimeFilter> timeFilters = getTimeFilters(filters);

    return viewsQueryBuilder.getQuery(viewRuleList, idFilters, timeFilters, modifiedGroupBy, aggregateFunction, sort,
        customFields, cloudProviderTableName);
  }

  private static Optional<QLCEViewFilterWrapper> getViewMetadataFilter(List<QLCEViewFilterWrapper> filters) {
    return filters.stream().filter(f -> f.getViewMetadataFilter().getViewId() != null).findFirst();
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

  List<String> convertToFilterValuesData(TableResult result, List<QLCEViewFieldInput> viewFieldList) {
    List<String> filterValues = new ArrayList<>();
    for (FieldValueList row : result.iterateAll()) {
      for (QLCEViewFieldInput field : viewFieldList) {
        filterValues.add(fetchStringValue(row, field));
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
}
