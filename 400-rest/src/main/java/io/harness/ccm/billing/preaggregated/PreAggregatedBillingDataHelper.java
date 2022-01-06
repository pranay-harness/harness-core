/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ccm.billing.preaggregated;

import static io.harness.annotations.dev.HarnessTeam.CE;
import static io.harness.ccm.billing.graphql.CloudBillingFilter.BILLING_AWS_STARTTIME;
import static io.harness.ccm.billing.preaggregated.PreAggregateConstants.entityCloudProviderConst;
import static io.harness.ccm.billing.preaggregated.PreAggregateConstants.entityConstantAwsBlendedCost;
import static io.harness.ccm.billing.preaggregated.PreAggregateConstants.entityConstantAwsInstanceType;
import static io.harness.ccm.billing.preaggregated.PreAggregateConstants.entityConstantAwsLinkedAccount;
import static io.harness.ccm.billing.preaggregated.PreAggregateConstants.entityConstantAwsNoInstanceType;
import static io.harness.ccm.billing.preaggregated.PreAggregateConstants.entityConstantAwsNoLinkedAccount;
import static io.harness.ccm.billing.preaggregated.PreAggregateConstants.entityConstantAwsNoService;
import static io.harness.ccm.billing.preaggregated.PreAggregateConstants.entityConstantAwsNoUsageType;
import static io.harness.ccm.billing.preaggregated.PreAggregateConstants.entityConstantAwsService;
import static io.harness.ccm.billing.preaggregated.PreAggregateConstants.entityConstantAwsTagKey;
import static io.harness.ccm.billing.preaggregated.PreAggregateConstants.entityConstantAwsTagValue;
import static io.harness.ccm.billing.preaggregated.PreAggregateConstants.entityConstantAwsUnBlendedCost;
import static io.harness.ccm.billing.preaggregated.PreAggregateConstants.entityConstantAwsUsageType;
import static io.harness.ccm.billing.preaggregated.PreAggregateConstants.entityConstantGcpBillingAccount;
import static io.harness.ccm.billing.preaggregated.PreAggregateConstants.entityConstantGcpCost;
import static io.harness.ccm.billing.preaggregated.PreAggregateConstants.entityConstantGcpDiscount;
import static io.harness.ccm.billing.preaggregated.PreAggregateConstants.entityConstantGcpLabelKey;
import static io.harness.ccm.billing.preaggregated.PreAggregateConstants.entityConstantGcpLabelValue;
import static io.harness.ccm.billing.preaggregated.PreAggregateConstants.entityConstantGcpNoProduct;
import static io.harness.ccm.billing.preaggregated.PreAggregateConstants.entityConstantGcpNoProjectId;
import static io.harness.ccm.billing.preaggregated.PreAggregateConstants.entityConstantGcpNoSku;
import static io.harness.ccm.billing.preaggregated.PreAggregateConstants.entityConstantGcpNoSkuId;
import static io.harness.ccm.billing.preaggregated.PreAggregateConstants.entityConstantGcpProduct;
import static io.harness.ccm.billing.preaggregated.PreAggregateConstants.entityConstantGcpProjectId;
import static io.harness.ccm.billing.preaggregated.PreAggregateConstants.entityConstantGcpSku;
import static io.harness.ccm.billing.preaggregated.PreAggregateConstants.entityConstantGcpSkuId;
import static io.harness.ccm.billing.preaggregated.PreAggregateConstants.entityConstantNoRegion;
import static io.harness.ccm.billing.preaggregated.PreAggregateConstants.entityConstantRawTableAwsBlendedCost;
import static io.harness.ccm.billing.preaggregated.PreAggregateConstants.entityConstantRawTableAwsInstanceType;
import static io.harness.ccm.billing.preaggregated.PreAggregateConstants.entityConstantRawTableAwsLinkedAccount;
import static io.harness.ccm.billing.preaggregated.PreAggregateConstants.entityConstantRawTableAwsService;
import static io.harness.ccm.billing.preaggregated.PreAggregateConstants.entityConstantRawTableAwsUnBlendedCost;
import static io.harness.ccm.billing.preaggregated.PreAggregateConstants.entityConstantRawTableAwsUsageType;
import static io.harness.ccm.billing.preaggregated.PreAggregateConstants.entityConstantRawTableGcpBillingAccount;
import static io.harness.ccm.billing.preaggregated.PreAggregateConstants.entityConstantRawTableGcpProduct;
import static io.harness.ccm.billing.preaggregated.PreAggregateConstants.entityConstantRawTableGcpProjectId;
import static io.harness.ccm.billing.preaggregated.PreAggregateConstants.entityConstantRawTableGcpRegion;
import static io.harness.ccm.billing.preaggregated.PreAggregateConstants.entityConstantRawTableGcpSku;
import static io.harness.ccm.billing.preaggregated.PreAggregateConstants.entityConstantRawTableGcpSkuId;
import static io.harness.ccm.billing.preaggregated.PreAggregateConstants.entityConstantRegion;
import static io.harness.ccm.billing.preaggregated.PreAggregateConstants.entityNoCloudProviderConst;
import static io.harness.ccm.billing.preaggregated.PreAggregateConstants.entityNoLabelConst;
import static io.harness.ccm.billing.preaggregated.PreAggregateConstants.entityNoTagConst;
import static io.harness.ccm.billing.preaggregated.PreAggregateConstants.maxPreAggStartTimeConstant;
import static io.harness.ccm.billing.preaggregated.PreAggregateConstants.minPreAggStartTimeConstant;
import static io.harness.ccm.billing.preaggregated.PreAggregateConstants.nullStringValueConstant;
import static io.harness.ccm.billing.preaggregated.PreAggregateConstants.totalStringValueConstant;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.ccm.billing.RawBillingTableSchema;
import io.harness.ccm.billing.TimeSeriesDataPoints;
import io.harness.ccm.billing.bigquery.AliasExpression;
import io.harness.ccm.billing.bigquery.BigQuerySQL;
import io.harness.ccm.billing.bigquery.ConstExpression;
import io.harness.ccm.billing.bigquery.TruncExpression;
import io.harness.ccm.billing.graphql.CloudBillingFilter;
import io.harness.ccm.billing.graphql.CloudBillingTimeFilter;
import io.harness.ccm.billing.preaggregated.PreAggregateBillingEntityDataPoint.PreAggregateBillingEntityDataPointBuilder;
import io.harness.ccm.billing.preaggregated.PreAggregateCloudOverviewDataPoint.PreAggregateCloudOverviewDataPointBuilder;
import io.harness.ccm.billing.preaggregated.PreAggregatedCostData.PreAggregatedCostDataBuilder;
import io.harness.exception.InvalidRequestException;

import software.wings.graphql.datafetcher.billing.BillingDataHelper;
import software.wings.graphql.datafetcher.billing.CloudBillingHelper;
import software.wings.graphql.datafetcher.billing.QLBillingAmountData;
import software.wings.graphql.datafetcher.billing.QLEntityData;
import software.wings.graphql.schema.type.QLK8sLabel;
import software.wings.graphql.schema.type.aggregation.QLBillingDataPoint;
import software.wings.graphql.schema.type.aggregation.QLBillingDataPoint.QLBillingDataPointBuilder;
import software.wings.graphql.schema.type.aggregation.QLReference;
import software.wings.graphql.schema.type.aggregation.QLTimeOperator;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingStatsInfo;

import com.google.cloud.Timestamp;
import com.google.cloud.bigquery.Field;
import com.google.cloud.bigquery.FieldList;
import com.google.cloud.bigquery.FieldValue;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.Schema;
import com.google.cloud.bigquery.StandardSQLTypeName;
import com.google.cloud.bigquery.TableResult;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.healthmarketscience.sqlbuilder.Condition;
import com.healthmarketscience.sqlbuilder.SelectQuery;
import com.healthmarketscience.sqlbuilder.SqlObject;
import com.healthmarketscience.sqlbuilder.dbspec.basic.DbColumn;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
@OwnedBy(CE)
@TargetModule(HarnessModule._375_CE_GRAPHQL)
public class PreAggregatedBillingDataHelper {
  @Inject BillingDataHelper billingDataHelper;
  @Inject CloudBillingHelper cloudBillingHelper;

  private static final String COST_DESCRIPTION = "of %s - %s";
  private static final String COST_VALUE = "$%s";
  private static final String AWS_ACCOUNT_TEMPLATE = "%s (%s)";
  private static final String LABEL = "%s:%s";

  public PreAggregateBillingTimeSeriesStatsDTO convertToPreAggregatesTimeSeriesData(TableResult result) {
    preconditionsValidation(result, "PreAggregate billing time series stats");
    Schema schema = result.getSchema();
    FieldList fields = schema.getFields();

    Map<Timestamp, List<QLBillingDataPoint>> timeSeriesDataPointsMap = new LinkedHashMap();
    for (FieldValueList row : result.iterateAll()) {
      QLBillingDataPointBuilder billingDataPointBuilder = QLBillingDataPoint.builder();
      Timestamp startTimeTruncatedTimestamp = null;
      Double value = Double.valueOf(0);
      for (Field field : fields) {
        switch (field.getType().getStandardType()) {
          case TIMESTAMP:
            startTimeTruncatedTimestamp = Timestamp.ofTimeMicroseconds(row.get(field.getName()).getTimestampValue());
            break;
          case STRING:
            setTimeSeriesDataPointKey(row, field, billingDataPointBuilder);
            break;
          case FLOAT64:
            value += getNumericValue(row, field);
            break;
          default:
            break;
        }
      }

      billingDataPointBuilder.value(billingDataHelper.getRoundedDoubleValue(value));
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

  private void setTimeSeriesDataPointKey(
      FieldValueList row, Field field, QLBillingDataPointBuilder billingDataPointBuilder) {
    String value;
    switch (field.getName()) {
      case entityConstantGcpLabelKey:
      case entityConstantAwsTagKey:
        boolean isLabel = field.getName().equals(entityConstantGcpLabelKey);
        String labelKey = fetchStringValue(row, field);
        String labelValue = fetchStringValue(row,
            Field
                .newBuilder(
                    isLabel ? entityConstantGcpLabelValue : entityConstantAwsTagValue, StandardSQLTypeName.STRING)
                .build());
        value = String.format(LABEL, labelKey, labelValue);
        if (labelKey.equals(nullStringValueConstant)) {
          value = isLabel ? entityNoLabelConst : entityNoTagConst;
        }
        billingDataPointBuilder.key(QLReference.builder().id(value).name(value).type(field.getName()).build());
        return;
      case entityConstantGcpLabelValue:
      case entityConstantAwsTagValue:
        return;
      default:
        value = fetchStringValue(row, field);
        billingDataPointBuilder.key(QLReference.builder().id(value).name(value).type(field.getName()).build());
        return;
    }
  }

  public static String fetchStringValue(FieldValueList row, Field field) {
    Object value = row.get(field.getName()).getValue();
    if (value != null) {
      return value.toString();
    }
    return getDefaultValue(field);
  }

  protected static String getDefaultValue(Field field) {
    switch (field.getName()) {
      case entityConstantRegion:
      case entityConstantRawTableGcpRegion:
        return entityConstantNoRegion;
      case entityConstantAwsLinkedAccount:
      case entityConstantRawTableAwsLinkedAccount:
        return entityConstantAwsNoLinkedAccount;
      case entityConstantAwsService:
      case entityConstantRawTableAwsService:
        return entityConstantAwsNoService;
      case entityConstantAwsUsageType:
      case entityConstantRawTableAwsUsageType:
        return entityConstantAwsNoUsageType;
      case entityConstantAwsInstanceType:
      case entityConstantRawTableAwsInstanceType:
        return entityConstantAwsNoInstanceType;
      case entityConstantGcpProjectId:
      case entityConstantRawTableGcpProjectId:
        return entityConstantGcpNoProjectId;
      case entityConstantGcpProduct:
      case entityConstantRawTableGcpProduct:
        return entityConstantGcpNoProduct;
      case entityConstantGcpSku:
      case entityConstantRawTableGcpSku:
        return entityConstantGcpNoSku;
      case entityConstantGcpSkuId:
      case entityConstantRawTableGcpSkuId:
        return entityConstantGcpNoSkuId;
      case entityCloudProviderConst:
        return entityNoCloudProviderConst;
      default:
        return nullStringValueConstant;
    }
  }

  public static double getNumericValue(FieldValueList row, Field field) {
    FieldValue value = row.get(field.getName());
    if (!value.isNull()) {
      return Math.round(value.getNumericValue().doubleValue() * 100D) / 100D;
    }
    return 0;
  }

  private long getTimeStampValue(FieldValueList row, Field field) {
    FieldValue value = row.get(field.getName());
    if (!value.isNull()) {
      return value.getTimestampValue();
    }
    return 0;
  }

  private long getTimeStampValue(FieldValueList row, String fieldName) {
    FieldValue value = row.get(fieldName);
    if (!value.isNull()) {
      return value.getTimestampValue();
    }
    return 0;
  }

  public static List<TimeSeriesDataPoints> convertTimeSeriesPointsMapToList(
      Map<Timestamp, List<QLBillingDataPoint>> timeSeriesDataPointsMap) {
    return timeSeriesDataPointsMap.entrySet()
        .stream()
        .map(e
            -> TimeSeriesDataPoints.builder().time(e.getKey().toSqlTimestamp().getTime()).values(e.getValue()).build())
        .collect(Collectors.toList());
  }

  public SelectQuery getQuery(List<SqlObject> aggregateFunction, List<Object> groupByObjects,
      List<Condition> conditions, List<SqlObject> sort, boolean addTimeTruncGroupBy) {
    return getQuery(aggregateFunction, groupByObjects, conditions, sort, addTimeTruncGroupBy, false, false);
  }

  public SelectQuery getQuery(List<SqlObject> aggregateFunction, List<Object> groupByObjects,
      List<Condition> conditions, List<SqlObject> sort, boolean addTimeTruncGroupBy, boolean isQueryRawTable,
      boolean isAWSCloudProvider) {
    List<Object> selectObjects = new ArrayList<>();
    List<Object> sqlGroupByObjects = new ArrayList<>();
    List<Object> sortObjects = new ArrayList<>();

    if (sort != null) {
      sortObjects.addAll(sort);
    }

    if (groupByObjects != null) {
      groupByObjects.stream().filter(g -> g instanceof DbColumn).forEach(sqlGroupByObjects::add);
      if (addTimeTruncGroupBy) {
        processAndAddTimeTruncatedGroupBy(groupByObjects, sqlGroupByObjects, isQueryRawTable, isAWSCloudProvider);
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
    return bigQuerySql.getQuery().validate();
  }

  private void processAndAddTimeTruncatedGroupBy(List<Object> groupByObjects, List<Object> sqlGroupByObjects,
      boolean isQueryRawTable, boolean isAWSCloudProvider) {
    Optional<Object> timeTruncatedGroupBy =
        groupByObjects.stream().filter(g -> g instanceof TruncExpression).findFirst();
    DbColumn rawTableStartTime =
        isAWSCloudProvider ? RawBillingTableSchema.awsStartTime : RawBillingTableSchema.startTime;

    timeTruncatedGroupBy =
        Optional.of(new TruncExpression(isQueryRawTable ? rawTableStartTime : PreAggregatedTableSchema.startTime,
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
        if (columnNameSQL.equals(RawBillingTableSchema.labelsKey.getColumnNameSQL())
            || columnNameSQL.equals(RawBillingTableSchema.labelsValue.getColumnNameSQL())
            || columnNameSQL.equals(RawBillingTableSchema.tagsKey.getColumnNameSQL())
            || columnNameSQL.equals(RawBillingTableSchema.tagsValue.getColumnNameSQL())) {
          sqlGroupByObjects.set(i, alias);
        }
        selectObjects.add(new AliasExpression(columnNameSQL, alias));
      }
    }
  }

  public PreAggregateBillingEntityStatsDTO convertToPreAggregatesEntityData(TableResult result,
      Map<String, String> awsCloudAccountMap, Map<String, PreAggregatedCostData> idToPrevBillingAmountData,
      List<CloudBillingFilter> filters, Instant trendFilterStartTime) {
    if (preconditionsValidation(result, "convertToPreAggregatesEntityData")) {
      return null;
    }
    Schema schema = result.getSchema();
    FieldList fields = schema.getFields();
    List<PreAggregateBillingEntityDataPoint> dataPointList = new ArrayList<>();
    for (FieldValueList row : result.iterateAll()) {
      processDataPointAndAppendToList(
          fields, row, dataPointList, awsCloudAccountMap, idToPrevBillingAmountData, filters, trendFilterStartTime);
    }
    return PreAggregateBillingEntityStatsDTO.builder().stats(dataPointList).build();
  }

  @VisibleForTesting
  static boolean preconditionsValidation(TableResult result, String entryPoint) {
    Preconditions.checkNotNull(result);
    if (result.getTotalRows() == 0) {
      log.warn("No result from " + entryPoint + " query");
      return true;
    }
    return false;
  }

  @VisibleForTesting
  void processDataPointAndAppendToList(FieldList fields, FieldValueList row,
      List<PreAggregateBillingEntityDataPoint> dataPointList, Map<String, String> awsCloudAccountMap,
      Map<String, PreAggregatedCostData> idToPrevBillingAmountData, List<CloudBillingFilter> filters,
      Instant trendFilterStartTime) {
    PreAggregateBillingEntityDataPointBuilder dataPointBuilder = PreAggregateBillingEntityDataPoint.builder();
    PreAggregatedCostDataBuilder preAggregatedCostDataBuilder = PreAggregatedCostData.builder();
    Double cloudCost = Double.valueOf(0);
    dataPointBuilder.id(totalStringValueConstant);
    for (Field field : fields) {
      String value = null;
      switch (field.getName()) {
        case entityConstantRegion:
        case entityConstantRawTableGcpRegion:
          value = fetchStringValue(row, field);
          dataPointBuilder.id(value);
          dataPointBuilder.region(value);
          break;
        case entityConstantAwsLinkedAccount:
        case entityConstantRawTableAwsLinkedAccount:
          String awsAccountId = fetchStringValue(row, field);
          dataPointBuilder.id(awsAccountId);
          dataPointBuilder.awsLinkedAccount(getAwsAccountName(awsAccountId, awsCloudAccountMap));
          break;
        case entityConstantAwsService:
        case entityConstantRawTableAwsService:
          value = fetchStringValue(row, field);
          dataPointBuilder.id(value);
          dataPointBuilder.awsService(value);
          break;
        case entityConstantAwsUsageType:
        case entityConstantRawTableAwsUsageType:
          value = fetchStringValue(row, field);
          dataPointBuilder.id(value);
          dataPointBuilder.awsUsageType(value);
          break;
        case entityConstantAwsInstanceType:
        case entityConstantRawTableAwsInstanceType:
          value = fetchStringValue(row, field);
          dataPointBuilder.id(value);
          dataPointBuilder.awsInstanceType(value);
          break;
        case entityConstantAwsBlendedCost:
        case entityConstantRawTableAwsBlendedCost:
          double blendedCost = getNumericValue(row, field);
          cloudCost += blendedCost;
          dataPointBuilder.awsBlendedCost(billingDataHelper.getRoundedDoubleValue(blendedCost));
          break;
        case entityConstantAwsUnBlendedCost:
        case entityConstantRawTableAwsUnBlendedCost:
          double unBlendedCost = getNumericValue(row, field);
          cloudCost += unBlendedCost;
          dataPointBuilder.awsUnblendedCost(billingDataHelper.getRoundedDoubleValue(unBlendedCost));
          break;
        case entityConstantGcpProjectId:
        case entityConstantRawTableGcpProjectId:
          value = fetchStringValue(row, field);
          dataPointBuilder.id(value);
          dataPointBuilder.gcpProjectId(value);
          break;
        case entityConstantGcpProduct:
        case entityConstantRawTableGcpProduct:
          value = fetchStringValue(row, field);
          dataPointBuilder.id(value);
          dataPointBuilder.gcpProduct(value);
          break;
        case entityConstantGcpSku:
        case entityConstantRawTableGcpSku:
          value = fetchStringValue(row, field);
          dataPointBuilder.id(value);
          dataPointBuilder.gcpSkuDescription(value);
          break;
        case entityConstantGcpSkuId:
        case entityConstantRawTableGcpSkuId:
          value = fetchStringValue(row, field);
          dataPointBuilder.id(value);
          dataPointBuilder.gcpSkuId(value);
          break;
        case entityConstantGcpLabelKey:
          String labelKey = fetchStringValue(row, field);
          String labelValue =
              fetchStringValue(row, Field.newBuilder(entityConstantGcpLabelValue, StandardSQLTypeName.STRING).build());
          value = String.format(LABEL, labelKey, labelValue);
          if (labelKey.equals(nullStringValueConstant)) {
            value = entityNoLabelConst;
          }
          dataPointBuilder.id(value);
          dataPointBuilder.gcpLabel(value);
          break;
        case entityConstantAwsTagKey:
          String tagKey = fetchStringValue(row, field);
          String tagValue =
              fetchStringValue(row, Field.newBuilder(entityConstantAwsTagValue, StandardSQLTypeName.STRING).build());
          value = String.format(LABEL, tagKey, tagValue);
          if (tagKey.equals(nullStringValueConstant)) {
            value = entityNoTagConst;
          }
          dataPointBuilder.id(value);
          dataPointBuilder.awsTag(value);
          break;
        case entityConstantGcpCost:
          double cost = getNumericValue(row, field);
          cloudCost += cost;
          dataPointBuilder.gcpTotalCost(billingDataHelper.getRoundedDoubleValue(cost));
          break;
        case entityConstantGcpDiscount:
          double discount = getNumericValue(row, field);
          cloudCost += discount;
          dataPointBuilder.gcpDiscount(billingDataHelper.getRoundedDoubleValue(discount));
          break;
        default:
          break;
      }
    }

    preAggregatedCostDataBuilder.cost(cloudCost);
    dataPointBuilder.gcpSubTotalCost(billingDataHelper.getRoundedDoubleValue(cloudCost));

    PreAggregatedCostData preAggregatedCostData =
        preAggregatedCostDataBuilder.maxStartTime(getTimeStampValue(row, maxPreAggStartTimeConstant))
            .minStartTime(getTimeStampValue(row, minPreAggStartTimeConstant))
            .build();
    PreAggregateBillingEntityDataPoint billingEntityDataPoint = dataPointBuilder.build();
    if (idToPrevBillingAmountData != null && idToPrevBillingAmountData.containsKey(billingEntityDataPoint.getId())) {
      billingEntityDataPoint.setCostTrend(getCostTrend(preAggregatedCostData,
          idToPrevBillingAmountData.get(billingEntityDataPoint.getId()), filters, trendFilterStartTime));
    }
    dataPointList.add(billingEntityDataPoint);
  }

  private double getCostTrend(PreAggregatedCostData preAggregatedCostData,
      PreAggregatedCostData prevPreAggregatedCostData, List<CloudBillingFilter> filters, Instant trendFilterStartTime) {
    BigDecimal forecastCost =
        billingDataHelper.getForecastCost(QLBillingAmountData.builder()
                                              .cost(BigDecimal.valueOf(preAggregatedCostData.getCost()))
                                              .minStartTime(preAggregatedCostData.getMinStartTime() / 1000)
                                              .maxStartTime(preAggregatedCostData.getMaxStartTime() / 1000)
                                              .build(),
            Instant.ofEpochMilli(getEndTimeFilter(filters).getValue().longValue()));
    return getBillingTrend(BigDecimal.valueOf(preAggregatedCostData.getCost()), forecastCost, prevPreAggregatedCostData,
        trendFilterStartTime);
  }

  public PreAggregatedCostDataStats convertToAggregatedCostData(TableResult result) {
    if (preconditionsValidation(result, "convertToAggregatedCostData")) {
      return null;
    }
    Schema schema = result.getSchema();
    FieldList fields = schema.getFields();
    PreAggregatedCostDataBuilder unBlendedCostDataBuilder = PreAggregatedCostData.builder();
    PreAggregatedCostDataBuilder blendedCostDataBuilder = PreAggregatedCostData.builder();
    PreAggregatedCostDataBuilder costDataBuilder = PreAggregatedCostData.builder();

    for (FieldValueList row : result.iterateAll()) {
      processTrendDataAndAppendToList(fields, row, blendedCostDataBuilder, unBlendedCostDataBuilder, costDataBuilder);
    }
    return PreAggregatedCostDataStats.builder()
        .unBlendedCost(unBlendedCostDataBuilder.build())
        .blendedCost(blendedCostDataBuilder.build())
        .cost(costDataBuilder.build())
        .build();
  }

  @VisibleForTesting
  void processTrendDataAndAppendToList(FieldList fields, FieldValueList row,
      PreAggregatedCostDataBuilder blendedCostData, PreAggregatedCostDataBuilder unBlendedCostData,
      PreAggregatedCostDataBuilder costDataBuilder) {
    Double gcpCost = Double.valueOf(0);
    for (Field field : fields) {
      switch (field.getName()) {
        case entityConstantAwsBlendedCost:
          blendedCostData.cost(getNumericValue(row, field));
          break;
        case entityConstantAwsUnBlendedCost:
          unBlendedCostData.cost(getNumericValue(row, field));
          break;
        case entityConstantGcpCost:
        case entityConstantGcpDiscount:
          gcpCost += getNumericValue(row, field);
          break;
        case minPreAggStartTimeConstant:
          long minStartTime = getTimeStampValue(row, field);
          costDataBuilder.minStartTime(minStartTime);
          blendedCostData.minStartTime(minStartTime);
          unBlendedCostData.minStartTime(minStartTime);
          break;
        case maxPreAggStartTimeConstant:
          long maxStartTime = getTimeStampValue(row, field);
          costDataBuilder.maxStartTime(maxStartTime);
          blendedCostData.maxStartTime(maxStartTime);
          unBlendedCostData.maxStartTime(maxStartTime);
          break;
        default:
          break;
      }
    }
    costDataBuilder.cost(billingDataHelper.getRoundedDoubleValue(gcpCost));
  }

  protected QLBillingStatsInfo getCostBillingStats(PreAggregatedCostData costData, PreAggregatedCostData prevCostData,
      List<CloudBillingFilter> filters, String label, Instant trendFilterStartTime) {
    Instant startInstant = Instant.ofEpochMilli(getStartTimeFilter(filters).getValue().longValue());
    Instant endInstant = Instant.ofEpochMilli(costData.getMaxStartTime() / 1000);
    if (costData.getMaxStartTime() == 0) {
      endInstant = Instant.ofEpochMilli(getEndTimeFilter(filters).getValue().longValue());
    }
    boolean isYearRequired = billingDataHelper.isYearRequired(startInstant, endInstant);
    String startInstantFormat = billingDataHelper.getTotalCostFormattedDate(startInstant, isYearRequired);
    String endInstantFormat = billingDataHelper.getTotalCostFormattedDate(endInstant, isYearRequired);
    String totalCostDescription = String.format(COST_DESCRIPTION, startInstantFormat, endInstantFormat);
    String totalCostValue = String.format(
        COST_VALUE, billingDataHelper.formatNumber(billingDataHelper.getRoundedDoubleValue(costData.getCost())));

    BigDecimal forecastCost = billingDataHelper.getForecastCost(QLBillingAmountData.builder()
                                                                    .cost(BigDecimal.valueOf(costData.getCost()))
                                                                    .minStartTime(costData.getMinStartTime() / 1000)
                                                                    .maxStartTime(costData.getMaxStartTime() / 1000)
                                                                    .build(),
        Instant.ofEpochMilli(getEndTimeFilter(filters).getValue().longValue()));

    return QLBillingStatsInfo.builder()
        .statsLabel(label)
        .statsDescription(totalCostDescription)
        .statsValue(totalCostValue)
        .statsTrend(
            getBillingTrend(BigDecimal.valueOf(costData.getCost()), forecastCost, prevCostData, trendFilterStartTime))
        .build();
  }

  private Double getBillingTrend(BigDecimal totalBillingAmount, BigDecimal forecastCost,
      PreAggregatedCostData prevCostData, Instant trendFilterStartTime) {
    Double trendCostValue = 0.0;
    if (prevCostData != null && prevCostData.getCost() > 0) {
      BigDecimal prevTotalBillingAmount = BigDecimal.valueOf(prevCostData.getCost());
      Instant startInstant = Instant.ofEpochMilli(prevCostData.getMinStartTime() / 1000);
      BigDecimal amountDifference = totalBillingAmount.subtract(prevTotalBillingAmount);
      if (null != forecastCost) {
        amountDifference = forecastCost.subtract(prevTotalBillingAmount);
      }
      if (trendFilterStartTime.plus(1, ChronoUnit.DAYS).isAfter(startInstant)) {
        BigDecimal trendPercentage =
            amountDifference.multiply(BigDecimal.valueOf(100)).divide(prevTotalBillingAmount, 2, RoundingMode.HALF_UP);
        trendCostValue = billingDataHelper.getRoundedDoubleValue(trendPercentage.doubleValue());
      }
    }
    return trendCostValue;
  }

  protected List<CloudBillingFilter> getTrendFilters(List<CloudBillingFilter> filters) {
    Instant endInstant = Instant.ofEpochMilli(getEndTimeFilter(filters).getValue().longValue());
    Instant startInstant = Instant.ofEpochMilli(getStartTimeFilter(filters).getValue().longValue());
    long diffMillis = Duration.between(startInstant, endInstant).toMillis();
    long trendEndTime = startInstant.toEpochMilli() - 1000;
    long trendStartTime = trendEndTime - diffMillis;

    List<CloudBillingFilter> trendFilters =
        filters.stream()
            .filter(qlDataFilter
                -> qlDataFilter.getPreAggregatedStartTime() == null && qlDataFilter.getPreAggregatedEndTime() == null)
            .collect(Collectors.toList());
    trendFilters.add(getStartTimeBillingFilter(trendStartTime));
    trendFilters.add(getEndTimeBillingFilter(trendEndTime));

    return trendFilters;
  }

  protected CloudBillingFilter getStartTimeBillingFilter(Long filterTime) {
    CloudBillingFilter cloudBillingFilter = new CloudBillingFilter();
    cloudBillingFilter.setPreAggregatedTableStartTime(CloudBillingTimeFilter.builder()
                                                          .variable(BILLING_AWS_STARTTIME)
                                                          .operator(QLTimeOperator.AFTER)
                                                          .value(filterTime)
                                                          .build());
    return cloudBillingFilter;
  }

  protected CloudBillingFilter getEndTimeBillingFilter(Long filterTime) {
    CloudBillingFilter cloudBillingFilter = new CloudBillingFilter();
    cloudBillingFilter.setPreAggregatedTableEndTime(CloudBillingTimeFilter.builder()
                                                        .variable(BILLING_AWS_STARTTIME)
                                                        .operator(QLTimeOperator.BEFORE)
                                                        .value(filterTime)
                                                        .build());
    return cloudBillingFilter;
  }

  protected List<Condition> filtersToConditions(List<CloudBillingFilter> filters) {
    return filtersToConditions(filters, false, false);
  }

  protected List<Condition> filtersToConditions(
      List<CloudBillingFilter> filters, boolean isQueryRawTable, boolean isAWSCloudProvider) {
    return Optional.ofNullable(filters)
        .map(Collection::stream)
        .orElseGet(Stream::empty)
        .map(cloudBillingHelper.getFiltersMapper(isAWSCloudProvider, isQueryRawTable))
        .collect(Collectors.toList());
  }

  protected CloudBillingTimeFilter getStartTimeFilter(List<CloudBillingFilter> filters) {
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

  protected CloudBillingTimeFilter getEndTimeFilter(List<CloudBillingFilter> filters) {
    Optional<CloudBillingFilter> endTimeDataFilter =
        filters.stream()
            .filter(qlEndTimeDataFilter -> qlEndTimeDataFilter.getPreAggregatedEndTime() != null)
            .findFirst();
    if (endTimeDataFilter.isPresent()) {
      return endTimeDataFilter.get().getPreAggregatedTableEndTime();
    } else {
      throw new InvalidRequestException("End time cannot be null");
    }
  }

  public PreAggregateFilterValuesDTO convertToPreAggregatesFilterValue(
      TableResult result, Map<String, String> awsCloudAccountMap) {
    if (preconditionsValidation(result, "convertToPreAggregatesFilterValue")) {
      return null;
    }

    Schema schema = result.getSchema();
    FieldList fields = schema.getFields();
    Set<QLEntityData> region = new HashSet<>();
    Set<QLEntityData> awsService = new HashSet<>();
    Set<QLEntityData> awsUsageType = new HashSet<>();
    Set<QLEntityData> awsInstanceType = new HashSet<>();
    Set<QLEntityData> awsLinkedAccount = new HashSet<>();
    Set<QLEntityData> gcpProjectId = new HashSet<>();
    Set<QLEntityData> gcpProduct = new HashSet<>();
    Set<QLEntityData> gcpSku = new HashSet<>();
    Set<QLEntityData> gcpBillingAccount = new HashSet<>();
    Map<String, Set<String>> labelsMap = new HashMap<>();
    Map<String, Set<String>> tagsMap = new HashMap<>();

    for (FieldValueList row : result.iterateAll()) {
      for (Field field : fields) {
        switch (field.getName()) {
          case entityConstantRawTableGcpRegion:
          case entityConstantRegion:
            getEntityDataPoint(row, field);
            region.add(getEntityDataPoint(row, field));
            break;
          case entityConstantAwsLinkedAccount:
          case entityConstantRawTableAwsLinkedAccount:
            String awsAccountId = fetchStringValue(row, field);
            String awsAccountName = getAwsAccountName(awsAccountId, awsCloudAccountMap);
            awsLinkedAccount.add(
                QLEntityData.builder().id(awsAccountId).name(awsAccountName).type(field.getName()).build());
            break;
          case entityConstantAwsService:
          case entityConstantRawTableAwsService:
            awsService.add(getEntityDataPoint(row, field));
            break;
          case entityConstantAwsUsageType:
          case entityConstantRawTableAwsUsageType:
            awsUsageType.add(getEntityDataPoint(row, field));
            break;
          case entityConstantAwsInstanceType:
          case entityConstantRawTableAwsInstanceType:
            awsInstanceType.add(getEntityDataPoint(row, field));
            break;
          case entityConstantGcpSku:
          case entityConstantRawTableGcpSku:
            gcpSku.add(getEntityDataPoint(row, field));
            break;
          case entityConstantGcpProduct:
          case entityConstantRawTableGcpProduct:
            gcpProduct.add(getEntityDataPoint(row, field));
            break;
          case entityConstantGcpProjectId:
          case entityConstantRawTableGcpProjectId:
            gcpProjectId.add(getEntityDataPoint(row, field));
            break;
          case entityConstantGcpBillingAccount:
          case entityConstantRawTableGcpBillingAccount:
            gcpBillingAccount.add(getEntityDataPoint(row, field));
            break;
          case entityConstantGcpLabelKey:
            String labelKey = fetchStringValue(row, field);
            String labelValue = fetchStringValue(
                row, Field.newBuilder(entityConstantGcpLabelValue, StandardSQLTypeName.STRING).build());
            if (!labelKey.equals(nullStringValueConstant)) {
              updateLabelsMap(labelsMap, labelKey, labelValue);
            }
            break;
          case entityConstantAwsTagKey:
            String tagKey = fetchStringValue(row, field);
            String tagValue =
                fetchStringValue(row, Field.newBuilder(entityConstantAwsTagValue, StandardSQLTypeName.STRING).build());
            if (!tagKey.equals(nullStringValueConstant)) {
              updateLabelsMap(tagsMap, tagKey, tagValue);
            }
            break;
          default:
            break;
        }
      }
    }

    List<QLK8sLabel> labelsDTO = new ArrayList<>();
    List<QLK8sLabel> tagsDTO = new ArrayList<>();
    getLabelsDTO(labelsMap, labelsDTO);
    getLabelsDTO(tagsMap, tagsDTO);

    return PreAggregateFilterValuesDTO.builder()
        .data(Arrays.asList(PreAggregatedFilterValuesDataPoint.builder()
                                .region(region)
                                .awsService(awsService)
                                .awsUsageType(awsUsageType)
                                .awsInstanceType(awsInstanceType)
                                .awsLinkedAccount(awsLinkedAccount)
                                .gcpSku(gcpSku)
                                .gcpProduct(gcpProduct)
                                .gcpProjectId(gcpProjectId)
                                .gcpBillingAccount(gcpBillingAccount)
                                .gcpLabels(labelsDTO)
                                .awsTags(tagsDTO)
                                .build()))
        .build();
  }

  private void getLabelsDTO(Map<String, Set<String>> labelsMap, List<QLK8sLabel> labelsDTO) {
    for (Map.Entry<String, Set<String>> labels : labelsMap.entrySet()) {
      labelsDTO.add(QLK8sLabel.builder()
                        .name(labels.getKey())
                        .values(labels.getValue().toArray(new String[labels.getValue().size()]))
                        .build());
    }
  }

  public void updateLabelsMap(Map<String, Set<String>> labels, String labelKey, String labelValue) {
    if (!labels.containsKey(labelKey)) {
      labels.put(labelKey, new HashSet<>());
    }
    Set<String> labelValues = labels.get(labelKey);
    labelValues.add(labelValue);
  }

  private String getAwsAccountName(String awsAccountId, Map<String, String> awsCloudAccountMap) {
    if (awsCloudAccountMap.containsKey(awsAccountId)) {
      return String.format(AWS_ACCOUNT_TEMPLATE, awsCloudAccountMap.get(awsAccountId), awsAccountId);
    }
    return awsAccountId;
  }

  private QLEntityData getEntityDataPoint(FieldValueList row, Field field) {
    String value = fetchStringValue(row, field);
    return QLEntityData.builder().id(value).name(value).type(field.getName()).build();
  }

  public PreAggregateCloudOverviewDataDTO convertToPreAggregatesOverview(TableResult result,
      Map<String, PreAggregatedCostData> idToPrevOverviewAmountData, List<CloudBillingFilter> filters,
      Instant trendFilterStartTime) {
    if (preconditionsValidation(result, "convertToPreAggregatesOverview")) {
      return null;
    }
    Schema schema = result.getSchema();
    FieldList fields = schema.getFields();
    List<PreAggregateCloudOverviewDataPoint> dataPointList = new ArrayList<>();
    Double totalCost = Double.valueOf(0);
    for (FieldValueList row : result.iterateAll()) {
      PreAggregateCloudOverviewDataPointBuilder dataPointBuilder = PreAggregateCloudOverviewDataPoint.builder();
      for (Field field : fields) {
        switch (field.getName()) {
          case entityCloudProviderConst:
            dataPointBuilder.name(fetchStringValue(row, field));
            break;
          case entityConstantGcpCost:
            double cost = billingDataHelper.getRoundedDoubleValue(getNumericValue(row, field));
            totalCost += cost;
            dataPointBuilder.cost(cost);
            break;
          default:
            break;
        }
      }
      PreAggregateCloudOverviewDataPoint cloudOverviewDataPoint = dataPointBuilder.build();
      PreAggregatedCostData preAggregatedCostData =
          PreAggregatedCostData.builder()
              .cost(cloudOverviewDataPoint.getCost().doubleValue())
              .maxStartTime(getTimeStampValue(row, maxPreAggStartTimeConstant))
              .minStartTime(getTimeStampValue(row, minPreAggStartTimeConstant))
              .build();

      if (idToPrevOverviewAmountData != null
          && idToPrevOverviewAmountData.containsKey(cloudOverviewDataPoint.getName())) {
        cloudOverviewDataPoint.setTrend(getCostTrend(preAggregatedCostData,
            idToPrevOverviewAmountData.get(cloudOverviewDataPoint.getName()), filters, trendFilterStartTime));
      }
      dataPointList.add(cloudOverviewDataPoint);
    }
    return PreAggregateCloudOverviewDataDTO.builder().totalCost(totalCost).data(dataPointList).build();
  }

  public Map<String, PreAggregatedCostData> convertToIdToPrevBillingData(TableResult result) {
    Schema schema = result.getSchema();
    FieldList fields = schema.getFields();
    Map<String, PreAggregatedCostData> idToPrevBillingDataMap = new HashMap<>();
    for (FieldValueList row : result.iterateAll()) {
      processDataPrevDataAndAppendToList(fields, row, idToPrevBillingDataMap);
    }
    return idToPrevBillingDataMap;
  }

  protected void processDataPrevDataAndAppendToList(
      FieldList fields, FieldValueList row, Map<String, PreAggregatedCostData> idToPrevBillingDataMap) {
    String id = totalStringValueConstant;
    Double cost = Double.valueOf(0);
    for (Field field : fields) {
      switch (field.getName()) {
        case entityConstantRegion:
        case entityConstantAwsLinkedAccount:
        case entityConstantAwsService:
        case entityConstantAwsUsageType:
        case entityConstantAwsInstanceType:
        case entityConstantGcpProjectId:
        case entityConstantGcpProduct:
        case entityConstantGcpSku:
        case entityConstantGcpSkuId:
        case entityConstantRawTableGcpProjectId:
        case entityConstantRawTableGcpProduct:
        case entityConstantRawTableGcpSku:
        case entityConstantRawTableGcpSkuId:
        case entityConstantRawTableGcpRegion:
        case entityConstantRawTableAwsLinkedAccount:
        case entityConstantRawTableAwsUsageType:
        case entityConstantRawTableAwsInstanceType:
        case entityConstantRawTableAwsService:
          id = fetchStringValue(row, field);
          break;
        case entityConstantGcpLabelKey:
        case entityConstantAwsTagKey:
          boolean isLabel = field.getName().equals(entityConstantGcpLabelKey);
          String labelKey = fetchStringValue(row, field);
          String labelValue = fetchStringValue(row,
              Field
                  .newBuilder(
                      isLabel ? entityConstantGcpLabelValue : entityConstantAwsTagValue, StandardSQLTypeName.STRING)
                  .build());
          id = String.format(LABEL, labelKey, labelValue);
          if (labelKey.equals(nullStringValueConstant)) {
            id = isLabel ? entityNoLabelConst : entityNoTagConst;
          }
          break;
        case entityConstantAwsBlendedCost:
        case entityConstantAwsUnBlendedCost:
        case entityConstantRawTableAwsBlendedCost:
        case entityConstantRawTableAwsUnBlendedCost:
        case entityConstantGcpCost:
        case entityConstantGcpDiscount:
          cost += getNumericValue(row, field);
          break;
        default:
          break;
      }
    }
    idToPrevBillingDataMap.put(id,
        PreAggregatedCostData.builder()
            .cost(cost)
            .maxStartTime(getTimeStampValue(row, maxPreAggStartTimeConstant))
            .minStartTime(getTimeStampValue(row, minPreAggStartTimeConstant))
            .build());
  }

  public Map<String, PreAggregatedCostData> convertIdToPrevOverviewBillingData(TableResult result) {
    if (preconditionsValidation(result, "convertToPrevPreAggregatesOverview")) {
      return null;
    }
    Schema schema = result.getSchema();
    FieldList fields = schema.getFields();
    Map<String, PreAggregatedCostData> idToPrevBillingDataMap = new HashMap<>();
    Double cost = Double.valueOf(0);
    String cloudProvider = null;
    for (FieldValueList row : result.iterateAll()) {
      for (Field field : fields) {
        switch (field.getName()) {
          case entityCloudProviderConst:
            cloudProvider = fetchStringValue(row, field);
            break;
          case entityConstantGcpCost:
            cost = billingDataHelper.getRoundedDoubleValue(getNumericValue(row, field));
            break;
          default:
            break;
        }
      }
      idToPrevBillingDataMap.put(cloudProvider,
          PreAggregatedCostData.builder()
              .cost(cost)
              .maxStartTime(getTimeStampValue(row, maxPreAggStartTimeConstant))
              .minStartTime(getTimeStampValue(row, minPreAggStartTimeConstant))
              .build());
    }
    return idToPrevBillingDataMap;
  }
}
