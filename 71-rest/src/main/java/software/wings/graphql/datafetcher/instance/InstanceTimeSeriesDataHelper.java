package software.wings.graphql.datafetcher.instance;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static software.wings.graphql.datafetcher.AbstractStatsDataFetcher.MAX_RETRY;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.exception.WingsException;
import io.harness.timescaledb.TimeScaleDBService;
import lombok.extern.slf4j.Slf4j;
import software.wings.graphql.schema.type.aggregation.QLAggregateFunction;
import software.wings.graphql.schema.type.aggregation.QLDataPoint;
import software.wings.graphql.schema.type.aggregation.QLDataType;
import software.wings.graphql.schema.type.aggregation.QLNumberFilter;
import software.wings.graphql.schema.type.aggregation.QLReference;
import software.wings.graphql.schema.type.aggregation.QLStackedTimeSeriesData;
import software.wings.graphql.schema.type.aggregation.QLStackedTimeSeriesDataPoint;
import software.wings.graphql.schema.type.aggregation.QLStringFilter;
import software.wings.graphql.schema.type.aggregation.QLTimeSeriesAggregation;
import software.wings.graphql.schema.type.aggregation.QLTimeSeriesData;
import software.wings.graphql.schema.type.aggregation.QLTimeSeriesDataPoint;
import software.wings.graphql.schema.type.aggregation.QLTimeSeriesDataPoint.QLTimeSeriesDataPointBuilder;
import software.wings.graphql.schema.type.aggregation.instance.QLInstanceAggregation;
import software.wings.graphql.schema.type.aggregation.instance.QLInstanceFilter;
import software.wings.graphql.schema.type.aggregation.instance.QLInstanceFilterType;
import software.wings.graphql.utils.nameservice.NameResult;
import software.wings.graphql.utils.nameservice.NameService;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Helper class that contains all the methods required to fetch instance time series data
 * from TimeScaleDB.
 * @author rktummala on 06/27/19
 */
@Singleton
@Slf4j
public class InstanceTimeSeriesDataHelper {
  @Inject TimeScaleDBService timeScaleDBService;
  @Inject NameService nameService;
  @Inject InstanceStatsDataFetcher instanceStatsDataFetcher;

  public QLStackedTimeSeriesData getTimeSeriesAggregatedData(String accountId, QLAggregateFunction aggregateFunction,
      List<QLInstanceFilter> filters, QLTimeSeriesAggregation groupByTime, QLInstanceAggregation groupBy) {
    try {
      String aggregateOperation = instanceStatsDataFetcher.getSqlAggregateOperation(aggregateFunction);
      StringBuilder queryBuilder = new StringBuilder(150);
      queryBuilder.append("SELECT ").append(aggregateOperation).append("(INSTANCECOUNT) AS CNT, ");
      String timeQuerySegment = instanceStatsDataFetcher.getGroupByTimeQuery(groupByTime, "REPORTEDAT");
      String groupByFieldName = getSqlFieldName(groupBy);
      queryBuilder.append(timeQuerySegment)
          .append(" AS GRP_BY_TIME, ")
          .append(groupByFieldName)
          .append(" AS ENTITY_ID  FROM INSTANCE_STATS WHERE ");
      if (isNotEmpty(filters)) {
        filters.forEach(filter -> {
          queryBuilder.append(getSqlFilter(filter));
          queryBuilder.append(" AND ");
        });
      }
      queryBuilder.append("ACCOUNTID = '" + accountId + "'");

      setGroupByTime(queryBuilder, "GRP_BY_TIME");
      queryBuilder.append(", ").append(groupByFieldName).append(" ORDER BY GRP_BY_TIME , ").append(groupByFieldName);

      List<QLStackedTimeSeriesDataPoint> dataPoints = new ArrayList<>();
      executeStackedTimeSeriesQuery(accountId, queryBuilder.toString(), dataPoints, groupBy);
      return QLStackedTimeSeriesData.builder().data(dataPoints).build();
    } catch (Exception ex) {
      throw new WingsException("Error while getting time series data", ex);
    }
  }

  public QLTimeSeriesData getTimeSeriesData(String accountId, QLAggregateFunction aggregateFunction,
      List<QLInstanceFilter> filters, QLTimeSeriesAggregation groupByTime) {
    try {
      String aggregateOperation = instanceStatsDataFetcher.getSqlAggregateOperation(aggregateFunction);

      StringBuilder queryBuilder = new StringBuilder(120);
      queryBuilder.append("SELECT ").append(aggregateOperation).append("(INSTANCECOUNT) AS CNT, ");

      String timeQuerySegment = instanceStatsDataFetcher.getGroupByTimeQuery(groupByTime, "REPORTEDAT");

      queryBuilder.append(timeQuerySegment).append(" AS GRP_BY_TIME FROM INSTANCE_STATS WHERE ");
      if (isNotEmpty(filters)) {
        filters.forEach(filter -> { queryBuilder.append(getSqlFilter(filter)).append(" AND "); });
      }
      queryBuilder.append("ACCOUNTID = '").append(accountId).append('\'');

      setGroupByTime(queryBuilder, "GRP_BY_TIME");

      queryBuilder.append(" ORDER BY GRP_BY_TIME");

      List<QLTimeSeriesDataPoint> dataPoints = new ArrayList<>();
      executeTimeSeriesQuery(accountId, queryBuilder.toString(), dataPoints);
      return QLTimeSeriesData.builder().dataPoints(dataPoints).build();
    } catch (Exception ex) {
      throw new WingsException("Error while getting time series data", ex);
    }
  }

  private String getSqlFieldName(QLInstanceAggregation aggregation) {
    switch (aggregation) {
      case Application:
        return "APPID";
      case Service:
        return "SERVICEID";
      case Environment:
        return "ENVID";
      case CloudProvider:
        return "CLOUDPROVIDERID";
      case InstanceType:
        return "INSTANCETYPE";
      default:
        throw new WingsException("Unknown aggregation type" + aggregation);
    }
  }

  private void executeTimeSeriesQuery(String accountId, String query, List<QLTimeSeriesDataPoint> dataPoints) {
    int retryCount = 0;
    while (retryCount < MAX_RETRY) {
      try (Connection connection = timeScaleDBService.getDBConnection();
           Statement statement = connection.createStatement()) {
        ResultSet resultSet = statement.executeQuery(query);
        while (resultSet != null && resultSet.next()) {
          QLTimeSeriesDataPointBuilder dataPointBuilder = QLTimeSeriesDataPoint.builder();
          dataPointBuilder.data(resultSet.getInt("CNT"));
          dataPointBuilder.time(resultSet.getTimestamp("GRP_BY_TIME").getTime());
          dataPoints.add(dataPointBuilder.build());
        }
        return;
      } catch (SQLException e) {
        if (retryCount >= MAX_RETRY) {
          logger.error("Failed to execute query=[{}],accountId=[{}]", query, accountId, e);
        } else {
          logger.warn("Failed to execute query=[{}],accountId=[{}],retryCount=[{}]", query, accountId, retryCount);
        }
        retryCount++;
      }
    }
  }

  private void executeStackedTimeSeriesQuery(String accountId, String query,
      List<QLStackedTimeSeriesDataPoint> stackedTimeSeriesDataPoints, QLInstanceAggregation groupBy) {
    int retryCount = 0;
    String entityType = groupBy.name();
    Multimap<String, QLReference> idRefMap = HashMultimap.create();
    while (retryCount < MAX_RETRY) {
      try (Connection connection = timeScaleDBService.getDBConnection();
           Statement statement = connection.createStatement()) {
        ResultSet resultSet = statement.executeQuery(query);
        long previousTime = -1L;
        List<QLDataPoint> dataPointList = new ArrayList<>();
        while (resultSet != null && resultSet.next()) {
          long timestamp = resultSet.getTimestamp("GRP_BY_TIME").getTime();
          if (previousTime != timestamp) {
            previousTime = timestamp;
            dataPointList = new ArrayList<>();
            QLStackedTimeSeriesDataPoint stackedDataPoint = new QLStackedTimeSeriesDataPoint();
            stackedDataPoint.setTime(timestamp);
            stackedDataPoint.setValues(dataPointList);
            stackedTimeSeriesDataPoints.add(stackedDataPoint);
          }

          QLReference qlReference = new QLReference();
          String entityId = resultSet.getString("ENTITY_ID");
          qlReference.setId(entityId);
          idRefMap.put(entityId, qlReference);
          qlReference.setType(entityType);
          QLDataPoint dataPoint = new QLDataPoint();
          dataPoint.setKey(qlReference);
          dataPoint.setValue(resultSet.getInt("CNT"));
          dataPointList.add(dataPoint);
        }

        NameResult nameResult = nameService.getNames(idRefMap.keySet(), entityType);
        idRefMap.keySet().forEach(key -> {
          String name = instanceStatsDataFetcher.getEntityName(nameResult, key, entityType);
          idRefMap.get(key).forEach(ref -> ref.setName(name));
        });

        return;
      } catch (SQLException e) {
        if (retryCount >= MAX_RETRY) {
          logger.error("Failed to execute query=[{}],accountId=[{}]", query, accountId, e);
        } else {
          logger.warn("Failed to execute query=[{}],accountId=[{}],retryCount=[{}]", query, accountId, retryCount);
        }
        retryCount++;
      }
    }
  }

  private void setGroupByTime(StringBuilder queryBuilder, String timeQuerySegment) {
    queryBuilder.append(" GROUP BY ");
    queryBuilder.append(timeQuerySegment);
  }

  private String getSqlFilter(QLInstanceFilter filter) {
    QLInstanceFilterType type = filter.getType();
    QLDataType dataType = type.getDataType();
    String dbFieldName = type.getSqlDbFieldName();
    StringBuilder queryBuilder = new StringBuilder(dbFieldName);
    switch (dataType) {
      case NUMBER:
        QLNumberFilter numberFilter = filter.getNumberFilter();
        addNumberFilter(queryBuilder, numberFilter);
        break;
      case STRING:
        QLStringFilter stringFilter = filter.getStringFilter();
        addStringFilter(queryBuilder, stringFilter);
        break;
      default:
        throw new WingsException("Unknown instance filter type " + dataType);
    }
    return queryBuilder.toString();
  }

  private void addNumberFilter(StringBuilder queryBuilder, QLNumberFilter filter) {
    switch (filter.getOperator()) {
      case EQUALS:
        queryBuilder.append(" = ");
        queryBuilder.append(filter.getValues()[0]);
        break;
      case NOT_EQUALS:
        queryBuilder.append(" != ");
        queryBuilder.append(filter.getValues()[0]);
        break;
      case IN:
        queryBuilder.append(" IN (");
        instanceStatsDataFetcher.generateSqlInQuery(queryBuilder, filter.getValues());
        break;
      case NOT_IN:
        queryBuilder.append(" NOT IN (");
        instanceStatsDataFetcher.generateSqlInQuery(queryBuilder, filter.getValues());
        break;
      case LESS_THAN:
        queryBuilder.append(" < ");
        queryBuilder.append(filter.getValues()[0]);
        break;
      case LESS_THAN_OR_EQUALS:
        queryBuilder.append(" <= ");
        queryBuilder.append(filter.getValues()[0]);
        break;
      case GREATER_THAN:
        queryBuilder.append(" > ");
        queryBuilder.append(filter.getValues()[0]);
        break;
      case GREATER_THAN_OR_EQUALS:
        queryBuilder.append(" >= ");
        queryBuilder.append(filter.getValues()[0]);
        break;
      default:
        throw new RuntimeException("Number operator not supported" + filter.getOperator());
    }
  }

  private void addStringFilter(StringBuilder queryBuilder, QLStringFilter filter) {
    switch (filter.getOperator()) {
      case EQUALS:
        queryBuilder.append(" = '");
        queryBuilder.append(filter.getValues()[0]);
        queryBuilder.append('\'');
        break;
      case NOT_EQUALS:
        queryBuilder.append(" != '");
        queryBuilder.append(filter.getValues()[0]);
        queryBuilder.append('\'');
        break;
      case IN:
        queryBuilder.append(" IN (");
        instanceStatsDataFetcher.generateSqlInQuery(queryBuilder, filter.getValues());
        queryBuilder.append(')');
        break;
      case NOT_IN:
        queryBuilder.append(" NOT IN (");
        instanceStatsDataFetcher.generateSqlInQuery(queryBuilder, filter.getValues());
        queryBuilder.append(')');
        break;
      default:
        throw new WingsException("String operator not supported" + filter.getOperator());
    }
  }
}
