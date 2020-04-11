package io.harness.ccm.billing.preaggregated;

import com.google.cloud.Timestamp;
import com.google.cloud.bigquery.Field;
import com.google.cloud.bigquery.FieldList;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.Schema;
import com.google.cloud.bigquery.TableResult;
import com.google.common.base.Preconditions;
import com.google.inject.Singleton;

import com.healthmarketscience.sqlbuilder.Condition;
import com.healthmarketscience.sqlbuilder.SqlObject;
import com.healthmarketscience.sqlbuilder.dbspec.basic.DbColumn;
import io.harness.ccm.billing.TimeSeriesDataPoints;
import io.harness.ccm.billing.bigquery.AliasExpression;
import io.harness.ccm.billing.bigquery.BigQuerySQL;
import io.harness.ccm.billing.bigquery.ConstExpression;
import io.harness.ccm.billing.bigquery.TruncExpression;
import lombok.extern.slf4j.Slf4j;
import software.wings.graphql.schema.type.aggregation.QLBillingDataPoint;
import software.wings.graphql.schema.type.aggregation.QLBillingDataPoint.QLBillingDataPointBuilder;
import software.wings.graphql.schema.type.aggregation.QLReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Singleton
public class PreAggregatedBillingDataHelper {
  public PreAggregateBillingTimeSeriesStatsDTO convertToPreAggregatesTimeSeriesData(TableResult result) {
    Preconditions.checkNotNull(result);
    if (result.getTotalRows() == 0) {
      logger.warn("No result from PreAggregate billing time series stats query");
      return null;
    }

    Schema schema = result.getSchema();
    FieldList fields = schema.getFields();

    Map<Timestamp, List<QLBillingDataPoint>> timeSeriesDataPointsMap = new HashMap();
    for (FieldValueList row : result.iterateAll()) {
      QLBillingDataPointBuilder billingDataPointBuilder = QLBillingDataPoint.builder();
      Timestamp startTimeTruncatedTimestamp = null;
      for (Field field : fields) {
        switch (field.getType().getStandardType()) {
          case TIMESTAMP:
            startTimeTruncatedTimestamp = Timestamp.ofTimeMicroseconds(row.get(field.getName()).getTimestampValue());
            break;
          case STRING:
            billingDataPointBuilder.key(
                QLReference.builder().name(fetchStringValue(row, field)).type(field.getName()).build());
            break;
          case FLOAT64:
            billingDataPointBuilder.value(row.get(field.getName()).getNumericValue());
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

  public String getQuery(List<SqlObject> aggregateFunction, List<Object> groupByObjects, List<Condition> conditions) {
    Preconditions.checkNotNull(groupByObjects, "Queries to Pre-Aggregated Tables need at least one groupBy");
    List<Object> selectObjects = new ArrayList<>();
    List<Object> sqlGroupByObjects = new ArrayList<>();
    List<Object> sortObjects = new ArrayList<>();

    groupByObjects.stream().filter(g -> g instanceof DbColumn).forEach(sqlGroupByObjects::add);

    processAndAddTimeTruncatedGroupBy(groupByObjects, sqlGroupByObjects);
    processAndAddGroupBy(sqlGroupByObjects, selectObjects);

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
    if (!timeTruncatedGroupBy.isPresent()) {
      timeTruncatedGroupBy = Optional.of(
          new TruncExpression(PreAggregatedTableSchema.startTime, TruncExpression.DatePart.DAY, "start_time_trunc"));
    }
    sqlGroupByObjects.add(timeTruncatedGroupBy.get());
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
}
