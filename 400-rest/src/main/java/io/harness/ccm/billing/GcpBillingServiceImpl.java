/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ccm.billing;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.ccm.bigQuery.BigQueryService;
import io.harness.ccm.billing.GcpBillingEntityDataPoints.GcpBillingEntityDataPointsBuilder;
import io.harness.ccm.billing.bigquery.AliasExpression;
import io.harness.ccm.billing.bigquery.BigQuerySQL;
import io.harness.ccm.billing.bigquery.ConstExpression;
import io.harness.ccm.billing.bigquery.TruncExpression;

import software.wings.graphql.schema.type.aggregation.QLBillingDataPoint;
import software.wings.graphql.schema.type.aggregation.QLReference;

import com.google.cloud.Timestamp;
import com.google.cloud.bigquery.Field;
import com.google.cloud.bigquery.FieldList;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.Schema;
import com.google.cloud.bigquery.TableResult;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.healthmarketscience.sqlbuilder.AliasedObject;
import com.healthmarketscience.sqlbuilder.Condition;
import com.healthmarketscience.sqlbuilder.FunctionCall;
import com.healthmarketscience.sqlbuilder.SqlObject;
import com.healthmarketscience.sqlbuilder.dbspec.basic.DbColumn;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.stat.regression.SimpleRegression;

@Slf4j
@Singleton
@OwnedBy(CE)
@TargetModule(HarnessModule._375_CE_GRAPHQL)
public class GcpBillingServiceImpl implements GcpBillingService {
  private BigQueryService bigQueryService;

  @Inject
  GcpBillingServiceImpl(BigQueryService bigQueryService) {
    this.bigQueryService = bigQueryService;
  }

  @Override
  public BigDecimal getTotalCost(List<Condition> conditions) {
    List<Object> selectObjects = new ArrayList<>();
    selectObjects.add(
        AliasedObject.toAliasedObject(FunctionCall.sum().addColumnParams(RawBillingTableSchema.cost), "cost_sum"));

    BigQuerySQL bigQuerySql = BigQuerySQL.builder().selectColumns(selectObjects).conditions(conditions).build();
    String query = bigQuerySql.getQuery().validate().toString();
    QueryJobConfiguration queryConfig = QueryJobConfiguration.newBuilder(query).build();
    TableResult result = null;
    try {
      result = bigQueryService.get().query(queryConfig);
      return toTotalCost(result);
    } catch (InterruptedException e) {
      log.error("Failed to get GCP billing Entity stats.", e);
      Thread.currentThread().interrupt();
      return BigDecimal.ZERO;
    }
  }

  private BigDecimal toTotalCost(TableResult result) {
    Preconditions.checkNotNull(result);
    Preconditions.checkArgument(result.getTotalRows() == 1, "Unexpected result from this query.");
    Preconditions.checkArgument(result.getSchema().getFields().size() == 1, "Unexpected result from this query.");
    String fieldName = result.getSchema().getFields().get(0).getName();
    BigDecimal totalCost = null;
    for (FieldValueList row : result.iterateAll()) {
      totalCost = row.get(fieldName).getNumericValue();
    }
    return totalCost;
  }

  @Override
  public BigDecimal getCostTrend(SimpleRegression regression, Date startDate, Date endDate) {
    Calendar c = Calendar.getInstance();
    c.setTime(endDate);

    Date currMonthEndDate = endDate;
    c.add(Calendar.MONTH, -1);
    Date currMonthStartDate = c.getTime();
    BigDecimal currMonthCost = getCostEstimate(regression, currMonthStartDate, currMonthEndDate);
    c.add(Calendar.DATE, -1);
    Date prevMonthEndDate = c.getTime();
    c.add(Calendar.MONTH, -1);
    Date prevMonthStartDate = c.getTime();
    BigDecimal prevMonthCost = getCostEstimate(regression, prevMonthStartDate, prevMonthEndDate);
    return currMonthCost.subtract(prevMonthCost)
        .divide(prevMonthCost, 2, RoundingMode.HALF_UP)
        .multiply(new BigDecimal(100));
  }

  @Override
  public BigDecimal getCostEstimate(SimpleRegression regression, Date startDate, Date endDate) {
    BigDecimal startDateCost = BigDecimal.valueOf(regression.predict(toDouble(startDate)));
    BigDecimal endDateCost = BigDecimal.valueOf(regression.predict(toDouble(endDate)));
    return startDateCost.add(endDateCost)
        .multiply(BigDecimal.valueOf(toDouble(endDate) - toDouble(startDate)))
        .divide(new BigDecimal(2));
  }

  @Override
  public SimpleRegression getSimpleRegression(List<Condition> conditions, Date startDate, Date endDate) {
    // todo: specify a different time range from the filter
    // e.g. 30 days before the endDate
    double[][] observations = getGcpBillingObservations(conditions);
    SimpleRegression regression = new SimpleRegression(false);
    regression.addData(observations);
    return regression;
  }

  private double[][] getGcpBillingObservations(List<Condition> conditions) {
    List<Object> selectObjects = new ArrayList<>();
    Object timeTruncGroupBy =
        new TruncExpression(RawBillingTableSchema.startTime, TruncExpression.DatePart.DAY, "start_time_trunc");
    selectObjects.add(timeTruncGroupBy);
    selectObjects.add(
        AliasedObject.toAliasedObject(FunctionCall.sum().addColumnParams(RawBillingTableSchema.cost), "cost_sum"));

    BigQuerySQL bigQuerySql = BigQuerySQL.builder()
                                  .selectColumns(selectObjects)
                                  .groupbyObjects(Arrays.asList(((TruncExpression) timeTruncGroupBy).getAlias()))
                                  .sortObjects(Arrays.asList(((TruncExpression) timeTruncGroupBy).getAlias()))
                                  .conditions(conditions)
                                  .build();
    String query = bigQuerySql.getQuery().validate().toString();
    QueryJobConfiguration queryConfig = QueryJobConfiguration.newBuilder(query).build();
    TableResult result = null;
    try {
      result = bigQueryService.get().query(queryConfig);
    } catch (InterruptedException e) {
      log.error("Failed to get GCP billing Entity stats.", e);
      Thread.currentThread().interrupt();
      return null;
    }
    return toGcpBillingObservations(result);
  }

  private double[][] toGcpBillingObservations(TableResult result) {
    Preconditions.checkNotNull(result);
    List<List<Double>> arrayLists = new ArrayList<>();
    for (FieldValueList row : result.iterateAll()) {
      Timestamp startTimeTruncTimestamp = Timestamp.ofTimeMicroseconds(row.get("start_time_trunc").getTimestampValue());
      double costSum = row.get("cost_sum").getNumericValue().doubleValue();
      LocalDate localDate = Instant.ofEpochMilli(startTimeTruncTimestamp.getSeconds() * 1000)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate();
      arrayLists.add(Arrays.asList((double) localDate.toEpochDay(), costSum));
    }
    return arrayLists.stream().map(a -> a.stream().mapToDouble(d -> d).toArray()).toArray(double[][] ::new);
  }

  private static double toDouble(Date date) {
    LocalDate localDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    return localDate.toEpochDay();
  }

  @Override
  public GcpBillingEntityStatsDTO getGcpBillingEntityStats(
      List<SqlObject> aggregateFunction, List<Object> groupByObjects, List<Condition> conditions) {
    String query = getQuery(aggregateFunction, groupByObjects, conditions);
    QueryJobConfiguration queryConfig = QueryJobConfiguration.newBuilder(query).build();
    TableResult result = null;
    try {
      result = bigQueryService.get().query(queryConfig);
    } catch (InterruptedException e) {
      log.error("Failed to get GCP billing Entity stats.", e);
      Thread.currentThread().interrupt();
      return null;
    }
    return toGcpBillingEntityStats(result);
  }

  private GcpBillingEntityStatsDTO toGcpBillingEntityStats(TableResult result) {
    Preconditions.checkNotNull(result);
    if (result.getTotalRows() == 0) {
      log.warn("No result from this query");
      return null;
    }

    Schema schema = result.getSchema();
    FieldList fields = schema.getFields();
    List<GcpBillingEntityDataPoints> dataPoints = new ArrayList<>();
    for (FieldValueList row : result.iterateAll()) {
      GcpBillingEntityDataPointsBuilder dataPointsBuilder = GcpBillingEntityDataPoints.builder();
      GcpBillingEntityDataPoints dataPoint = null;
      String productName = null;
      String usageAmountInPricingUnits = null;
      String usagePricingUnit = null;
      for (Field field : fields) {
        // TODO : Move all these constants into a common Class
        switch (field.getName()) {
          case "sku_description":
          case "project_name":
            dataPointsBuilder.name((String) row.get(field.getName()).getValue());
            break;
          case "sku_id":
          case "project_id":
            dataPointsBuilder.id((String) row.get(field.getName()).getValue());
            break;
          case "service_description":
            productName = (String) row.get(field.getName()).getValue();
            break;
          case "usage_pricing_unit":
            usagePricingUnit = (String) row.get(field.getName()).getValue();
            break;
          case "usage_amount_in_pricing_units":
            usageAmountInPricingUnits = (String) row.get(field.getName()).getValue();
            break;
          case "sum_cost":
            dataPointsBuilder.totalCost(row.get(field.getName()).getDoubleValue());
            break;
          case "sum_discount":
            dataPointsBuilder.discounts(row.get(field.getName()).getDoubleValue());
            break;
          case "location_region":
            dataPointsBuilder.region((String) row.get(field.getName()).getValue());
            break;
          case "project_ancestry_numbers":
            dataPointsBuilder.projectNumber((String) row.get(field.getName()).getValue());
            break;
          default:
            break;
        }
      }
      dataPoint = dataPointsBuilder.build();
      // Validate and add data based on view
      if (productName != null) {
        // Product GroupBy
        if (dataPoint.getId() == null) {
          dataPoint.setId(productName);
          dataPoint.setName(productName);
        } else {
          // SKU GroupBy
          dataPoint.setProductType(productName);
          dataPoint.setUsage(usageAmountInPricingUnits + " " + usagePricingUnit);
        }
      }
      dataPoints.add(dataPoint);
      // TODO: Added this break to restrict Processing Huge amounts of Data
      if (dataPoints.size() == 1000) {
        break;
      }
    }
    return GcpBillingEntityStatsDTO.builder().data(dataPoints).build();
  }

  public GcpBillingTimeSeriesStatsDTO getGcpBillingTimeSeriesStats(
      SqlObject aggregateFunction, List<Object> groupbyObjects, List<Condition> conditions) {
    String query = getQuery(Collections.singletonList(aggregateFunction), groupbyObjects, conditions);
    QueryJobConfiguration queryConfig = QueryJobConfiguration.newBuilder(query).build();
    TableResult result = null;
    try {
      result = bigQueryService.get().query(queryConfig);
    } catch (InterruptedException e) {
      log.error("Failed to get GCP billing time series stats.", e);
      Thread.currentThread().interrupt();
      return null;
    }
    return toGcpBillingTimeSeriesStats(result);
  }

  private static GcpBillingTimeSeriesStatsDTO toGcpBillingTimeSeriesStats(TableResult result) {
    Preconditions.checkNotNull(result);
    if (result.getTotalRows() == 0) {
      log.warn("No result from this query");
      return null;
    }
    Map<Timestamp, List<QLBillingDataPoint>> timeSeriesDataPointsMap = new HashMap();
    for (FieldValueList row : result.iterateAll()) {
      // todo do not hard code "start_time_trunc" here, read it from schema
      Timestamp startTimeTruncTimestamp = Timestamp.ofTimeMicroseconds(row.get("start_time_trunc").getTimestampValue());
      // todo explain why it's 1 here. This is because the sql selection follows a strict order
      String entityNameString = Optional.ofNullable(row.get(1).getValue()).orElse("").toString();
      BigDecimal sumCost = row.get("sum_cost").getNumericValue();
      QLBillingDataPoint dataPoint =
          QLBillingDataPoint.builder().key(QLReference.builder().name(entityNameString).build()).value(sumCost).build();

      if (timeSeriesDataPointsMap.containsKey(startTimeTruncTimestamp)) {
        List<QLBillingDataPoint> dataPoints = timeSeriesDataPointsMap.get(startTimeTruncTimestamp);
        dataPoints.add(dataPoint);
        timeSeriesDataPointsMap.put(startTimeTruncTimestamp, dataPoints);
      } else {
        List<QLBillingDataPoint> dataPoints = new ArrayList<>();
        dataPoints.add(dataPoint);
        timeSeriesDataPointsMap.put(startTimeTruncTimestamp, dataPoints);
      }
    }

    List<TimeSeriesDataPoints> timeSeriesDataPointsList = timeSeriesDataPointsMap.entrySet()
                                                              .stream()
                                                              .map(e
                                                                  -> TimeSeriesDataPoints.builder()
                                                                         .time(e.getKey().toSqlTimestamp().getTime())
                                                                         .values(e.getValue())
                                                                         .build())
                                                              .collect(Collectors.toList());

    return GcpBillingTimeSeriesStatsDTO.builder().stats(timeSeriesDataPointsList).build();
  }

  private String getQuery(List<SqlObject> aggregateFunction, List<Object> groupbyObjects, List<Condition> conditions) {
    Preconditions.checkNotNull(groupbyObjects, "Queries for GCP Billing stats require at lease one group-by.");
    List<Object> selectObjects = new ArrayList<>();
    List<Object> sqlGroubyObjects = new ArrayList<>();
    List<Object> sortObjects = new ArrayList<>();

    Optional<Object> timeTruncGroupBy = groupbyObjects.stream().filter(g -> g instanceof TruncExpression).findFirst();
    if (!timeTruncGroupBy.isPresent()) { // set default timeTruncGroupby
      timeTruncGroupBy = Optional.of(
          new TruncExpression(RawBillingTableSchema.startTime, TruncExpression.DatePart.DAY, "start_time_trunc"));
    }
    // ensure timeTruncGroupby come before entityGroupBy
    sqlGroubyObjects.add(timeTruncGroupBy.get());

    List<Object> sqlGroupByObjectsTempList = new ArrayList<>();
    groupbyObjects.stream()
        .filter(g -> g instanceof DbColumn)
        .forEach(entityGroupBy -> sqlGroupByObjectsTempList.add(entityGroupBy));
    if (sqlGroupByObjectsTempList.isEmpty()) { // default: "total" as total
      sqlGroubyObjects.add(new ConstExpression("total", "total"));
    } else {
      sqlGroubyObjects.addAll(sqlGroupByObjectsTempList);
    }

    for (int i = 0; i < sqlGroubyObjects.size(); i++) {
      if (sqlGroubyObjects.get(i) instanceof TruncExpression) {
        selectObjects.add(sqlGroubyObjects.get(i));
        sqlGroubyObjects.set(i, ((TruncExpression) sqlGroubyObjects.get(i)).getAlias());
      }
      if (sqlGroubyObjects.get(i) instanceof ConstExpression) {
        selectObjects.add(sqlGroubyObjects.get(i));
        sqlGroubyObjects.set(i, ((ConstExpression) sqlGroubyObjects.get(i)).getAlias());
      }
      if (sqlGroubyObjects.get(i) instanceof DbColumn) {
        String columnNameSQL = ((DbColumn) sqlGroubyObjects.get(i)).getColumnNameSQL();
        String alias = columnNameSQL.replace(".", "_");
        selectObjects.add(new AliasExpression(columnNameSQL, alias));
      }
      // TODO: (Make this more Fool Proof)
      sortObjects.add(sqlGroubyObjects.get(i));
    }

    if (aggregateFunction != null) {
      selectObjects.addAll(aggregateFunction);
    }

    BigQuerySQL bigQuerySql = BigQuerySQL.builder()
                                  .selectColumns(selectObjects)
                                  .groupbyObjects(sqlGroubyObjects)
                                  .sortObjects(sortObjects)
                                  .conditions(conditions)
                                  .build();
    return bigQuerySql.getQuery().validate().toString();
  }
}
