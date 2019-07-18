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
import software.wings.graphql.schema.type.aggregation.QLDataPoint;
import software.wings.graphql.schema.type.aggregation.QLIdFilter;
import software.wings.graphql.schema.type.aggregation.QLReference;
import software.wings.graphql.schema.type.aggregation.QLStackedTimeSeriesData;
import software.wings.graphql.schema.type.aggregation.QLStackedTimeSeriesDataPoint;
import software.wings.graphql.schema.type.aggregation.QLTimeFilter;
import software.wings.graphql.schema.type.aggregation.QLTimeSeriesAggregation;
import software.wings.graphql.schema.type.aggregation.QLTimeSeriesData;
import software.wings.graphql.schema.type.aggregation.QLTimeSeriesDataPoint;
import software.wings.graphql.schema.type.aggregation.QLTimeSeriesDataPoint.QLTimeSeriesDataPointBuilder;
import software.wings.graphql.schema.type.aggregation.instance.QLInstanceAggregation;
import software.wings.graphql.schema.type.aggregation.instance.QLInstanceFilter;
import software.wings.graphql.utils.nameservice.NameResult;
import software.wings.graphql.utils.nameservice.NameService;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
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

  public QLStackedTimeSeriesData getTimeSeriesAggregatedData(String accountId, List<QLInstanceFilter> filters,
      QLTimeSeriesAggregation groupByTime, QLInstanceAggregation groupBy) {
    //    select avg(sum_value), time_bucket('1 hours',reportedat) AS grp_by_time, entity_id
    //    from (select envId as entity_id, reportedat, sum(INSTANCECOUNT) as sum_value from INSTANCE_STATS group by
    //    entity_id, reportedat) INSTANCE_STATS group by entity_id, grp_by_time order by grp_by_time

    try {
      StringBuilder queryBuilder = new StringBuilder(275);
      queryBuilder.append("SELECT AVG(SUM_VALUE) AS CNT, ");
      String timeQuerySegment = instanceStatsDataFetcher.getGroupByTimeQuery(groupByTime, "REPORTEDAT");
      String groupByFieldName = getSqlFieldName(groupBy);
      queryBuilder.append(timeQuerySegment)
          .append(" AS GRP_BY_TIME, ENTITY_ID FROM (SELECT ")
          .append(groupByFieldName)
          .append(" AS ENTITY_ID, REPORTEDAT, SUM(INSTANCECOUNT) AS SUM_VALUE FROM INSTANCE_STATS WHERE ");
      if (isNotEmpty(filters)) {
        filters.forEach(filter -> {
          queryBuilder.append(getSqlFilter(filter));
          queryBuilder.append(" AND ");
        });
      }
      queryBuilder.append(" ACCOUNTID = '")
          .append(accountId)
          .append(
              "' GROUP BY ENTITY_ID, REPORTEDAT) INSTANCE_STATS GROUP BY ENTITY_ID, GRP_BY_TIME ORDER BY GRP_BY_TIME");

      List<QLStackedTimeSeriesDataPoint> dataPoints = new ArrayList<>();
      executeStackedTimeSeriesQuery(accountId, queryBuilder.toString(), dataPoints, groupBy);
      return QLStackedTimeSeriesData.builder().data(dataPoints).build();
    } catch (Exception ex) {
      throw new WingsException("Error while getting time series data", ex);
    }
  }

  public QLTimeSeriesData getTimeSeriesData(
      String accountId, List<QLInstanceFilter> filters, QLTimeSeriesAggregation groupByTime) {
    //    select avg(sum_value), time_bucket('1 hours',reportedat) AS grp_by_time
    //    from (select reportedat, sum(INSTANCECOUNT) as sum_value from INSTANCE_STATS group by reportedat)
    //    INSTANCE_STATS group by grp_by_time order by grp_by_time

    try {
      StringBuilder queryBuilder = new StringBuilder(250);
      queryBuilder.append("SELECT AVG(SUM_VALUE) AS CNT, ");
      String timeQuerySegment = instanceStatsDataFetcher.getGroupByTimeQuery(groupByTime, "REPORTEDAT");
      queryBuilder.append(timeQuerySegment)
          .append(
              " AS GRP_BY_TIME FROM (SELECT REPORTEDAT, SUM(INSTANCECOUNT) AS SUM_VALUE FROM INSTANCE_STATS WHERE ");
      if (isNotEmpty(filters)) {
        filters.forEach(filter -> {
          queryBuilder.append(getSqlFilter(filter));
          queryBuilder.append(" AND ");
        });
      }
      queryBuilder.append(" ACCOUNTID = '")
          .append(accountId)
          .append("' GROUP BY REPORTEDAT) INSTANCE_STATS GROUP BY GRP_BY_TIME ORDER BY GRP_BY_TIME");

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
          dataPointBuilder.value(resultSet.getInt("CNT"));
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

  private String getSqlFilter(QLInstanceFilter filter) {
    StringBuilder queryBuilder = new StringBuilder(56);
    boolean multipleCriteria = false;
    if (filter.getApplication() != null) {
      multipleCriteria = setMultipleCriteria(multipleCriteria, queryBuilder);
      queryBuilder.append(" APPID ");
      addIdQuery(queryBuilder, filter.getApplication());
    }

    if (filter.getCloudProvider() != null) {
      queryBuilder.append(" CLOUDPROVIDERID ");
      multipleCriteria = setMultipleCriteria(multipleCriteria, queryBuilder);
      addIdQuery(queryBuilder, filter.getCloudProvider());
    }

    if (filter.getEnvironment() != null) {
      queryBuilder.append(" ENVID ");
      multipleCriteria = setMultipleCriteria(multipleCriteria, queryBuilder);
      addIdQuery(queryBuilder, filter.getEnvironment());
    }

    if (filter.getService() != null) {
      queryBuilder.append(" SERVICEID ");
      multipleCriteria = setMultipleCriteria(multipleCriteria, queryBuilder);
      addIdQuery(queryBuilder, filter.getService());
    }

    if (filter.getCreatedAt() != null) {
      queryBuilder.append(" REPORTEDAT ");
      multipleCriteria = setMultipleCriteria(multipleCriteria, queryBuilder);
      addTimeQuery(queryBuilder, filter.getCreatedAt());
    }

    if (filter.getInstanceType() != null) {
      setMultipleCriteria(multipleCriteria, queryBuilder);
      queryBuilder.append(" INSTANCETYPE = '");
      queryBuilder.append(filter.getInstanceType().name());
      queryBuilder.append('\'');
    }

    return queryBuilder.toString();
  }

  private boolean setMultipleCriteria(boolean multipleCriteria, StringBuilder queryBuilder) {
    if (multipleCriteria) {
      queryBuilder.append(" AND ");
    } else {
      multipleCriteria = true;
    }
    return multipleCriteria;
  }

  private void addTimeQuery(StringBuilder queryBuilder, QLTimeFilter filter) {
    switch (filter.getOperator()) {
      case EQUALS:
        queryBuilder.append(" = timestamp '");
        queryBuilder.append(new Timestamp((Long) filter.getValue()));
        queryBuilder.append('\'');
        break;
      case BEFORE:
        queryBuilder.append(" <= timestamp '");
        queryBuilder.append(new Timestamp((Long) filter.getValue()));
        queryBuilder.append('\'');
        break;
      case AFTER:
        queryBuilder.append(" >= timestamp '");
        queryBuilder.append(new Timestamp((Long) filter.getValue()));
        queryBuilder.append('\'');
        break;
      default:
        throw new RuntimeException("Number operator not supported" + filter.getOperator());
    }
  }

  private void addIdQuery(StringBuilder queryBuilder, QLIdFilter filter) {
    switch (filter.getOperator()) {
      case EQUALS:
        queryBuilder.append(" = '");
        queryBuilder.append(filter.getValues()[0]);
        queryBuilder.append('\'');
        break;
      case IN:
        queryBuilder.append(" IN (");
        instanceStatsDataFetcher.generateSqlInQuery(queryBuilder, filter.getValues());
        queryBuilder.append(')');
        break;
      default:
        throw new WingsException("Id operator not supported" + filter.getOperator());
    }
  }
}
