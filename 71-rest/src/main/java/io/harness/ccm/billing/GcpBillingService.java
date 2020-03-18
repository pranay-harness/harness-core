package io.harness.ccm.billing;

import com.google.cloud.Timestamp;
import com.google.cloud.bigquery.Field;
import com.google.cloud.bigquery.FieldList;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.Schema;
import com.google.cloud.bigquery.TableResult;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

import com.healthmarketscience.sqlbuilder.Condition;
import com.healthmarketscience.sqlbuilder.SqlObject;
import com.healthmarketscience.sqlbuilder.dbspec.basic.DbColumn;
import io.harness.ccm.billing.GcpBillingEntityDataPoints.GcpBillingEntityDataPointsBuilder;
import io.harness.ccm.billing.bigquery.AliasExpression;
import io.harness.ccm.billing.bigquery.BigQuerySQL;
import io.harness.ccm.billing.bigquery.BigQueryService;
import io.harness.ccm.billing.bigquery.ConstExpression;
import io.harness.ccm.billing.bigquery.TruncExpression;
import lombok.extern.slf4j.Slf4j;
import software.wings.graphql.schema.type.aggregation.QLBillingDataPoint;
import software.wings.graphql.schema.type.aggregation.QLReference;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
public class GcpBillingService {
  private BigQueryService bigQueryService;

  @Inject
  GcpBillingService(BigQueryService bigQueryService) {
    this.bigQueryService = bigQueryService;
  }

  public GcpBillingEntityStatsDTO getGcpBillingEntityStats(
      List<SqlObject> aggregateFunction, List<Object> groupByObjects, List<Condition> conditions) {
    String query = getQuery(aggregateFunction, groupByObjects, conditions);
    QueryJobConfiguration queryConfig = QueryJobConfiguration.newBuilder(query).build();
    TableResult result = null;
    try {
      result = bigQueryService.get().query(queryConfig);
    } catch (InterruptedException e) {
      logger.error("Failed to get GCP billing Entity stats.", e);
      Thread.currentThread().interrupt();
      return null;
    }
    return toGcpBillingEntityStats(result);
  }

  private GcpBillingEntityStatsDTO toGcpBillingEntityStats(TableResult result) {
    Preconditions.checkNotNull(result);
    if (result.getTotalRows() == 0) {
      logger.warn("No result from this query");
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
      logger.error("Failed to get GCP billing time series stats.", e);
      Thread.currentThread().interrupt();
      return null;
    }
    return toGcpBillingTimeSeriesStats(result);
  }

  private String getQuery(List<SqlObject> aggregateFunction, List<Object> groupbyObjects, List<Condition> conditions) {
    Preconditions.checkNotNull(groupbyObjects, "Queries for GCP Billing stats require at lease one group-by.");
    List<Object> selectObjects = new ArrayList<>();
    List<Object> sqlGroubyObjects = new ArrayList<>();
    List<Object> sortObjects = new ArrayList<>();

    Optional<Object> timeTruncGroupBy = groupbyObjects.stream().filter(g -> g instanceof TruncExpression).findFirst();
    if (!timeTruncGroupBy.isPresent()) { // set default timeTruncGroupby
      timeTruncGroupBy = Optional.of(
          new TruncExpression(GcpBillingTableSchema.usageStartTime, TruncExpression.DatePart.DAY, "start_time_trunc"));
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

  private static GcpBillingTimeSeriesStatsDTO toGcpBillingTimeSeriesStats(TableResult result) {
    Preconditions.checkNotNull(result);
    if (result.getTotalRows() == 0) {
      logger.warn("No result from this query");
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
}
