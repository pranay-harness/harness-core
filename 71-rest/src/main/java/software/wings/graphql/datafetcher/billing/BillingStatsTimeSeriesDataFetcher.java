package software.wings.graphql.datafetcher.billing;

import com.google.common.collect.Sets;
import com.google.inject.Inject;

import io.harness.exception.InvalidRequestException;
import io.harness.timescaledb.DBUtils;
import io.harness.timescaledb.TimeScaleDBService;
import lombok.extern.slf4j.Slf4j;
import software.wings.graphql.datafetcher.AbstractStatsDataFetcher;
import software.wings.graphql.datafetcher.billing.BillingDataQueryMetadata.BillingDataMetaDataFields;
import software.wings.graphql.schema.type.aggregation.QLData;
import software.wings.graphql.schema.type.aggregation.QLDataPoint;
import software.wings.graphql.schema.type.aggregation.QLReference;
import software.wings.graphql.schema.type.aggregation.QLStackedTimeSeriesData;
import software.wings.graphql.schema.type.aggregation.QLStackedTimeSeriesData.QLStackedTimeSeriesDataBuilder;
import software.wings.graphql.schema.type.aggregation.QLStackedTimeSeriesDataPoint;
import software.wings.graphql.schema.type.aggregation.QLStackedTimeSeriesDataPoint.QLStackedTimeSeriesDataPointBuilder;
import software.wings.graphql.schema.type.aggregation.QLTimeDataPoint;
import software.wings.graphql.schema.type.aggregation.QLTimeDataPoint.QLTimeDataPointBuilder;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingDataFilter;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingSortCriteria;
import software.wings.graphql.schema.type.aggregation.billing.QLCCMEntityGroupBy;
import software.wings.graphql.schema.type.aggregation.billing.QLCCMGroupBy;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.annotations.AuthRule;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;

@Slf4j
public class BillingStatsTimeSeriesDataFetcher extends AbstractStatsDataFetcher<QLCCMAggregationFunction,
    QLBillingDataFilter, QLCCMGroupBy, QLBillingSortCriteria> {
  @Inject private TimeScaleDBService timeScaleDBService;
  @Inject QLBillingStatsHelper statsHelper;
  @Inject BillingDataQueryBuilder billingDataQueryBuilder;

  private static final long ONE_DAY_MILLIS = 86400000;

  @Override
  @AuthRule(permissionType = PermissionType.LOGGED_IN)
  protected QLData fetch(String accountId, QLCCMAggregationFunction aggregateFunction,
      List<QLBillingDataFilter> filters, List<QLCCMGroupBy> groupBy, List<QLBillingSortCriteria> sortCriteria) {
    try {
      if (timeScaleDBService.isValid()) {
        return getData(accountId, filters, aggregateFunction, groupBy, sortCriteria);
      } else {
        throw new InvalidRequestException("Cannot process request in BillingStatsTimeSeriesDataFetcher");
      }
    } catch (Exception e) {
      throw new InvalidRequestException("Error while fetching billing data {}", e);
    }
  }

  @Override
  protected QLData postFetch(String accountId, List<QLCCMGroupBy> groupByList, QLData qlData) {
    return null;
  }

  protected QLData getData(@NotNull String accountId, List<QLBillingDataFilter> filters,
      QLCCMAggregationFunction aggregateFunction, List<QLCCMGroupBy> groupByList,
      List<QLBillingSortCriteria> sortCriteria) {
    BillingDataQueryMetadata queryData;
    ResultSet resultSet = null;
    List<QLCCMEntityGroupBy> groupByEntityList = billingDataQueryBuilder.getGroupByEntity(groupByList);
    if (filters == null) {
      filters = new ArrayList<>();
    }

    List<QLCCMAggregationFunction> aggregationFunctions = Arrays.asList(aggregateFunction);

    queryData =
        billingDataQueryBuilder.formQuery(accountId, filters, aggregationFunctions, groupByEntityList, sortCriteria);
    logger.info("BillingStatsTimeSeriesDataFetcher query!! {}", queryData.getQuery());
    logger.info(queryData.getQuery());

    try (Connection connection = timeScaleDBService.getDBConnection();
         Statement statement = connection.createStatement()) {
      resultSet = statement.executeQuery(queryData.getQuery());
      return generateStackedTimeSeriesData(queryData, resultSet, getMinStartTimeFromFilters(filters));
    } catch (SQLException e) {
      logger.error("BillingStatsTimeSeriesDataFetcher Error exception {}", e);
    } finally {
      DBUtils.close(resultSet);
    }
    return null;
  }

  protected QLStackedTimeSeriesData generateStackedTimeSeriesData(
      BillingDataQueryMetadata queryData, ResultSet resultSet, long startTimeFromFilters) throws SQLException {
    Map<Long, List<QLTimeDataPoint>> qlTimeDataPointMap = new LinkedHashMap<>();

    if (resultSet != null && resultSet.next()) {
      String id = "";
      for (BillingDataMetaDataFields field : queryData.getFieldNames()) {
        if (field.getFieldName().contains("ID")) {
          id = resultSet.getString(field.getFieldName());
        }
      }
      if (checkStartTimeFilterIsValid(startTimeFromFilters)) {
        long timeOfFirstEntry =
            resultSet.getTimestamp(BillingDataMetaDataFields.STARTTIME.getFieldName(), utils.getDefaultCalendar())
                .getTime();
        addDummyData(queryData, qlTimeDataPointMap, id, timeOfFirstEntry, startTimeFromFilters);
      }
    }

    do {
      QLTimeDataPointBuilder dataPointBuilder = QLTimeDataPoint.builder();
      for (BillingDataMetaDataFields field : queryData.getFieldNames()) {
        switch (field.getDataType()) {
          case DOUBLE:
            dataPointBuilder.value(Math.round(resultSet.getDouble(field.getFieldName()) * 100D) / 100D);
            break;
          case STRING:
            final String entityId = resultSet.getString(field.getFieldName());
            dataPointBuilder.key(buildQLReference(field, entityId));
            break;
          case TIMESTAMP:
            long time = resultSet.getTimestamp(field.getFieldName(), utils.getDefaultCalendar()).getTime();
            dataPointBuilder.time(time);
            break;
          default:
            throw new InvalidRequestException("UnsupportedType " + field.getDataType());
        }
      }
      QLTimeDataPoint dataPoint = dataPointBuilder.build();
      qlTimeDataPointMap.computeIfAbsent(dataPoint.getTime(), k -> new ArrayList<>());
      qlTimeDataPointMap.get(dataPoint.getTime()).add(dataPoint);
    } while (resultSet != null && resultSet.next());

    return prepareStackedTimeSeriesData(queryData, qlTimeDataPointMap);
  }

  private long getMinStartTimeFromFilters(List<QLBillingDataFilter> filters) {
    long minStartTime = Long.MAX_VALUE;
    for (QLBillingDataFilter filter : filters) {
      if (filter.getStartTime() != null) {
        minStartTime = Math.min(filter.getStartTime().getValue().longValue(), minStartTime);
      }
    }
    return minStartTime;
  }

  private boolean checkStartTimeFilterIsValid(long startTime) {
    return startTime != Long.MAX_VALUE;
  }

  private void addDummyData(BillingDataQueryMetadata queryData, Map<Long, List<QLTimeDataPoint>> qlTimeDataPointMap,
      String id, long timeOfFirstEntry, long startTimeFromFilters) {
    int missingDays = (int) ((timeOfFirstEntry - startTimeFromFilters) / ONE_DAY_MILLIS);
    long startTime = timeOfFirstEntry - missingDays * ONE_DAY_MILLIS;
    while (timeOfFirstEntry > startTime) {
      QLTimeDataPointBuilder dataPointBuilder = QLTimeDataPoint.builder();
      for (BillingDataMetaDataFields field : queryData.getFieldNames()) {
        switch (field.getDataType()) {
          case DOUBLE:
            dataPointBuilder.value(0);
            break;
          case STRING:
            final String entityId = id;
            dataPointBuilder.key(buildQLReference(field, entityId));
            break;
          case TIMESTAMP:
            dataPointBuilder.time(startTime);
            break;
          default:
            throw new InvalidRequestException("UnsupportedType " + field.getDataType());
        }
      }
      QLTimeDataPoint dataPoint = dataPointBuilder.build();
      qlTimeDataPointMap.computeIfAbsent(dataPoint.getTime(), k -> new ArrayList<>());
      qlTimeDataPointMap.get(dataPoint.getTime()).add(dataPoint);
      startTime += ONE_DAY_MILLIS;
    }
  }

  private QLReference buildQLReference(BillingDataMetaDataFields field, String key) {
    return QLReference.builder().type(field.getFieldName()).id(key).name(statsHelper.getEntityName(field, key)).build();
  }

  private QLStackedTimeSeriesData prepareStackedTimeSeriesData(
      BillingDataQueryMetadata queryData, Map<Long, List<QLTimeDataPoint>> qlTimeDataPointMap) {
    QLStackedTimeSeriesDataBuilder timeSeriesDataBuilder = QLStackedTimeSeriesData.builder();

    List<QLStackedTimeSeriesDataPoint> timeSeriesDataPoints = new ArrayList<>();

    qlTimeDataPointMap.keySet().forEach(time -> {
      List<QLTimeDataPoint> timeDataPoints = qlTimeDataPointMap.get(time);
      QLStackedTimeSeriesDataPointBuilder builder = QLStackedTimeSeriesDataPoint.builder();
      List<QLDataPoint> dataPoints =
          timeDataPoints.stream().map(QLTimeDataPoint::getQLDataPoint).collect(Collectors.toList());
      if (queryData.getGroupByFields() != null) {
        dataPoints = filterQLDataPoints(dataPoints, queryData.getFilters(), queryData.getGroupByFields().get(0));
      }
      builder.values(dataPoints).time(time);
      timeSeriesDataPoints.add(builder.build());
    });

    return timeSeriesDataBuilder.data(timeSeriesDataPoints).build();
  }

  private List<QLDataPoint> filterQLDataPoints(
      List<QLDataPoint> dataPoints, List<QLBillingDataFilter> filters, BillingDataMetaDataFields groupBy) {
    if (groupBy != null) {
      Map<BillingDataMetaDataFields, String[]> filterValueMap = getFilterDeploymentMetaDataField(filters);
      String[] values = filterValueMap.get(groupBy);
      if (values != null) {
        final Set valueSet = Sets.newHashSet(values);
        dataPoints.removeIf(dataPoint -> !valueSet.contains(dataPoint.getKey().getId()));
      }
    }

    return dataPoints;
  }

  private Map<BillingDataMetaDataFields, String[]> getFilterDeploymentMetaDataField(List<QLBillingDataFilter> filters) {
    Map<BillingDataMetaDataFields, String[]> filterMap = new HashMap<>();
    for (QLBillingDataFilter filter : filters) {
      if (filter.getApplication() != null) {
        filterMap.put(BillingDataMetaDataFields.APPID, filter.getApplication().getValues());
      }
    }

    return filterMap;
  }

  @Override
  public String getEntityType() {
    return null;
  }
}
