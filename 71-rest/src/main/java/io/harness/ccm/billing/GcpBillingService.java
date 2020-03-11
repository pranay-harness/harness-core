package io.harness.ccm.billing;

import com.google.cloud.Timestamp;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.TableResult;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

import com.healthmarketscience.sqlbuilder.Condition;
import com.healthmarketscience.sqlbuilder.SqlObject;
import com.healthmarketscience.sqlbuilder.dbspec.basic.DbColumn;
import io.harness.ccm.billing.bigquery.BigQuerySQL;
import io.harness.ccm.billing.bigquery.BigQueryService;
import io.harness.ccm.billing.bigquery.ConstExpression;
import io.harness.ccm.billing.bigquery.TruncExpression;
import lombok.extern.slf4j.Slf4j;
import software.wings.graphql.schema.type.aggregation.QLBillingDataPoint;
import software.wings.graphql.schema.type.aggregation.QLReference;

import java.math.BigDecimal;
import java.util.ArrayList;
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

  public GcpBillingTimeSeriesStatsDTO getGcpBillingTimeSeriesStats(
      SqlObject aggregateFunction, List<Object> groupbyObjects, List<Condition> conditions) {
    Preconditions.checkNotNull(
        groupbyObjects, "Queries for GCP Billing time series stats require at lease one group-by.");
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

    Optional<Object> entityGroupBy = groupbyObjects.stream().filter(g -> g instanceof DbColumn).findFirst();
    if (!entityGroupBy.isPresent()) { // default: "total" as total
      sqlGroubyObjects.add(new ConstExpression("total", "total"));
    } else {
      sqlGroubyObjects.add(entityGroupBy.get());
    }

    for (int i = 0; i < sqlGroubyObjects.size(); i++) {
      selectObjects.add(sqlGroubyObjects.get(i));
      if (sqlGroubyObjects.get(i) instanceof TruncExpression) {
        sqlGroubyObjects.set(i, ((TruncExpression) sqlGroubyObjects.get(i)).getAlias());
      }
      if (sqlGroubyObjects.get(i) instanceof ConstExpression) {
        sqlGroubyObjects.set(i, ((ConstExpression) sqlGroubyObjects.get(i)).getAlias());
      }
      sortObjects.add(sqlGroubyObjects.get(i));
    }

    if (aggregateFunction != null) {
      selectObjects.add(aggregateFunction);
    }

    BigQuerySQL bigQuerySql = BigQuerySQL.builder()
                                  .selectColumns(selectObjects)
                                  .groupbyObjects(sqlGroubyObjects)
                                  .sortObjects(sortObjects)
                                  .conditions(conditions)
                                  .build();
    String query = bigQuerySql.getQuery().validate().toString();
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
