package software.wings.graphql.datafetcher.instance;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static software.wings.graphql.datafetcher.AbstractStatsDataFetcher.MAX_RETRY;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.exception.WingsException;
import io.harness.timescaledb.DBUtils;
import io.harness.timescaledb.TimeScaleDBConfig;
import io.harness.timescaledb.TimeScaleDBService;
import lombok.extern.slf4j.Slf4j;
import software.wings.graphql.schema.type.aggregation.QLDataPoint;
import software.wings.graphql.schema.type.aggregation.QLIdFilter;
import software.wings.graphql.schema.type.aggregation.QLNoOpAggregateFunction;
import software.wings.graphql.schema.type.aggregation.QLReference;
import software.wings.graphql.schema.type.aggregation.QLStackedTimeSeriesData;
import software.wings.graphql.schema.type.aggregation.QLStackedTimeSeriesDataPoint;
import software.wings.graphql.schema.type.aggregation.QLTimeFilter;
import software.wings.graphql.schema.type.aggregation.QLTimeOperator;
import software.wings.graphql.schema.type.aggregation.QLTimeSeriesAggregation;
import software.wings.graphql.schema.type.aggregation.QLTimeSeriesData;
import software.wings.graphql.schema.type.aggregation.QLTimeSeriesDataPoint;
import software.wings.graphql.schema.type.aggregation.QLTimeSeriesDataPoint.QLTimeSeriesDataPointBuilder;
import software.wings.graphql.schema.type.aggregation.instance.QLInstanceEntityAggregation;
import software.wings.graphql.schema.type.aggregation.instance.QLInstanceFilter;
import software.wings.graphql.utils.nameservice.NameResult;
import software.wings.graphql.utils.nameservice.NameService;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
  private static int DEFAULT_EXPIRY_IN_DAYS = 210;

  public QLStackedTimeSeriesData getTimeSeriesAggregatedData(String accountId,
      QLNoOpAggregateFunction aggregateFunction, List<QLInstanceFilter> filters, QLTimeSeriesAggregation groupByTime,
      QLInstanceEntityAggregation groupByEntity) {
    //    SELECT PERCENTILE_DISC(0.50) WITHIN GROUP (ORDER BY SUM_VALUE) AS CNT,
    //    time_bucket_gapfill('1 days',REPORTEDAT, '2019-08-03T10:35:10.248Z', '2019-08-10T10:35:10.248Z') AS
    //    GRP_BY_TIME, ENTITY_ID FROM (SELECT APPID AS ENTITY_ID, REPORTEDAT, SUM(INSTANCECOUNT) AS SUM_VALUE FROM
    //    INSTANCE_STATS WHERE REPORTEDAT  >= timestamp '2019-08-03T10:35:10.248Z' AND  REPORTEDAT  < timestamp
    //    '2019-08-10T10:35:10.248Z' AND ACCOUNTID = 'kmpySmUISimoRrJL6NL73w' GROUP BY ENTITY_ID, REPORTEDAT)
    //    INSTANCE_STATS GROUP BY ENTITY_ID, GRP_BY_TIME ORDER BY GRP_BY_TIME

    try {
      StringBuilder queryBuilder = new StringBuilder(350);
      queryBuilder.append("SELECT PERCENTILE_DISC(0.50) WITHIN GROUP (ORDER BY SUM_VALUE) AS CNT, ");

      String[] values = extractTime(filters);
      String from = values[0];
      String to = values[1];

      String timeQuerySegment =
          instanceStatsDataFetcher.getGroupByTimeQueryWithGapFill(groupByTime, "REPORTEDAT", from, to);

      String groupByFieldName = getSqlFieldName(groupByEntity);
      queryBuilder.append(timeQuerySegment)
          .append(" AS GRP_BY_TIME, ENTITY_ID FROM (SELECT ")
          .append(groupByFieldName)
          .append(" AS ENTITY_ID, REPORTEDAT, SUM(INSTANCECOUNT) AS SUM_VALUE FROM INSTANCE_STATS WHERE ");
      if (isNotEmpty(filters)) {
        filters.forEach(filter -> {
          String sqlFilter = getSqlFilter(filter);
          if (isNotEmpty(sqlFilter)) {
            queryBuilder.append(sqlFilter);
            queryBuilder.append(" AND ");
          }
        });
      }

      queryBuilder.append(" REPORTEDAT ");
      addTimeQuery(queryBuilder, QLTimeOperator.AFTER, from);
      queryBuilder.append(" AND REPORTEDAT ");
      addTimeQuery(queryBuilder, QLTimeOperator.BEFORE, to);
      queryBuilder.append(" AND ACCOUNTID = '")
          .append(accountId)
          .append(
              "' GROUP BY ENTITY_ID, REPORTEDAT) INSTANCE_STATS GROUP BY ENTITY_ID, GRP_BY_TIME ORDER BY GRP_BY_TIME");

      List<QLStackedTimeSeriesDataPoint> dataPoints = new ArrayList<>();
      executeStackedTimeSeriesQuery(accountId, queryBuilder.toString(), dataPoints, groupByEntity);
      return QLStackedTimeSeriesData.builder().data(dataPoints).build();
    } catch (Exception ex) {
      throw new WingsException("Error while getting time series data", ex);
    }
  }

  private String[] extractTime(List<QLInstanceFilter> filters) {
    String from = null;
    String to = null;

    if (isNotEmpty(filters)) {
      for (QLInstanceFilter filter : filters) {
        QLTimeFilter createdAt = filter.getCreatedAt();
        if (createdAt == null) {
          continue;
        }

        QLTimeOperator operator = createdAt.getOperator();
        if (operator == null) {
          throw new WingsException("Time operator should have operator set");
        }

        switch (operator) {
          case BEFORE:
            to = Instant.ofEpochMilli((Long) createdAt.getValue()).toString();
            break;
          case AFTER:
            from = Instant.ofEpochMilli((Long) createdAt.getValue()).toString();
            break;
          default:
            throw new WingsException("Time operator " + operator.name() + " not supported.");
        }
      }
    }

    if (isEmpty(from) && isEmpty(to)) {
      logger.info("No time filter set in Instance time series stats query. Setting default to 7 days");
      Instant now = Instant.now();
      from = now.minus(7, ChronoUnit.DAYS).toString();
      to = now.toString();
    } else if (isEmpty(from)) {
      from = Instant.parse(to).minus(7, ChronoUnit.DAYS).toString();
    } else if (isEmpty(to)) {
      to = Instant.parse(from).plus(7, ChronoUnit.DAYS).toString();
    }
    return new String[] {from, to};
  }

  public QLTimeSeriesData getTimeSeriesData(String accountId, QLNoOpAggregateFunction aggregateFunction,
      List<QLInstanceFilter> filters, QLTimeSeriesAggregation groupByTime) {
    //    select percentile_disc(0.95) within group (order by sum_value) as percent, time_bucket_gapfill('1
    //    hours',reportedat) AS grp_by_time from (select reportedat, sum(INSTANCECOUNT) as sum_value from INSTANCE_STATS
    //    group by reportedat) INSTANCE_STATS group by grp_by_time order by grp_by_time

    try {
      StringBuilder queryBuilder = new StringBuilder(302);
      queryBuilder.append("SELECT PERCENTILE_DISC(0.50) WITHIN GROUP (ORDER BY SUM_VALUE) AS CNT, ");
      String[] values = extractTime(filters);
      String from = values[0];
      String to = values[1];

      String timeQuerySegment =
          instanceStatsDataFetcher.getGroupByTimeQueryWithGapFill(groupByTime, "REPORTEDAT", from, to);

      queryBuilder.append(timeQuerySegment)
          .append(
              " AS GRP_BY_TIME FROM (SELECT REPORTEDAT, SUM(INSTANCECOUNT) AS SUM_VALUE FROM INSTANCE_STATS WHERE ");
      if (isNotEmpty(filters)) {
        filters.forEach(filter -> {
          String sqlFilter = getSqlFilter(filter);
          if (isNotEmpty(sqlFilter)) {
            queryBuilder.append(sqlFilter);
            queryBuilder.append(" AND ");
          }
        });
      }

      queryBuilder.append(" REPORTEDAT ");
      addTimeQuery(queryBuilder, QLTimeOperator.AFTER, from);
      queryBuilder.append(" AND REPORTEDAT ");
      addTimeQuery(queryBuilder, QLTimeOperator.BEFORE, to);
      queryBuilder.append(" AND ACCOUNTID = '")
          .append(accountId)
          .append("' GROUP BY REPORTEDAT) INSTANCE_STATS GROUP BY GRP_BY_TIME ORDER BY GRP_BY_TIME");

      List<QLTimeSeriesDataPoint> dataPoints = new ArrayList<>();
      executeTimeSeriesQuery(accountId, queryBuilder.toString(), dataPoints);
      return QLTimeSeriesData.builder().dataPoints(dataPoints).build();
    } catch (Exception ex) {
      throw new WingsException("Error while getting time series data", ex);
    }
  }

  private String getSqlFieldName(QLInstanceEntityAggregation aggregation) {
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
      ResultSet resultSet = null;
      try (Connection connection = timeScaleDBService.getDBConnection();
           Statement statement = connection.createStatement()) {
        resultSet = statement.executeQuery(query);
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
      } finally {
        DBUtils.close(resultSet);
      }
    }
  }

  public void purgeOldInstances() {
    if (!timeScaleDBService.isValid()) {
      logger.info("Skipping purge of old instances from time scale db since time scale db is not initialized");
      return;
    }

    int retryCount = 0;
    TimeScaleDBConfig timeScaleDBConfig = timeScaleDBService.getTimeScaleDBConfig();
    int expiryInDays = timeScaleDBConfig != null && timeScaleDBConfig.getInstanceDataRetentionDays() > 0
        ? timeScaleDBConfig.getInstanceDataRetentionDays()
        : DEFAULT_EXPIRY_IN_DAYS;
    String query = new StringBuilder("SELECT drop_chunks(interval '")
                       .append(expiryInDays)
                       .append(" days', 'instance_stats')")
                       .toString();

    while (retryCount < MAX_RETRY) {
      try (Connection connection = timeScaleDBService.getDBConnection();
           Statement statement = connection.createStatement()) {
        statement.execute(query);
        return;
      } catch (SQLException e) {
        if (retryCount >= MAX_RETRY) {
          logger.error("Failed to execute query=[{}]", query, e);
        } else {
          logger.warn("Failed to execute query=[{}], retryCount=[{}]", query, retryCount);
        }
        retryCount++;
      }
    }
  }

  private void executeStackedTimeSeriesQuery(String accountId, String query,
      List<QLStackedTimeSeriesDataPoint> stackedTimeSeriesDataPoints, QLInstanceEntityAggregation groupBy) {
    int retryCount = 0;
    String entityType = groupBy.name();
    Multimap<String, QLReference> idRefMap = HashMultimap.create();
    while (retryCount < MAX_RETRY) {
      ResultSet resultSet = null;
      try (Connection connection = timeScaleDBService.getDBConnection();
           Statement statement = connection.createStatement()) {
        resultSet = statement.executeQuery(query);
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
      } finally {
        DBUtils.close(resultSet);
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

  private void addTimeQuery(StringBuilder queryBuilder, QLTimeOperator operator, String timestamp) {
    switch (operator) {
      case EQUALS:
        queryBuilder.append(" = timestamp '");
        queryBuilder.append(timestamp);
        queryBuilder.append('\'');
        break;
      case BEFORE:
        queryBuilder.append(" < timestamp '");
        queryBuilder.append(timestamp);
        queryBuilder.append('\'');
        break;
      case AFTER:
        queryBuilder.append(" >= timestamp '");
        queryBuilder.append(timestamp);
        queryBuilder.append('\'');
        break;
      default:
        throw new RuntimeException("Time operator not supported" + timestamp);
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
