package io.harness.ccm.billing.preaggregated;

import static io.harness.ccm.billing.preaggregated.PreAggregateConstants.entityConstantAwsBlendedCost;
import static io.harness.ccm.billing.preaggregated.PreAggregateConstants.entityConstantAwsInstanceType;
import static io.harness.ccm.billing.preaggregated.PreAggregateConstants.entityConstantAwsLinkedAccount;
import static io.harness.ccm.billing.preaggregated.PreAggregateConstants.entityConstantAwsRegion;
import static io.harness.ccm.billing.preaggregated.PreAggregateConstants.entityConstantAwsService;
import static io.harness.ccm.billing.preaggregated.PreAggregateConstants.entityConstantAwsUnBlendedCost;
import static io.harness.ccm.billing.preaggregated.PreAggregateConstants.entityConstantAwsUsageType;
import static io.harness.ccm.billing.preaggregated.PreAggregateConstants.maxPreAggStartTimeConstant;
import static io.harness.ccm.billing.preaggregated.PreAggregateConstants.minPreAggStartTimeConstant;

import com.google.cloud.Timestamp;
import com.google.cloud.bigquery.Field;
import com.google.cloud.bigquery.FieldList;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.Schema;
import com.google.cloud.bigquery.TableResult;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.healthmarketscience.sqlbuilder.Condition;
import com.healthmarketscience.sqlbuilder.SqlObject;
import com.healthmarketscience.sqlbuilder.dbspec.basic.DbColumn;
import io.harness.ccm.billing.TimeSeriesDataPoints;
import io.harness.ccm.billing.bigquery.AliasExpression;
import io.harness.ccm.billing.bigquery.BigQuerySQL;
import io.harness.ccm.billing.bigquery.ConstExpression;
import io.harness.ccm.billing.bigquery.TruncExpression;
import io.harness.ccm.billing.graphql.BillingTimeFilter;
import io.harness.ccm.billing.graphql.CloudBillingFilter;
import io.harness.ccm.billing.preaggregated.PreAggregateBillingEntityDataPoint.PreAggregateBillingEntityDataPointBuilder;
import io.harness.ccm.billing.preaggregated.PreAggregatedCostData.PreAggregatedCostDataBuilder;
import io.harness.exception.InvalidRequestException;
import lombok.extern.slf4j.Slf4j;
import software.wings.graphql.datafetcher.billing.BillingDataHelper;
import software.wings.graphql.datafetcher.billing.QLEntityData;
import software.wings.graphql.schema.type.aggregation.QLBillingDataPoint;
import software.wings.graphql.schema.type.aggregation.QLBillingDataPoint.QLBillingDataPointBuilder;
import software.wings.graphql.schema.type.aggregation.QLReference;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingStatsInfo;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Singleton
public class PreAggregatedBillingDataHelper {
  @Inject BillingDataHelper billingDataHelper;

  private static final String COST_DESCRIPTION = "of %s - %s";
  private static final String COST_VALUE = "$%s";

  public PreAggregateBillingTimeSeriesStatsDTO convertToPreAggregatesTimeSeriesData(TableResult result) {
    preconditionsValidation(result, "PreAggregate billing time series stats");
    Schema schema = result.getSchema();
    FieldList fields = schema.getFields();

    Map<Timestamp, List<QLBillingDataPoint>> timeSeriesDataPointsMap = new LinkedHashMap();
    for (FieldValueList row : result.iterateAll()) {
      QLBillingDataPointBuilder billingDataPointBuilder = QLBillingDataPoint.builder();
      Timestamp startTimeTruncatedTimestamp = null;
      for (Field field : fields) {
        switch (field.getType().getStandardType()) {
          case TIMESTAMP:
            startTimeTruncatedTimestamp = Timestamp.ofTimeMicroseconds(row.get(field.getName()).getTimestampValue());
            break;
          case STRING:
            String value = fetchStringValue(row, field);
            billingDataPointBuilder.key(QLReference.builder().id(value).name(value).type(field.getName()).build());
            break;
          case FLOAT64:
            billingDataPointBuilder.value(
                billingDataHelper.getRoundedDoubleValue(row.get(field.getName()).getDoubleValue()));
            break;
          default:
            break;
        }
      }

      List<QLBillingDataPoint> dataPoints = new ArrayList<>();
      if (timeSeriesDataPointsMap.containsKey(startTimeTruncatedTimestamp)) {
        dataPoints = timeSeriesDataPointsMap.get(startTimeTruncatedTimestamp);
      }
      dataPoints.add(billingDataPointBuilder.build());
      timeSeriesDataPointsMap.put(startTimeTruncatedTimestamp, dataPoints);
    }

    return PreAggregateBillingTimeSeriesStatsDTO.builder()
        .stats(convertTimeSeriesPointsMapToList(timeSeriesDataPointsMap))
        .build();
  }

  private String fetchStringValue(FieldValueList row, Field field) {
    Object value = row.get(field.getName()).getValue();
    if (value != null) {
      return value.toString();
    }
    return PreAggregateConstants.nullStringValueConstant;
  }

  private List<TimeSeriesDataPoints> convertTimeSeriesPointsMapToList(
      Map<Timestamp, List<QLBillingDataPoint>> timeSeriesDataPointsMap) {
    return timeSeriesDataPointsMap.entrySet()
        .stream()
        .map(e
            -> TimeSeriesDataPoints.builder().time(e.getKey().toSqlTimestamp().getTime()).values(e.getValue()).build())
        .collect(Collectors.toList());
  }

  public String getQuery(List<SqlObject> aggregateFunction, List<Object> groupByObjects, List<Condition> conditions,
      List<SqlObject> sort, boolean addTimeTrucGroupBy) {
    List<Object> selectObjects = new ArrayList<>();
    List<Object> sqlGroupByObjects = new ArrayList<>();
    List<Object> sortObjects = new ArrayList<>();

    if (sort != null) {
      sortObjects.addAll(sort);
    }

    if (groupByObjects != null) {
      groupByObjects.stream().filter(g -> g instanceof DbColumn).forEach(sqlGroupByObjects::add);
      if (addTimeTrucGroupBy) {
        processAndAddTimeTruncatedGroupBy(groupByObjects, sqlGroupByObjects);
      }
      processAndAddGroupBy(sqlGroupByObjects, selectObjects);
    }

    if (aggregateFunction != null) {
      selectObjects.addAll(aggregateFunction);
    }

    BigQuerySQL bigQuerySql = BigQuerySQL.builder()
                                  .selectColumns(selectObjects)
                                  .groupbyObjects(sqlGroupByObjects)
                                  .sortObjects(sortObjects)
                                  .conditions(conditions)
                                  .build();
    return bigQuerySql.getQuery().validate().toString();
  }

  private void processAndAddTimeTruncatedGroupBy(List<Object> groupByObjects, List<Object> sqlGroupByObjects) {
    Optional<Object> timeTruncatedGroupBy =
        groupByObjects.stream().filter(g -> g instanceof TruncExpression).findFirst();

    timeTruncatedGroupBy = Optional.of(new TruncExpression(PreAggregatedTableSchema.startTime,
        getTimeTruncationInterval(timeTruncatedGroupBy), PreAggregateConstants.startTimeTruncatedConstant));

    sqlGroupByObjects.add(timeTruncatedGroupBy.get());
  }

  private TruncExpression.DatePart getTimeTruncationInterval(Optional<Object> timeTruncatedGroupBy) {
    if (timeTruncatedGroupBy.isPresent()) {
      return (TruncExpression.DatePart) ((TruncExpression) timeTruncatedGroupBy.get()).get_datePart();
    }
    return TruncExpression.DatePart.DAY;
  }

  private void processAndAddGroupBy(List<Object> sqlGroupByObjects, List<Object> selectObjects) {
    for (int i = 0; i < sqlGroupByObjects.size(); i++) {
      if (sqlGroupByObjects.get(i) instanceof TruncExpression) {
        selectObjects.add(sqlGroupByObjects.get(i));
        sqlGroupByObjects.set(i, ((TruncExpression) sqlGroupByObjects.get(i)).getAlias());
      }
      if (sqlGroupByObjects.get(i) instanceof ConstExpression) {
        selectObjects.add(sqlGroupByObjects.get(i));
        sqlGroupByObjects.set(i, ((ConstExpression) sqlGroupByObjects.get(i)).getAlias());
      }
      if (sqlGroupByObjects.get(i) instanceof DbColumn) {
        String columnNameSQL = ((DbColumn) sqlGroupByObjects.get(i)).getColumnNameSQL();
        String alias = columnNameSQL.replace(".", "_");
        selectObjects.add(new AliasExpression(columnNameSQL, alias));
      }
    }
  }

  public PreAggregateBillingEntityStatsDTO convertToPreAggregatesEntityData(TableResult result) {
    if (preconditionsValidation(result, "convertToPreAggregatesEntityData")) {
      return null;
    }
    Schema schema = result.getSchema();
    FieldList fields = schema.getFields();
    List<PreAggregateBillingEntityDataPoint> dataPointList = new ArrayList<>();
    for (FieldValueList row : result.iterateAll()) {
      ProcessDataPointAndAppendToList(fields, row, dataPointList);
    }
    return PreAggregateBillingEntityStatsDTO.builder().stats(dataPointList).build();
  }

  @VisibleForTesting
  boolean preconditionsValidation(TableResult result, String entryPoint) {
    Preconditions.checkNotNull(result);
    if (result.getTotalRows() == 0) {
      logger.warn("No result from " + entryPoint + " query");
      return true;
    }
    return false;
  }

  @VisibleForTesting
  void ProcessDataPointAndAppendToList(
      FieldList fields, FieldValueList row, List<PreAggregateBillingEntityDataPoint> dataPointList) {
    PreAggregateBillingEntityDataPointBuilder dataPointBuilder = PreAggregateBillingEntityDataPoint.builder();
    for (Field field : fields) {
      String value = null;
      switch (field.getName()) {
        case entityConstantAwsRegion:
          value = fetchStringValue(row, field);
          dataPointBuilder.id(value);
          dataPointBuilder.awsRegion(value);
          break;
        case entityConstantAwsLinkedAccount:
          value = fetchStringValue(row, field);
          dataPointBuilder.id(value);
          dataPointBuilder.awsLinkedAccount(value);
          break;
        case entityConstantAwsService:
          value = fetchStringValue(row, field);
          dataPointBuilder.id(value);
          dataPointBuilder.awsService(value);
          break;
        case entityConstantAwsUsageType:
          value = fetchStringValue(row, field);
          dataPointBuilder.id(value);
          dataPointBuilder.awsUsageType(value);
          break;
        case entityConstantAwsInstanceType:
          value = fetchStringValue(row, field);
          dataPointBuilder.id(value);
          dataPointBuilder.awsInstanceType(value);
          break;
        case entityConstantAwsBlendedCost:
          dataPointBuilder.awsBlendedCost(
              billingDataHelper.getRoundedDoubleValue(row.get(field.getName()).getDoubleValue()));
          break;
        case entityConstantAwsUnBlendedCost:
          dataPointBuilder.awsUnblendedCost(
              billingDataHelper.getRoundedDoubleValue(row.get(field.getName()).getDoubleValue()));
          break;
        default:
          break;
      }
    }
    dataPointBuilder.costTrend(2.44);
    dataPointList.add(dataPointBuilder.build());
  }

  public PreAggregatedCostDataStats convertToAggregatedCostData(TableResult result) {
    if (preconditionsValidation(result, "convertToAggregatedCostData")) {
      return null;
    }
    Schema schema = result.getSchema();
    FieldList fields = schema.getFields();
    PreAggregatedCostDataBuilder unBlendedCostDataBuilder = PreAggregatedCostData.builder();
    PreAggregatedCostDataBuilder blendedCostDataBuilder = PreAggregatedCostData.builder();
    for (FieldValueList row : result.iterateAll()) {
      processTrendDataAndAppendToList(fields, row, blendedCostDataBuilder, unBlendedCostDataBuilder);
    }
    return PreAggregatedCostDataStats.builder()
        .unBlendedCost(unBlendedCostDataBuilder.build())
        .blendedCost(blendedCostDataBuilder.build())
        .build();
  }

  @VisibleForTesting
  void processTrendDataAndAppendToList(FieldList fields, FieldValueList row,
      PreAggregatedCostDataBuilder blendedCostData, PreAggregatedCostDataBuilder unBlendedCostData) {
    for (Field field : fields) {
      switch (field.getName()) {
        case entityConstantAwsBlendedCost:
          blendedCostData.cost(row.get(field.getName()).getDoubleValue());
          break;
        case entityConstantAwsUnBlendedCost:
          unBlendedCostData.cost(row.get(field.getName()).getDoubleValue());
          break;
        case minPreAggStartTimeConstant:
          blendedCostData.minStartTime(row.get(field.getName()).getTimestampValue());
          unBlendedCostData.minStartTime(row.get(field.getName()).getTimestampValue());
          break;
        case maxPreAggStartTimeConstant:
          blendedCostData.maxStartTime(row.get(field.getName()).getTimestampValue());
          unBlendedCostData.maxStartTime(row.get(field.getName()).getTimestampValue());
          break;
        default:
          break;
      }
    }
  }

  protected QLBillingStatsInfo getCostBillingStats(
      PreAggregatedCostData costData, List<CloudBillingFilter> filters, String label) {
    Instant startInstant = Instant.ofEpochMilli(getStartTimeFilter(filters).getValue().longValue());
    Instant endInstant = Instant.ofEpochMilli(costData.getMaxStartTime() / 1000);
    String startInstantFormat = billingDataHelper.getTotalCostFormattedDate(startInstant);
    String endInstantFormat = billingDataHelper.getTotalCostFormattedDate(endInstant);
    String totalCostDescription = String.format(COST_DESCRIPTION, startInstantFormat, endInstantFormat);
    String totalCostValue = String.format(COST_VALUE, billingDataHelper.getRoundedDoubleValue(costData.getCost()));
    return QLBillingStatsInfo.builder()
        .statsLabel(label)
        .statsDescription(totalCostDescription)
        .statsValue(totalCostValue)
        .statsTrend(2.44)
        .build();
  }

  protected BillingTimeFilter getStartTimeFilter(List<CloudBillingFilter> filters) {
    Optional<CloudBillingFilter> startTimeDataFilter =
        filters.stream()
            .filter(qlStartTimeDataFilter -> qlStartTimeDataFilter.getPreAggregatedStartTime() != null)
            .findFirst();
    if (startTimeDataFilter.isPresent()) {
      return startTimeDataFilter.get().getPreAggregatedTableStartTime();
    } else {
      throw new InvalidRequestException("Start time cannot be null");
    }
  }

  public PreAggregateFilterValuesDTO convertToPreAggregatesFilterValue(TableResult result) {
    if (preconditionsValidation(result, "convertToPreAggregatesFilterValue")) {
      return null;
    }
    Schema schema = result.getSchema();
    FieldList fields = schema.getFields();
    Set<QLEntityData> awsRegion = new HashSet<>();
    Set<QLEntityData> awsService = new HashSet<>();
    Set<QLEntityData> awsUsageType = new HashSet<>();
    Set<QLEntityData> awsInstanceType = new HashSet<>();
    Set<QLEntityData> awsLinkedAccount = new HashSet<>();
    for (FieldValueList row : result.iterateAll()) {
      processAndGetFilterValuesDataPoint(
          fields, row, awsRegion, awsService, awsUsageType, awsInstanceType, awsLinkedAccount);
    }
    return PreAggregateFilterValuesDTO.builder()
        .data(Arrays.asList(PreAggregatedFilterValuesDataPoint.builder()
                                .awsRegion(awsRegion)
                                .awsService(awsService)
                                .awsUsageType(awsUsageType)
                                .awsInstanceType(awsInstanceType)
                                .awsLinkedAccount(awsLinkedAccount)
                                .build()))
        .build();
  }

  @VisibleForTesting
  void processAndGetFilterValuesDataPoint(FieldList fields, FieldValueList row, Set<QLEntityData> awsRegion,
      Set<QLEntityData> awsService, Set<QLEntityData> awsUsageType, Set<QLEntityData> awsInstanceType,
      Set<QLEntityData> awsLinkedAccount) {
    for (Field field : fields) {
      switch (field.getName()) {
        case entityConstantAwsRegion:
          getEntityDataPoint(row, field);
          awsRegion.add(getEntityDataPoint(row, field));
          break;
        case entityConstantAwsLinkedAccount:
          awsLinkedAccount.add(getEntityDataPoint(row, field));
          break;
        case entityConstantAwsService:
          awsService.add(getEntityDataPoint(row, field));
          break;
        case entityConstantAwsUsageType:
          awsUsageType.add(getEntityDataPoint(row, field));
          break;
        case entityConstantAwsInstanceType:
          awsInstanceType.add(getEntityDataPoint(row, field));
          break;
        default:
          break;
      }
    }
  }

  private QLEntityData getEntityDataPoint(FieldValueList row, Field field) {
    String value = fetchStringValue(row, field);
    return QLEntityData.builder().id(value).name(value).type(field.getName()).build();
  }
}
