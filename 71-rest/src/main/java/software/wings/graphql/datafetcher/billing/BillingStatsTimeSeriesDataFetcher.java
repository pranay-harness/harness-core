package software.wings.graphql.datafetcher.billing;

import com.google.common.collect.Sets;
import com.google.inject.Inject;

import io.harness.exception.InvalidRequestException;
import io.harness.timescaledb.DBUtils;
import io.harness.timescaledb.TimeScaleDBService;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.EntityType;
import software.wings.graphql.datafetcher.AbstractStatsDataFetcherWithAggregationListAndTags;
import software.wings.graphql.datafetcher.billing.BillingDataQueryMetadata.BillingDataMetaDataFields;
import software.wings.graphql.schema.type.aggregation.QLBillingDataPoint;
import software.wings.graphql.schema.type.aggregation.QLBillingStackedTimeSeriesData;
import software.wings.graphql.schema.type.aggregation.QLBillingStackedTimeSeriesData.QLBillingStackedTimeSeriesDataBuilder;
import software.wings.graphql.schema.type.aggregation.QLBillingStackedTimeSeriesDataPoint;
import software.wings.graphql.schema.type.aggregation.QLBillingStackedTimeSeriesDataPoint.QLBillingStackedTimeSeriesDataPointBuilder;
import software.wings.graphql.schema.type.aggregation.QLBillingTimeDataPoint;
import software.wings.graphql.schema.type.aggregation.QLBillingTimeDataPoint.QLBillingTimeDataPointBuilder;
import software.wings.graphql.schema.type.aggregation.QLData;
import software.wings.graphql.schema.type.aggregation.QLReference;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingDataFilter;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingDataLabelAggregation;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingDataTagAggregation;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingDataTagType;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingSortCriteria;
import software.wings.graphql.schema.type.aggregation.billing.QLCCMEntityGroupBy;
import software.wings.graphql.schema.type.aggregation.billing.QLCCMGroupBy;
import software.wings.graphql.schema.type.aggregation.billing.QLCCMTimeSeriesAggregation;
import software.wings.graphql.utils.nameservice.NameService;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.annotations.AuthRule;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;

@Slf4j
public class BillingStatsTimeSeriesDataFetcher
    extends AbstractStatsDataFetcherWithAggregationListAndTags<QLCCMAggregationFunction, QLBillingDataFilter,
        QLCCMGroupBy, QLBillingSortCriteria, QLBillingDataTagType, QLBillingDataTagAggregation,
        QLBillingDataLabelAggregation, QLCCMEntityGroupBy> {
  @Inject private TimeScaleDBService timeScaleDBService;
  @Inject QLBillingStatsHelper statsHelper;
  @Inject BillingDataQueryBuilder billingDataQueryBuilder;

  private static final long ONE_DAY_MILLIS = 86400000;

  @Override
  @AuthRule(permissionType = PermissionType.LOGGED_IN)
  protected QLData fetch(String accountId, List<QLCCMAggregationFunction> aggregateFunction,
      List<QLBillingDataFilter> filters, List<QLCCMGroupBy> groupBy, List<QLBillingSortCriteria> sortCriteria,
      Integer limit, Integer offset) {
    boolean timeScaleDBServiceValid = timeScaleDBService.isValid();
    logger.info("Timescale db service status {}", timeScaleDBServiceValid);
    if (!timeScaleDBServiceValid) {
      throw new InvalidRequestException("Cannot process request in BillingStatsTimeSeriesDataFetcher");
    }
    try {
      return getData(accountId, filters, aggregateFunction, groupBy, sortCriteria);
    } catch (Exception e) {
      throw new InvalidRequestException("Error while fetching billing data {}", e);
    }
  }

  protected QLData getData(@NotNull String accountId, List<QLBillingDataFilter> filters,
      List<QLCCMAggregationFunction> aggregateFunction, List<QLCCMGroupBy> groupByList,
      List<QLBillingSortCriteria> sortCriteria) {
    BillingDataQueryMetadata queryData;
    ResultSet resultSet = null;
    List<QLCCMEntityGroupBy> groupByEntityList = billingDataQueryBuilder.getGroupByEntity(groupByList);
    List<QLBillingDataTagAggregation> groupByTagList = getGroupByTag(groupByList);
    List<QLBillingDataLabelAggregation> groupByLabelList = getGroupByLabel(groupByList);
    QLCCMTimeSeriesAggregation groupByTime = billingDataQueryBuilder.getGroupByTime(groupByList);

    if (!groupByTagList.isEmpty()) {
      groupByEntityList = getGroupByEntityListFromTags(groupByList, groupByEntityList, groupByTagList);
    } else if (!groupByLabelList.isEmpty()) {
      groupByEntityList = getGroupByEntityListFromLabels(groupByList, groupByEntityList, groupByLabelList);
    }
    groupByEntityList = getGroupByEntityListFromTags(groupByList, groupByEntityList, groupByTagList);

    if (filters == null) {
      filters = new ArrayList<>();
    }

    queryData = billingDataQueryBuilder.formQuery(
        accountId, filters, aggregateFunction, groupByEntityList, groupByTime, sortCriteria, true);
    logger.info("BillingStatsTimeSeriesDataFetcher query: {}", queryData.getQuery());
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

  protected QLBillingStackedTimeSeriesData generateStackedTimeSeriesData(
      BillingDataQueryMetadata queryData, ResultSet resultSet, long startTimeFromFilters) throws SQLException {
    Map<Long, List<QLBillingTimeDataPoint>> qlTimeDataPointMap = new LinkedHashMap<>();
    Map<Long, List<QLBillingTimeDataPoint>> qlTimeCpuPointMap = new LinkedHashMap<>();
    Map<Long, List<QLBillingTimeDataPoint>> qlTimeMemoryPointMap = new LinkedHashMap<>();
    Map<Long, List<QLBillingTimeDataPoint>> qlTimeMemoryUtilsPointMap = new LinkedHashMap<>();
    Map<Long, List<QLBillingTimeDataPoint>> qlTimeCpuUtilsPointMap = new LinkedHashMap<>();

    checkAndAddPrecedingZeroValuedData(queryData, resultSet, startTimeFromFilters, qlTimeDataPointMap);
    // Checking if namespace should be appended to entity Id in order to distinguish between same workloadNames across
    // Distinct namespaces
    boolean addNamespaceToEntityId = queryData.groupByFields.contains(BillingDataMetaDataFields.WORKLOADNAME);
    String additionalInfo = "";

    do {
      QLBillingTimeDataPointBuilder dataPointBuilder = QLBillingTimeDataPoint.builder();
      // For First Level Idle Cost Drill Down
      QLBillingTimeDataPointBuilder cpuPointBuilder = QLBillingTimeDataPoint.builder();
      QLBillingTimeDataPointBuilder memoryPointBuilder = QLBillingTimeDataPoint.builder();
      // For Leaf level Idle cost Drill Down
      QLBillingTimeDataPointBuilder memoryAvgUtilsPointBuilder = QLBillingTimeDataPoint.builder();
      QLBillingTimeDataPointBuilder memoryMaxUtilsPointBuilder = QLBillingTimeDataPoint.builder();

      QLBillingTimeDataPointBuilder cpuAvgUtilsPointBuilder = QLBillingTimeDataPoint.builder();
      QLBillingTimeDataPointBuilder cpuMaxUtilsPointBuilder = QLBillingTimeDataPoint.builder();

      for (BillingDataMetaDataFields field : queryData.getFieldNames()) {
        switch (field.getDataType()) {
          case DOUBLE:
            switch (field) {
              case CPUIDLECOST:
                cpuPointBuilder.value(roundingDoubleFieldValue(field, resultSet));
                break;
              case MEMORYIDLECOST:
                memoryPointBuilder.value(roundingDoubleFieldValue(field, resultSet));
                break;
              case MAXCPUUTILIZATION:
                cpuMaxUtilsPointBuilder.value(roundingDoubleFieldPercentageValue(field, resultSet));
                break;
              case AVGCPUUTILIZATION:
                cpuAvgUtilsPointBuilder.value(roundingDoubleFieldPercentageValue(field, resultSet));
                break;
              case MAXMEMORYUTILIZATION:
                memoryMaxUtilsPointBuilder.value(roundingDoubleFieldPercentageValue(field, resultSet));
                break;
              case AVGMEMORYUTILIZATION:
                memoryAvgUtilsPointBuilder.value(roundingDoubleFieldPercentageValue(field, resultSet));
                break;
              default:
                dataPointBuilder.value(roundingDoubleFieldValue(field, resultSet));
            }
            break;
          case STRING:
            if (addNamespaceToEntityId && field == BillingDataMetaDataFields.NAMESPACE) {
              additionalInfo = resultSet.getString(field.getFieldName());
              break;
            }

            String entityId = resultSet.getString(field.getFieldName());
            String idWithInfo =
                addNamespaceToEntityId ? additionalInfo + BillingStatsDefaultKeys.TOKEN + entityId : entityId;
            cpuPointBuilder.key(buildQLReference(field, entityId, idWithInfo));
            memoryPointBuilder.key(buildQLReference(field, entityId, idWithInfo));
            dataPointBuilder.key(buildQLReference(field, entityId, idWithInfo));
            cpuMaxUtilsPointBuilder.key(buildQLReferenceForUtilization("MAX", idWithInfo));
            cpuAvgUtilsPointBuilder.key(buildQLReferenceForUtilization("AVG", idWithInfo));
            memoryMaxUtilsPointBuilder.key(buildQLReferenceForUtilization("MAX", idWithInfo));
            memoryAvgUtilsPointBuilder.key(buildQLReferenceForUtilization("AVG", idWithInfo));
            break;
          case TIMESTAMP:
            long time = resultSet.getTimestamp(field.getFieldName(), utils.getDefaultCalendar()).getTime();
            cpuPointBuilder.time(time);
            memoryPointBuilder.time(time);
            dataPointBuilder.time(time);
            cpuMaxUtilsPointBuilder.time(time);
            cpuAvgUtilsPointBuilder.time(time);
            memoryMaxUtilsPointBuilder.time(time);
            memoryAvgUtilsPointBuilder.time(time);
            break;
          default:
            throw new InvalidRequestException("UnsupportedType " + field.getDataType());
        }
      }

      checkDataPointIsValidAndInsert(dataPointBuilder.build(), qlTimeDataPointMap);
      checkDataPointIsValidAndInsert(cpuPointBuilder.build(), qlTimeCpuPointMap);
      checkDataPointIsValidAndInsert(memoryPointBuilder.build(), qlTimeMemoryPointMap);
      checkDataPointIsValidAndInsert(cpuMaxUtilsPointBuilder.build(), qlTimeCpuUtilsPointMap);
      checkDataPointIsValidAndInsert(memoryMaxUtilsPointBuilder.build(), qlTimeMemoryUtilsPointMap);
      checkDataPointIsValidAndInsert(cpuAvgUtilsPointBuilder.build(), qlTimeCpuUtilsPointMap);
      checkDataPointIsValidAndInsert(memoryAvgUtilsPointBuilder.build(), qlTimeMemoryUtilsPointMap);
    } while (resultSet != null && resultSet.next());

    QLBillingStackedTimeSeriesDataBuilder timeSeriesDataBuilder = QLBillingStackedTimeSeriesData.builder();

    return timeSeriesDataBuilder.data(prepareStackedTimeSeriesData(queryData, qlTimeDataPointMap))
        .cpuIdleCost(prepareStackedTimeSeriesData(queryData, qlTimeCpuPointMap))
        .memoryIdleCost(prepareStackedTimeSeriesData(queryData, qlTimeMemoryPointMap))
        .cpuUtilMetrics(prepareStackedTimeSeriesData(queryData, qlTimeCpuUtilsPointMap))
        .memoryUtilMetrics(prepareStackedTimeSeriesData(queryData, qlTimeMemoryUtilsPointMap))
        .build();
  }

  private void checkDataPointIsValidAndInsert(
      QLBillingTimeDataPoint dataPoint, Map<Long, List<QLBillingTimeDataPoint>> qlTimeDataPointMap) {
    if (dataPoint.getValue() != null) {
      qlTimeDataPointMap.computeIfAbsent(dataPoint.getTime(), k -> new ArrayList<>());
      qlTimeDataPointMap.get(dataPoint.getTime()).add(dataPoint);
    }
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

  private double roundingDoubleFieldValue(BillingDataMetaDataFields field, ResultSet resultSet) throws SQLException {
    return Math.round(resultSet.getDouble(field.getFieldName()) * 100D) / 100D;
  }

  private double roundingDoubleFieldPercentageValue(BillingDataMetaDataFields field, ResultSet resultSet)
      throws SQLException {
    return 100 * roundingDoubleFieldValue(field, resultSet);
  }

  private void checkAndAddPrecedingZeroValuedData(BillingDataQueryMetadata queryData, ResultSet resultSet,
      long startTimeFromFilters, Map<Long, List<QLBillingTimeDataPoint>> qlTimeDataPointMap) throws SQLException {
    if (resultSet != null && resultSet.next()) {
      String entityId = "";
      String idWithInfo = "";
      String additionalInfo = "";
      String timeFieldName = BillingDataMetaDataFields.STARTTIME.getFieldName();
      boolean addNamespaceToEntityId = queryData.groupByFields.contains(BillingDataMetaDataFields.WORKLOADNAME);
      for (BillingDataMetaDataFields field : queryData.getFieldNames()) {
        switch (field.getDataType()) {
          case STRING:
            if (addNamespaceToEntityId && field == BillingDataMetaDataFields.NAMESPACE) {
              additionalInfo = resultSet.getString(field.getFieldName());
              break;
            }
            entityId = resultSet.getString(field.getFieldName());
            idWithInfo = addNamespaceToEntityId ? additionalInfo + BillingStatsDefaultKeys.TOKEN + entityId : entityId;
            break;
          case TIMESTAMP:
            timeFieldName = field.getFieldName();
            break;
          default:
            break;
        }
      }
      if (checkStartTimeFilterIsValid(startTimeFromFilters)) {
        long timeOfFirstEntry = resultSet.getTimestamp(timeFieldName, utils.getDefaultCalendar()).getTime();
        addPrecedingZeroValuedData(
            queryData, qlTimeDataPointMap, entityId, idWithInfo, timeOfFirstEntry, startTimeFromFilters);
      }
    }
  }

  private void addPrecedingZeroValuedData(BillingDataQueryMetadata queryData,
      Map<Long, List<QLBillingTimeDataPoint>> qlTimeDataPointMap, String entityId, String idWithInfo,
      long timeOfFirstEntry, long startTimeFromFilters) {
    int missingDays = (int) ((timeOfFirstEntry - startTimeFromFilters) / ONE_DAY_MILLIS);
    long startTime = timeOfFirstEntry - missingDays * ONE_DAY_MILLIS;
    while (timeOfFirstEntry > startTime) {
      QLBillingTimeDataPointBuilder dataPointBuilder = QLBillingTimeDataPoint.builder();
      for (BillingDataMetaDataFields field : queryData.getFieldNames()) {
        switch (field.getDataType()) {
          case DOUBLE:
            dataPointBuilder.value(0);
            break;
          case STRING:
            dataPointBuilder.key(buildQLReference(field, entityId, idWithInfo));
            break;
          case TIMESTAMP:
            dataPointBuilder.time(startTime);
            break;
          default:
            throw new InvalidRequestException("UnsupportedType " + field.getDataType());
        }
      }
      QLBillingTimeDataPoint dataPoint = dataPointBuilder.build();
      qlTimeDataPointMap.computeIfAbsent(dataPoint.getTime(), k -> new ArrayList<>());
      qlTimeDataPointMap.get(dataPoint.getTime()).add(dataPoint);
      startTime += ONE_DAY_MILLIS;
    }
  }

  private QLReference buildQLReference(BillingDataMetaDataFields field, String key, String id) {
    return QLReference.builder().type(field.getFieldName()).id(id).name(statsHelper.getEntityName(field, key)).build();
  }

  private QLReference buildQLReferenceForUtilization(String name, String id) {
    return QLReference.builder().name(name).id(id).type("Utilization").build();
  }

  private List<QLBillingStackedTimeSeriesDataPoint> prepareStackedTimeSeriesData(
      BillingDataQueryMetadata queryData, Map<Long, List<QLBillingTimeDataPoint>> qlTimeDataPointMap) {
    List<QLBillingStackedTimeSeriesDataPoint> timeSeriesDataPoints = new ArrayList<>();

    qlTimeDataPointMap.keySet().forEach(time -> {
      List<QLBillingTimeDataPoint> timeDataPoints = qlTimeDataPointMap.get(time);
      QLBillingStackedTimeSeriesDataPointBuilder builder = QLBillingStackedTimeSeriesDataPoint.builder();
      List<QLBillingDataPoint> dataPoints =
          timeDataPoints.stream().map(QLBillingTimeDataPoint::getQLBillingDataPoint).collect(Collectors.toList());
      if (queryData.getGroupByFields() != null) {
        dataPoints = filterQLDataPoints(dataPoints, queryData.getFilters(), queryData.getGroupByFields().get(0));
      }
      builder.values(dataPoints).time(time);
      timeSeriesDataPoints.add(builder.build());
    });
    return timeSeriesDataPoints;
  }

  private List<QLBillingDataPoint> filterQLDataPoints(
      List<QLBillingDataPoint> dataPoints, List<QLBillingDataFilter> filters, BillingDataMetaDataFields groupBy) {
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
    Map<BillingDataMetaDataFields, String[]> filterMap = new EnumMap<>(BillingDataMetaDataFields.class);
    for (QLBillingDataFilter filter : filters) {
      if (filter.getApplication() != null) {
        filterMap.put(BillingDataMetaDataFields.APPID, filter.getApplication().getValues());
      }
    }
    return filterMap;
  }

  @Override
  public QLData postFetch(String accountId, List<QLCCMGroupBy> groupBy,
      List<QLCCMAggregationFunction> aggregateFunction, List<QLBillingSortCriteria> sortCriteria, QLData qlData,
      Integer limit) {
    qlData = super.postFetch(accountId, groupBy, aggregateFunction, sortCriteria, qlData, limit);
    if (limit.equals(BillingStatsDefaultKeys.DEFAULT_LIMIT)) {
      return qlData;
    }
    Map<String, Double> aggregatedData = new HashMap<>();
    QLBillingStackedTimeSeriesData data = (QLBillingStackedTimeSeriesData) qlData;
    data.getData().forEach(dataPoint -> {
      for (QLBillingDataPoint entry : dataPoint.getValues()) {
        String key = entry.getKey().getId();
        if (aggregatedData.containsKey(key)) {
          aggregatedData.put(key, entry.getValue().doubleValue() + aggregatedData.get(key));
        } else {
          aggregatedData.put(key, entry.getValue().doubleValue());
        }
      }
    });
    List<String> selectedIdsAfterLimit = getElementIdsAfterLimit(aggregatedData, limit);

    return QLBillingStackedTimeSeriesData.builder()
        .data(getDataAfterLimit(data, selectedIdsAfterLimit))
        .cpuIdleCost(data.getCpuIdleCost())
        .memoryIdleCost(data.getMemoryIdleCost())
        .cpuUtilMetrics(data.getCpuUtilMetrics())
        .memoryUtilMetrics(data.getMemoryUtilMetrics())
        .build();
  }

  private List<QLBillingStackedTimeSeriesDataPoint> getDataAfterLimit(
      QLBillingStackedTimeSeriesData data, List<String> selectedIdsAfterLimit) {
    List<QLBillingStackedTimeSeriesDataPoint> limitProcessedData = new ArrayList<>();
    data.getData().forEach(dataPoint -> {
      List<QLBillingDataPoint> limitProcessedValues = new ArrayList<>();
      QLBillingDataPoint others =
          QLBillingDataPoint.builder()
              .key(
                  QLReference.builder().id(BillingStatsDefaultKeys.OTHERS).name(BillingStatsDefaultKeys.OTHERS).build())
              .value(0)
              .build();
      for (QLBillingDataPoint entry : dataPoint.getValues()) {
        String key = entry.getKey().getId();
        if (selectedIdsAfterLimit.contains(key)) {
          limitProcessedValues.add(entry);
        } else {
          others.setValue(others.getValue().doubleValue() + entry.getValue().doubleValue());
        }
      }

      if (others.getValue().doubleValue() > 0) {
        others.setValue(billingDataHelper.getRoundedDoubleValue(others.getValue().doubleValue()));
        limitProcessedValues.add(others);
      }

      limitProcessedData.add(
          QLBillingStackedTimeSeriesDataPoint.builder().time(dataPoint.getTime()).values(limitProcessedValues).build());
    });
    return limitProcessedData;
  }

  private List<String> getElementIdsAfterLimit(Map<String, Double> aggregatedData, int limit) {
    List<Map.Entry<String, Double>> list = new ArrayList<>(aggregatedData.entrySet());
    list.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));
    list = list.stream().limit(limit).collect(Collectors.toList());
    List<String> topNElementIds = new ArrayList<>();
    list.forEach(entry -> topNElementIds.add(entry.getKey()));
    return topNElementIds;
  }

  @Override
  public String getEntityType() {
    return NameService.deployment;
  }

  @Override
  protected QLBillingDataTagAggregation getTagAggregation(QLCCMGroupBy groupBy) {
    return groupBy.getTagAggregation();
  }

  @Override
  protected QLBillingDataLabelAggregation getLabelAggregation(QLCCMGroupBy groupBy) {
    return groupBy.getLabelAggregation();
  }

  @Override
  protected QLCCMEntityGroupBy getGroupByEntityFromTag(QLBillingDataTagAggregation groupByTag) {
    return billingDataQueryBuilder.getGroupByEntityFromTag(groupByTag);
  }

  @Override
  protected QLCCMEntityGroupBy getGroupByEntityFromLabel(QLBillingDataLabelAggregation groupByLabel) {
    return billingDataQueryBuilder.getGroupByEntityFromLabel(groupByLabel);
  }

  @Override
  protected EntityType getEntityType(QLBillingDataTagType entityType) {
    return billingDataQueryBuilder.getEntityType(entityType);
  }

  @Override
  protected QLCCMEntityGroupBy getEntityAggregation(QLCCMGroupBy groupBy) {
    return groupBy.getEntityGroupBy();
  }
}
