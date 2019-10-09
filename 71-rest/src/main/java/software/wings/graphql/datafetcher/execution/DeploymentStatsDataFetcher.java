package software.wings.graphql.datafetcher.execution;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.google.common.collect.Sets;
import com.google.inject.Inject;

import com.healthmarketscience.sqlbuilder.BinaryCondition;
import com.healthmarketscience.sqlbuilder.ComboCondition;
import com.healthmarketscience.sqlbuilder.ComboCondition.Op;
import com.healthmarketscience.sqlbuilder.Converter;
import com.healthmarketscience.sqlbuilder.CustomCondition;
import com.healthmarketscience.sqlbuilder.CustomExpression;
import com.healthmarketscience.sqlbuilder.FunctionCall;
import com.healthmarketscience.sqlbuilder.InCondition;
import com.healthmarketscience.sqlbuilder.OrderObject;
import com.healthmarketscience.sqlbuilder.OrderObject.Dir;
import com.healthmarketscience.sqlbuilder.SelectQuery;
import com.healthmarketscience.sqlbuilder.UnaryCondition;
import com.healthmarketscience.sqlbuilder.dbspec.basic.DbColumn;
import io.fabric8.utils.Lists;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.timescaledb.DBUtils;
import io.harness.timescaledb.TimeScaleDBService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.EntityType;
import software.wings.graphql.datafetcher.AbstractStatsDataFetcherWithTags;
import software.wings.graphql.datafetcher.execution.DeploymentStatsQueryMetaData.DeploymentMetaDataFields;
import software.wings.graphql.datafetcher.execution.DeploymentStatsQueryMetaData.DeploymentStatsQueryMetaDataBuilder;
import software.wings.graphql.datafetcher.execution.DeploymentStatsQueryMetaData.ResultType;
import software.wings.graphql.datafetcher.tag.TagHelper;
import software.wings.graphql.schema.type.aggregation.Filter;
import software.wings.graphql.schema.type.aggregation.QLAggregatedData;
import software.wings.graphql.schema.type.aggregation.QLAggregatedData.QLAggregatedDataBuilder;
import software.wings.graphql.schema.type.aggregation.QLAggregationKind;
import software.wings.graphql.schema.type.aggregation.QLData;
import software.wings.graphql.schema.type.aggregation.QLDataPoint;
import software.wings.graphql.schema.type.aggregation.QLDataPoint.QLDataPointBuilder;
import software.wings.graphql.schema.type.aggregation.QLEnumOperator;
import software.wings.graphql.schema.type.aggregation.QLFilterKind;
import software.wings.graphql.schema.type.aggregation.QLIdFilter;
import software.wings.graphql.schema.type.aggregation.QLIdOperator;
import software.wings.graphql.schema.type.aggregation.QLIntermediateStackDataPoint;
import software.wings.graphql.schema.type.aggregation.QLIntermediateStackDataPoint.QLIntermediateStackDataPointBuilder;
import software.wings.graphql.schema.type.aggregation.QLNumberFilter;
import software.wings.graphql.schema.type.aggregation.QLNumberOperator;
import software.wings.graphql.schema.type.aggregation.QLReference;
import software.wings.graphql.schema.type.aggregation.QLSinglePointData;
import software.wings.graphql.schema.type.aggregation.QLSinglePointData.QLSinglePointDataBuilder;
import software.wings.graphql.schema.type.aggregation.QLSortOrder;
import software.wings.graphql.schema.type.aggregation.QLStackedData;
import software.wings.graphql.schema.type.aggregation.QLStackedData.QLStackedDataBuilder;
import software.wings.graphql.schema.type.aggregation.QLStackedDataPoint;
import software.wings.graphql.schema.type.aggregation.QLStackedDataPoint.QLStackedDataPointBuilder;
import software.wings.graphql.schema.type.aggregation.QLStackedTimeSeriesData;
import software.wings.graphql.schema.type.aggregation.QLStackedTimeSeriesData.QLStackedTimeSeriesDataBuilder;
import software.wings.graphql.schema.type.aggregation.QLStackedTimeSeriesDataPoint;
import software.wings.graphql.schema.type.aggregation.QLStackedTimeSeriesDataPoint.QLStackedTimeSeriesDataPointBuilder;
import software.wings.graphql.schema.type.aggregation.QLStringFilter;
import software.wings.graphql.schema.type.aggregation.QLStringOperator;
import software.wings.graphql.schema.type.aggregation.QLTimeDataPoint;
import software.wings.graphql.schema.type.aggregation.QLTimeDataPoint.QLTimeDataPointBuilder;
import software.wings.graphql.schema.type.aggregation.QLTimeFilter;
import software.wings.graphql.schema.type.aggregation.QLTimeOperator;
import software.wings.graphql.schema.type.aggregation.QLTimeSeriesAggregation;
import software.wings.graphql.schema.type.aggregation.QLTimeSeriesData;
import software.wings.graphql.schema.type.aggregation.QLTimeSeriesData.QLTimeSeriesDataBuilder;
import software.wings.graphql.schema.type.aggregation.QLTimeSeriesDataPoint;
import software.wings.graphql.schema.type.aggregation.QLTimeSeriesDataPoint.QLTimeSeriesDataPointBuilder;
import software.wings.graphql.schema.type.aggregation.deployment.QLDeploymentAggregation;
import software.wings.graphql.schema.type.aggregation.deployment.QLDeploymentEntityAggregation;
import software.wings.graphql.schema.type.aggregation.deployment.QLDeploymentFilter;
import software.wings.graphql.schema.type.aggregation.deployment.QLDeploymentFilterType;
import software.wings.graphql.schema.type.aggregation.deployment.QLDeploymentSortCriteria;
import software.wings.graphql.schema.type.aggregation.deployment.QLDeploymentSortType;
import software.wings.graphql.schema.type.aggregation.deployment.QLDeploymentTagAggregation;
import software.wings.graphql.schema.type.aggregation.deployment.QLDeploymentTagFilter;
import software.wings.graphql.schema.type.aggregation.deployment.QLDeploymentTagType;
import software.wings.graphql.schema.type.aggregation.environment.QLEnvironmentTypeFilter;
import software.wings.graphql.utils.nameservice.NameService;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;

@Slf4j
public class DeploymentStatsDataFetcher extends AbstractStatsDataFetcherWithTags<QLDeploymentAggregationFunction,
    QLDeploymentFilter, QLDeploymentAggregation, QLDeploymentSortCriteria, QLDeploymentTagType,
    QLDeploymentTagAggregation, QLDeploymentEntityAggregation> {
  @Inject TimeScaleDBService timeScaleDBService;
  @Inject QLStatsHelper statsHelper;
  @Inject ExecutionQueryHelper executionQueryHelper;
  @Inject TagHelper tagHelper;
  private DeploymentTableSchema schema = new DeploymentTableSchema();
  private static final long weekOffset = 7 * 24 * 60 * 60 * 1000;

  @Override
  protected QLData fetch(String accountId, QLDeploymentAggregationFunction aggregateFunction,
      List<QLDeploymentFilter> filters, List<QLDeploymentAggregation> groupBy,
      List<QLDeploymentSortCriteria> sortCriteria) {
    try {
      if (timeScaleDBService.isValid()) {
        return getData(accountId, aggregateFunction, filters, groupBy, sortCriteria);
      } else {
        throw new InvalidRequestException("Cannot process request");
      }
    } catch (Exception e) {
      throw new InvalidRequestException("Error while fetching deployment data", e);
    }
  }

  protected QLData getData(@NotNull String accountId, QLDeploymentAggregationFunction aggregateFunction,
      List<QLDeploymentFilter> filters, List<QLDeploymentAggregation> groupByList,
      List<QLDeploymentSortCriteria> sortCriteria) {
    DeploymentStatsQueryMetaData queryData;
    boolean successful = false;
    int retryCount = 0;
    List<QLDeploymentEntityAggregation> groupByEntityList = getGroupByEntity(groupByList);
    List<QLDeploymentTagAggregation> groupByTagList = getGroupByTag(groupByList);
    QLTimeSeriesAggregation groupByTime = getGroupByTime(groupByList);

    groupByEntityList = getGroupByEntityListFromTags(groupByList, groupByEntityList, groupByTagList, groupByTime);

    preValidateInput(groupByEntityList, groupByTime);

    queryData = formQuery(accountId, aggregateFunction, filters, groupByEntityList, groupByTime, sortCriteria);

    while (!successful && retryCount < MAX_RETRY) {
      ResultSet resultSet = null;
      try (Connection connection = timeScaleDBService.getDBConnection();
           Statement statement = connection.createStatement()) {
        long startTime = System.currentTimeMillis();
        resultSet = statement.executeQuery(queryData.getQuery());
        successful = true;
        long endTime = System.currentTimeMillis();
        if (endTime - startTime > 2000L) {
          logger.warn("TIMESCALEDB : query taking [{}] ms, [{}]", endTime - startTime, queryData.getQuery());
        }
        switch (queryData.getResultType()) {
          case SINGLE_POINT:
            return generateSinglePointData(queryData, resultSet);
          case TIME_SERIES:
            return generateTimeSeriesData(queryData, resultSet);
          case AGGREGATE_DATA:
            return generateAggregateData(queryData, resultSet);
          case STACKED_TIME_SERIES:
            return generateStackedTimeSeriesData(queryData, resultSet);
          case STACKED_BAR_CHART:
            return generateStackedBarChartData(queryData, resultSet);
          default:
            throw new RuntimeException("Unsupported resultType for type:" + queryData.getResultType());
        }
      } catch (SQLException e) {
        if (retryCount >= MAX_RETRY) {
          logger.error("Failed to execute query=[{}],accountId=[{}]", queryData.getQuery(), accountId, e);
        } else {
          logger.warn("Failed to execute query=[{}],accountId=[{}],retryCount=[{}]", queryData.getQuery(), accountId,
              retryCount);
        }
        retryCount++;
      } finally {
        DBUtils.close(resultSet);
      }
    }
    return null;
  }

  private List<QLDeploymentEntityAggregation> getGroupByEntity(List<QLDeploymentAggregation> groupBy) {
    return groupBy != null ? groupBy.stream()
                                 .filter(g -> g.getEntityAggregation() != null)
                                 .map(QLDeploymentAggregation::getEntityAggregation)
                                 .collect(Collectors.toList())
                           : null;
  }

  private QLTimeSeriesAggregation getGroupByTime(List<QLDeploymentAggregation> groupBy) {
    if (groupBy != null) {
      Optional<QLTimeSeriesAggregation> first = groupBy.stream()
                                                    .filter(g -> g.getTimeAggregation() != null)
                                                    .map(QLDeploymentAggregation::getTimeAggregation)
                                                    .findFirst();
      if (first.isPresent()) {
        return first.get();
      }
    }
    return null;
  }

  protected QLStackedData generateStackedBarChartData(DeploymentStatsQueryMetaData queryData, ResultSet resultSet)
      throws SQLException {
    Map<String, List<QLIntermediateStackDataPoint>> qlTimeDataPointMap = new LinkedHashMap<>();
    while (resultSet != null && resultSet.next()) {
      QLIntermediateStackDataPointBuilder dataPointBuilder = QLIntermediateStackDataPoint.builder();
      for (DeploymentMetaDataFields field : queryData.getFieldNames()) {
        switch (field.getDataType()) {
          case INTEGER:
            dataPointBuilder.value(resultSet.getInt(field.getFieldName()));
            break;
          case LONG:
            dataPointBuilder.value(resultSet.getLong(field.getFieldName()));
            break;
          case STRING:
            final String entityId = resultSet.getString(field.getFieldName());
            if (queryData.getGroupByFields().get(1).equals(field)) {
              dataPointBuilder.key(buildQLReference(field, entityId));
            } else {
              dataPointBuilder.groupBy1(entityId);
            }
            break;
          default:
            logger.info("Unsupported data type : " + field.getDataType());
        }
      }

      QLIntermediateStackDataPoint dataPoint = dataPointBuilder.build();
      qlTimeDataPointMap.computeIfAbsent(dataPoint.getGroupBy1(), k -> new ArrayList<>());
      qlTimeDataPointMap.get(dataPoint.getGroupBy1()).add(dataPoint);
    }

    return generateStackedChart(queryData, queryData.getGroupByFields().get(0), qlTimeDataPointMap);
  }

  private QLStackedData generateStackedChart(DeploymentStatsQueryMetaData queryData, DeploymentMetaDataFields groupBy1,
      Map<String, List<QLIntermediateStackDataPoint>> intermediateMap) {
    QLStackedDataBuilder builder = QLStackedData.builder();
    List<QLStackedDataPoint> stackedDataPoints = new ArrayList<>();

    final Map<DeploymentMetaDataFields, String[]> filterValueMap =
        getFilterDeploymentMetaDataField(queryData.getFilters());

    String[] values = filterValueMap.get(groupBy1);
    Set<String> valueSet = values == null ? new HashSet<>() : Sets.newHashSet(values);
    intermediateMap.keySet().forEach(key -> {
      if (values == null || valueSet.contains(key)) {
        QLStackedDataPointBuilder stackedDataPointBuilder = QLStackedDataPoint.builder();
        List<QLDataPoint> dataPoints = intermediateMap.get(key)
                                           .stream()
                                           .map(QLIntermediateStackDataPoint::getDataPoint)
                                           .collect(Collectors.toList());

        dataPoints = filterQLDataPoints(dataPoints, queryData.getFilters(), queryData.getGroupByFields().get(1));

        stackedDataPointBuilder.values(dataPoints).key(buildQLReference(groupBy1, key));
        stackedDataPoints.add(stackedDataPointBuilder.build());
      }
    });

    boolean countSort = false;
    QLSortOrder countSortOrder = null;
    if (queryData.getSortCriteria() != null) {
      for (QLDeploymentSortCriteria sortCriteria : queryData.getSortCriteria()) {
        if (sortCriteria.getSortType().equals(QLDeploymentSortType.Count)) {
          countSort = true;
          countSortOrder = sortCriteria.getSortOrder();
          stackedDataPoints.sort(new QLStackedDataPointComparator(countSortOrder));
          break;
        }
      }
    }

    return builder.dataPoints(stackedDataPoints).build();
  }

  private class QLStackedDataPointComparator implements Comparator<QLStackedDataPoint> {
    private QLSortOrder sortOrder;

    QLStackedDataPointComparator(QLSortOrder sortOrder) {
      this.sortOrder = sortOrder;
    }

    @Override
    public int compare(QLStackedDataPoint o1, QLStackedDataPoint o2) {
      Integer o1Count;
      Integer o2Count;
      if (o1.getValues().get(0).getValue() instanceof Integer) {
        o1Count = handleInteger(o1);
        o2Count = handleInteger(o2);
      } else {
        o1Count = handleLong(o1);
        o2Count = handleLong(o2);
      }
      switch (sortOrder) {
        case ASCENDING:
          return o1Count - o2Count;
        case DESCENDING:
        default:
          return o2Count - o1Count;
      }
    }

    @org.jetbrains.annotations.NotNull
    private Integer handleInteger(QLStackedDataPoint o1) {
      Integer o1Count = 0;
      for (QLDataPoint dataPoint : o1.getValues()) {
        o1Count = (Integer) dataPoint.getValue() + o1Count;
      }
      return o1Count;
    }

    @org.jetbrains.annotations.NotNull
    private Integer handleLong(QLStackedDataPoint o1) {
      Long o1Count = 0L;
      for (QLDataPoint dataPoint : o1.getValues()) {
        o1Count = (Long) dataPoint.getValue() + o1Count;
      }
      return o1Count.intValue();
    }
  }

  private QLReference buildQLReference(DeploymentMetaDataFields field, String key) {
    return QLReference.builder().type(field.getFieldName()).id(key).name(statsHelper.getEntityName(field, key)).build();
  }

  protected QLStackedTimeSeriesData generateStackedTimeSeriesData(
      DeploymentStatsQueryMetaData queryData, ResultSet resultSet) throws SQLException {
    Map<Long, List<QLTimeDataPoint>> qlTimeDataPointMap = new LinkedHashMap<>();
    while (resultSet != null && resultSet.next()) {
      QLTimeDataPointBuilder dataPointBuilder = QLTimeDataPoint.builder();
      for (DeploymentMetaDataFields field : queryData.getFieldNames()) {
        switch (field.getDataType()) {
          case INTEGER:
            dataPointBuilder.value(resultSet.getInt(field.getFieldName()));
            break;
          case LONG:
            dataPointBuilder.value(resultSet.getLong(field.getFieldName()));
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
            throw new RuntimeException("UnsupportedType " + field.getDataType());
        }
      }

      QLTimeDataPoint dataPoint = dataPointBuilder.build();
      qlTimeDataPointMap.computeIfAbsent(dataPoint.getTime(), k -> new ArrayList<>());
      qlTimeDataPointMap.get(dataPoint.getTime()).add(dataPoint);
    }

    return prepareStackedTimeSeriesData(queryData, qlTimeDataPointMap);
  }

  private QLStackedTimeSeriesData prepareStackedTimeSeriesData(
      DeploymentStatsQueryMetaData queryData, Map<Long, List<QLTimeDataPoint>> qlTimeDataPointMap) {
    QLStackedTimeSeriesDataBuilder timeSeriesDataBuilder = QLStackedTimeSeriesData.builder();
    List<QLStackedTimeSeriesDataPoint> timeSeriesDataPoints = new ArrayList<>();
    qlTimeDataPointMap.keySet().forEach(time -> {
      List<QLTimeDataPoint> timeDataPoints = qlTimeDataPointMap.get(time);
      QLStackedTimeSeriesDataPointBuilder builder = QLStackedTimeSeriesDataPoint.builder();
      List<QLDataPoint> dataPoints =
          timeDataPoints.parallelStream().map(QLTimeDataPoint::getQLDataPoint).collect(Collectors.toList());
      dataPoints = filterQLDataPoints(dataPoints, queryData.getFilters(), queryData.getGroupByFields().get(0));
      builder.values(dataPoints).time(time);
      timeSeriesDataPoints.add(builder.build());
    });
    return timeSeriesDataBuilder.data(timeSeriesDataPoints).build();
  }

  protected QLTimeSeriesData generateTimeSeriesData(DeploymentStatsQueryMetaData queryData, ResultSet resultSet)
      throws SQLException {
    QLTimeSeriesDataBuilder builder = QLTimeSeriesData.builder();
    List<QLTimeSeriesDataPoint> dataPoints = new ArrayList<>();
    while (resultSet != null && resultSet.next()) {
      QLTimeSeriesDataPointBuilder dataPointBuilder = QLTimeSeriesDataPoint.builder();
      for (DeploymentMetaDataFields field : queryData.getFieldNames()) {
        switch (field.getDataType()) {
          case INTEGER:
            dataPointBuilder.value(resultSet.getInt(field.getFieldName()));
            break;
          case LONG:
            dataPointBuilder.value(resultSet.getLong(field.getFieldName()));
            break;
          case TIMESTAMP:
            dataPointBuilder.time(resultSet.getTimestamp(field.getFieldName(), utils.getDefaultCalendar()).getTime());
            break;
          default:
            throw new RuntimeException("UnsupportedType " + field.getDataType());
        }
      }
      dataPoints.add(dataPointBuilder.build());
    }
    return builder.dataPoints(dataPoints).build();
  }

  protected QLAggregatedData generateAggregateData(DeploymentStatsQueryMetaData queryData, ResultSet resultSet)
      throws SQLException {
    QLAggregatedDataBuilder builder = QLAggregatedData.builder();
    List<QLDataPoint> dataPoints = new ArrayList();
    while (resultSet != null && resultSet.next()) {
      QLDataPointBuilder dataPoint = QLDataPoint.builder();
      for (DeploymentMetaDataFields field : queryData.getFieldNames()) {
        switch (field.getDataType()) {
          case INTEGER:
            dataPoint.value(resultSet.getInt(field.getFieldName()));
            break;
          case LONG:
            dataPoint.value(resultSet.getLong(field.getFieldName()));
            break;
          case STRING:
            final String entityId = resultSet.getString(field.getFieldName());
            dataPoint.key(buildQLReference(field, entityId));
            break;
          default:
            throw new RuntimeException("UnsupportedType " + field.getDataType());
        }
      }
      dataPoints.add(dataPoint.build());
    }

    dataPoints = filterQLDataPoints(dataPoints, queryData.getFilters(), queryData.getGroupByFields().get(0));

    return builder.dataPoints(dataPoints).build();
  }

  private List<QLDataPoint> filterQLDataPoints(
      List<QLDataPoint> dataPoints, List<QLDeploymentFilter> filters, DeploymentMetaDataFields groupBy) {
    if (groupBy != null) {
      Map<DeploymentMetaDataFields, String[]> filterValueMap = getFilterDeploymentMetaDataField(filters);
      String[] values = filterValueMap.get(groupBy);
      if (values != null) {
        final Set valueSet = Sets.newHashSet(values);
        dataPoints.removeIf(dataPoint -> !valueSet.contains(dataPoint.getKey().getId()));
      }
    }

    return dataPoints;
  }

  protected QLSinglePointData generateSinglePointData(DeploymentStatsQueryMetaData queryData, ResultSet resultSet)
      throws SQLException {
    QLSinglePointDataBuilder builder = QLSinglePointData.builder();
    while (resultSet != null && resultSet.next()) {
      DeploymentMetaDataFields queryField = queryData.getFieldNames().get(0);
      Number data;
      switch (queryField.getDataType()) {
        case INTEGER:
          data = resultSet.getInt(queryField.getFieldName());
          break;
        case LONG:
          data = resultSet.getLong(queryField.getFieldName());
          break;
        default:
          throw new RuntimeException("Single Data Type data type not supported " + queryField.getDataType());
      }
      builder.dataPoint(
          QLDataPoint.builder()
              .value(data)
              .key(QLReference.builder().type(getEntityType()).id(getEntityType()).name(getEntityType()).build())
              .build());
    }
    return builder.build();
  }

  protected DeploymentStatsQueryMetaData formQuery(String accountId, QLDeploymentAggregationFunction aggregateFunction,
      List<QLDeploymentFilter> filters, List<QLDeploymentEntityAggregation> groupBy,
      QLTimeSeriesAggregation groupByTime, List<QLDeploymentSortCriteria> sortCriteria) {
    DeploymentStatsQueryMetaDataBuilder queryMetaDataBuilder = DeploymentStatsQueryMetaData.builder();
    SelectQuery selectQuery = new SelectQuery();

    ResultType resultType;
    if (isValidGroupBy(groupBy) && groupBy.size() == 1 && isValidGroupByTime(groupByTime)) {
      resultType = ResultType.STACKED_TIME_SERIES;
    } else if (isValidGroupByTime(groupByTime) && !isValidGroupBy(groupBy)) {
      resultType = ResultType.TIME_SERIES;
    } else if (isValidGroupBy(groupBy) && groupBy.size() == 1) {
      resultType = ResultType.AGGREGATE_DATA;
    } else if (isValidGroupBy(groupBy) && groupBy.size() == 2) {
      resultType = ResultType.STACKED_BAR_CHART;
    } else {
      resultType = ResultType.SINGLE_POINT;
    }

    queryMetaDataBuilder.resultType(resultType);
    List<DeploymentMetaDataFields> fieldNames = new ArrayList<>();
    List<DeploymentMetaDataFields> groupByFields = new ArrayList<>();

    if (aggregateFunction == null || aggregateFunction.getCount() != null) {
      selectQuery.addCustomColumns(
          Converter.toColumnSqlObject(FunctionCall.countAll(), DeploymentMetaDataFields.COUNT.getFieldName()));
      fieldNames.add(DeploymentMetaDataFields.COUNT);
    }
    if (aggregateFunction != null && aggregateFunction.getDuration() != null) {
      FunctionCall functionCall = getFunctionCall(aggregateFunction.getDuration());
      selectQuery.addCustomColumns(Converter.toColumnSqlObject(
          functionCall.addColumnParams(schema.getDuration()), DeploymentMetaDataFields.DURATION.getFieldName()));
      fieldNames.add(DeploymentMetaDataFields.DURATION);
    }

    if (aggregateFunction != null && aggregateFunction.getRollbackDuration() != null) {
      FunctionCall functionCall = getFunctionCall(aggregateFunction.getRollbackDuration());
      selectQuery.addCustomColumns(
          Converter.toColumnSqlObject(functionCall.addColumnParams(schema.getRollbackDuration()),
              DeploymentMetaDataFields.ROLLBACK_DURATION.getFieldName()));
      fieldNames.add(DeploymentMetaDataFields.ROLLBACK_DURATION);

      if (filters == null) {
        filters = new ArrayList<>();
      }

      // If the metric is 'rollback duration', add a filter to only include the entries that had rollback duration set.
      filters.add(
          QLDeploymentFilter.builder()
              .rollbackDuration(
                  QLNumberFilter.builder().operator(QLNumberOperator.GREATER_THAN).values(new Number[] {0}).build())
              .build());
    }

    selectQuery.addCustomFromTable(schema.getDeploymentTable());

    if (!Lists.isNullOrEmpty(filters)) {
      filters = processFilterForTags(accountId, filters);
      decorateQueryWithFilters(selectQuery, filters);
    }

    validateTimeFilters(filters, selectQuery);

    if (isValidGroupByTime(groupByTime)) {
      decorateQueryWithGroupByTime(fieldNames, selectQuery, groupByTime);
    }

    if (isValidGroupBy(groupBy)) {
      decorateQueryWithGroupBy(fieldNames, selectQuery, groupBy, groupByFields);
    }

    addAccountFilter(selectQuery, accountId);
    addParentIdFilter(selectQuery);

    List<QLDeploymentSortCriteria> finalSortCriteria = null;
    if (!isValidGroupByTime(groupByTime)) {
      finalSortCriteria = validateAndAddSortCriteria(selectQuery, sortCriteria, fieldNames);
    } else {
      finalSortCriteria = null;
      logger.info("Not adding sortCriteria since it is a timeSeries");
    }

    selectQuery.getWhereClause().setDisableParens(true);
    queryMetaDataBuilder.fieldNames(fieldNames);
    queryMetaDataBuilder.query(selectQuery.toString());
    queryMetaDataBuilder.groupByFields(groupByFields);
    queryMetaDataBuilder.sortCriteria(finalSortCriteria);
    queryMetaDataBuilder.filters(filters);
    return queryMetaDataBuilder.build();
  }

  private void validateTimeFilters(List<QLDeploymentFilter> filters, SelectQuery selectQuery) {
    TimeFilterQueryResult timeFilterQueryResult = checkTimeFilters(filters);
    if (!timeFilterQueryResult.isT1Present() && !timeFilterQueryResult.isT2Present()) {
      /**
       * No timeFilter present anywhere
       */
      long currentTime = System.currentTimeMillis();
      long timeWeekAgo = currentTime - weekOffset;
      logger.info("T1 and T2, both not present, adding T1:[{}], T2:[{}]", new Date(timeWeekAgo), new Date(currentTime));
      Filter t1Filter = QLTimeFilter.builder().operator(QLTimeOperator.AFTER).value(timeWeekAgo).build();
      Filter t2Filter = QLTimeFilter.builder().operator(QLTimeOperator.BEFORE).value(currentTime).build();
      addSimpleTimeFilter(selectQuery, t1Filter, QLDeploymentFilterType.EndTime);
      addSimpleTimeFilter(selectQuery, t2Filter, QLDeploymentFilterType.EndTime);
    } else if (!timeFilterQueryResult.isT1Present()) {
      /**
       * No T1 present, only T2 present
       */
      long t1Time = timeFilterQueryResult.getT2() - weekOffset;
      logger.info("T1 not present, adding T1:[{}]", new Date(t1Time));
      Filter t1Filter = QLTimeFilter.builder().operator(QLTimeOperator.AFTER).value(t1Time).build();
      addSimpleTimeFilter(selectQuery, t1Filter, timeFilterQueryResult.getT2Type());
    } else if (!timeFilterQueryResult.isT2Present()) {
      /**
       * No T2 present, only T1 present
       */
      long t2Time = timeFilterQueryResult.getT1() + weekOffset;
      logger.info("T2 not present, adding T2:[{}]", new Date(t2Time));
      Filter t2Filter = QLTimeFilter.builder().operator(QLTimeOperator.BEFORE).value(t2Time).build();
      addSimpleTimeFilter(selectQuery, t2Filter, timeFilterQueryResult.getT1Type());
    }
  }

  private TimeFilterQueryResult checkTimeFilters(List<QLDeploymentFilter> filters) {
    TimeFilterQueryResult result = new TimeFilterQueryResult();
    if (filters != null) {
      filters.forEach(filter -> {
        evaluateTimeFilter(result, filter.getEndTime(), QLDeploymentFilterType.EndTime);
        evaluateTimeFilter(result, filter.getStartTime(), QLDeploymentFilterType.StartTime);
      });
    }
    return result;
  }

  private TimeFilterQueryResult evaluateTimeFilter(
      TimeFilterQueryResult result, QLTimeFilter timeOperator, QLDeploymentFilterType type) {
    if (timeOperator != null) {
      if (timeOperator.getOperator().equals(QLTimeOperator.BEFORE)) {
        result.setT2Present(true);
        if (result.getT2() == null) {
          result.setT2(timeOperator.getValue().longValue());
          result.setT2Type(type);
        }
      } else if (timeOperator.getOperator().equals(QLTimeOperator.AFTER)) {
        result.setT1Present(true);
        if (result.getT1() == null) {
          result.setT1(timeOperator.getValue().longValue());
          result.setT1Type(type);
        }
      }
    }
    return result;
  }

  @Data
  class TimeFilterQueryResult {
    /**
     * Not using startTime and endTime in this datastructure to prevent any confusion from
     * startTime and endTime in the Deployment table
     * t1 -> startTime of the filter
     * t2 -> endTime of the filter
     */
    QLDeploymentFilterType t1Type;
    QLDeploymentFilterType t2Type;
    boolean t1Present;
    boolean t2Present;
    Long t1;
    Long t2;
  }

  private void addParentIdFilter(SelectQuery selectQuery) {
    selectQuery.addCondition(UnaryCondition.isNull(schema.getParentExecution()));
  }

  private List<QLDeploymentSortCriteria> validateAndAddSortCriteria(
      SelectQuery selectQuery, List<QLDeploymentSortCriteria> sortCriteria, List<DeploymentMetaDataFields> fieldNames) {
    if (isEmpty(sortCriteria)) {
      return new ArrayList<>();
    }

    sortCriteria.removeIf(qlDeploymentSortCriteria
        -> qlDeploymentSortCriteria.getSortOrder() == null
            || !fieldNames.contains(qlDeploymentSortCriteria.getSortType().getDeploymentMetadata()));

    List<DeploymentMetaDataFields> sortFields =
        sortCriteria.stream().map(s -> s.getSortType().getDeploymentMetadata()).collect(Collectors.toList());

    if (EmptyPredicate.isNotEmpty(sortCriteria)) {
      sortCriteria.forEach(s -> addOrderBy(selectQuery, s));
    }
    return sortCriteria;
  }

  private void preValidateInput(
      List<QLDeploymentEntityAggregation> groupByEntity, QLTimeSeriesAggregation groupByTime) {
    if (isValidGroupBy(groupByEntity) && groupByEntity.size() > 2) {
      throw new RuntimeException("More than 2 group bys not supported + " + groupByEntity);
    }

    if (isValidGroupBy(groupByEntity) && isValidGroupByTime(groupByTime) && groupByEntity.size() > 1) {
      throw new RuntimeException("For a time series aggregation, only a single groupByEntity is allowed");
    }
  }

  private void addAccountFilter(SelectQuery selectQuery, String accountId) {
    selectQuery.addCondition(BinaryCondition.equalTo(schema.getAccountId(), accountId));
  }

  private void decorateQueryWithFilters(SelectQuery selectQuery, List<QLDeploymentFilter> filters) {
    for (QLDeploymentFilter filter : filters) {
      Set<QLDeploymentFilterType> filterTypes = QLDeploymentFilter.getFilterTypes(filter);
      for (QLDeploymentFilterType type : filterTypes) {
        if (type.getMetaDataFields().getFilterKind().equals(QLFilterKind.SIMPLE)) {
          decorateSimpleFilter(selectQuery, filter, type);
        } else if (type.getMetaDataFields().getFilterKind().equals(QLFilterKind.ARRAY)) {
          decorateArrayFilter(selectQuery, filter, type);
        } else {
          logger.error("Failed to apply filter :[{}]", filter);
        }
      }
    }
  }

  private List<QLDeploymentFilter> processFilterForTags(String accountId, List<QLDeploymentFilter> filters) {
    List<QLDeploymentFilter> newList = new ArrayList<>();
    for (QLDeploymentFilter filter : filters) {
      Set<QLDeploymentFilterType> filterTypes = QLDeploymentFilter.getFilterTypes(filter);
      for (QLDeploymentFilterType type : filterTypes) {
        if (type.equals(QLDeploymentFilterType.Tag)) {
          QLDeploymentTagFilter tagFilter = filter.getTag();

          if (tagFilter != null) {
            Set<String> entityIds = tagHelper.getEntityIdsFromTags(
                accountId, tagFilter.getTags(), getEntityType(tagFilter.getEntityType()));
            if (isNotEmpty(entityIds)) {
              switch (tagFilter.getEntityType()) {
                case APPLICATION:
                  newList.add(QLDeploymentFilter.builder()
                                  .application(QLIdFilter.builder()
                                                   .operator(QLIdOperator.IN)
                                                   .values(entityIds.toArray(new String[0]))
                                                   .build())
                                  .build());
                  break;
                case SERVICE:
                  newList.add(QLDeploymentFilter.builder()
                                  .service(QLIdFilter.builder()
                                               .operator(QLIdOperator.IN)
                                               .values(entityIds.toArray(new String[0]))
                                               .build())
                                  .build());
                  break;
                case ENVIRONMENT:
                  newList.add(QLDeploymentFilter.builder()
                                  .environment(QLIdFilter.builder()
                                                   .operator(QLIdOperator.IN)
                                                   .values(entityIds.toArray(new String[0]))
                                                   .build())
                                  .build());
                  break;
                default:
                  logger.error("EntityType {} not supported in query", tagFilter.getEntityType());
                  throw new InvalidRequestException("Error while compiling query", WingsException.USER);
              }
            }
          }
        } else {
          newList.add(filter);
        }
      }
    }
    return newList;
  }

  private void addSimpleTimeFilter(SelectQuery selectQuery, Filter filter, QLDeploymentFilterType type) {
    DbColumn key = getFilterKey(type);
    QLTimeFilter timeFilter = (QLTimeFilter) filter;
    switch (timeFilter.getOperator()) {
      case BEFORE:
        selectQuery.addCondition(BinaryCondition.lessThanOrEq(key, Instant.ofEpochMilli((Long) timeFilter.getValue())));
        break;
      case AFTER:
        selectQuery.addCondition(
            BinaryCondition.greaterThanOrEq(key, Instant.ofEpochMilli((Long) timeFilter.getValue())));
        break;
      default:
        throw new RuntimeException("Invalid TimeFilter operator: " + filter.getOperator());
    }
  }

  private void decorateArrayFilter(SelectQuery selectQuery, QLDeploymentFilter filter, QLDeploymentFilterType type) {
    /**
     * We only support id based arrays over here
     */
    Filter f = QLDeploymentFilter.getFilter(type, filter);
    if (isIdFilter(f)) {
      addArrayIdFilter(selectQuery, f, type);
    } else if (isStringFilter(f)) {
      addArrayStringFilter(selectQuery, f, type);
    } else if (isEnvTypeFilter(f)) {
      addEnvTypeFilter(selectQuery, (QLEnvironmentTypeFilter) f, type);
    }
  }

  private void addArrayIdFilter(SelectQuery selectQuery, Filter filter, QLDeploymentFilterType type) {
    DbColumn key = getFilterKey(type);
    QLIdFilter idFilter = (QLIdFilter) filter;
    if (isEmpty(filter.getValues())) {
      throw new RuntimeException("No values are provided for IdFilter" + filter);
    }
    CustomCondition[] customConditions = new CustomCondition[idFilter.getValues().length];
    for (int i = 0; i < idFilter.getValues().length; i++) {
      String filterValue = "{" + idFilter.getValues()[i] + "}";
      switch (idFilter.getOperator()) {
        case IN:
        case EQUALS:
          customConditions[i] =
              new CustomCondition(schema.getDeploymentTable().getAlias() + "." + key.getColumnNameSQL() + " @>"
                  + "'" + filterValue + "'");
          break;
        default:
          throw new RuntimeException("Unsupported operator for ArrayIdFilter" + idFilter.getOperator());
      }
    }
    selectQuery.addCondition(new ComboCondition(Op.OR, customConditions).setDisableParens(false));
  }

  private void addEnvTypeFilter(SelectQuery selectQuery, QLEnvironmentTypeFilter filter, QLDeploymentFilterType type) {
    DbColumn key = getFilterKey(type);
    QLEnumOperator operator = filter.getOperator();
    if (isEmpty(filter.getValues())) {
      throw new RuntimeException("No values are provided for EnvTypeFilter" + operator);
    }
    CustomCondition[] customConditions = new CustomCondition[filter.getValues().length];

    for (int i = 0; i < filter.getValues().length; i++) {
      String filterValue = "{" + filter.getValues()[i] + "}";
      switch (operator) {
        case IN:
        case EQUALS:
          customConditions[i] =
              new CustomCondition(schema.getDeploymentTable().getAlias() + "." + key.getColumnNameSQL() + " @>"
                  + "'" + filterValue + "'");
          break;
        default:
          throw new RuntimeException("Unsupported operator for EnvTypeFilter" + operator);
      }
    }

    selectQuery.addCondition(new ComboCondition(Op.OR, customConditions).setDisableParens(false));
  }

  private void addArrayStringFilter(SelectQuery selectQuery, Filter filter, QLDeploymentFilterType type) {
    DbColumn key = getFilterKey(type);
    QLStringFilter stringFilter = (QLStringFilter) filter;
    if (isEmpty(filter.getValues())) {
      throw new RuntimeException("No values are provided for StringFilter" + filter);
    }
    CustomCondition[] customConditions = new CustomCondition[filter.getValues().length];
    for (int i = 0; i < filter.getValues().length; i++) {
      String filterValue = "{" + filter.getValues()[i] + "}";
      switch (stringFilter.getOperator()) {
        case IN:
        case EQUALS:
          customConditions[i] =
              new CustomCondition(schema.getDeploymentTable().getAlias() + "." + key.getColumnNameSQL() + " @>"
                  + "'" + filterValue + "'");
          break;
        default:
          throw new RuntimeException("Unsupported operator for ArrayStringFilter" + stringFilter.getOperator());
      }
    }
    selectQuery.addCondition(new ComboCondition(Op.OR, customConditions).setDisableParens(false));
  }

  private void decorateSimpleFilter(SelectQuery selectQuery, QLDeploymentFilter filter, QLDeploymentFilterType type) {
    Filter f = QLDeploymentFilter.getFilter(type, filter);
    if (checkFilter(f)) {
      if (isIdFilter(f)) {
        addSimpleIdOperator(selectQuery, f, type);
      } else if (isNumberFilter(f)) {
        addSimpleNumberFilter(selectQuery, f, type);
      } else if (isTimeFilter(f)) {
        addSimpleTimeFilter(selectQuery, f, type);
      } else if (isStringFilter(f)) {
        addSimpleStringOperator(selectQuery, f, type);
      }
    } else {
      logger.error("Not adding filter since it is not valid " + f);
    }
  }

  private boolean isIdFilter(Filter f) {
    return f instanceof QLIdFilter;
  }

  private boolean isStringFilter(Filter f) {
    return f instanceof QLStringFilter;
  }

  private boolean isEnvTypeFilter(Filter f) {
    return f instanceof QLEnvironmentTypeFilter;
  }

  private boolean isNumberFilter(Filter f) {
    return f instanceof QLNumberFilter;
  }

  private boolean isTimeFilter(Filter f) {
    return f instanceof QLTimeFilter;
  }

  private boolean checkFilter(Filter f) {
    return f.getOperator() != null && EmptyPredicate.isNotEmpty(f.getValues());
  }

  private void addSimpleNumberFilter(SelectQuery selectQuery, Filter filter, QLDeploymentFilterType type) {
    DbColumn key = getFilterKey(type);
    QLNumberOperator operator = (QLNumberOperator) filter.getOperator();

    switch (operator) {
      case EQUALS:
        selectQuery.addCondition(BinaryCondition.equalTo(key, filter.getValues()[0]));
        break;
      case NOT_EQUALS:
        selectQuery.addCondition(BinaryCondition.notEqualTo(key, filter.getValues()[0]));
        break;
      case IN:
        selectQuery.addCondition(new InCondition(key, (Object[]) filter.getValues()));
        break;
      case LESS_THAN:
        selectQuery.addCondition(BinaryCondition.lessThan(key, filter.getValues()[0]));
        break;
      case LESS_THAN_OR_EQUALS:
        selectQuery.addCondition(BinaryCondition.lessThanOrEq(key, filter.getValues()[0]));
        break;
      case GREATER_THAN:
        selectQuery.addCondition(BinaryCondition.greaterThan(key, filter.getValues()[0]));
        break;
      case GREATER_THAN_OR_EQUALS:
        selectQuery.addCondition(BinaryCondition.greaterThanOrEq(key, filter.getValues()[0]));
        break;
      default:
        throw new RuntimeException("String simple operator not supported" + filter.getOperator());
    }
  }

  private void addSimpleStringOperator(SelectQuery selectQuery, Filter filter, QLDeploymentFilterType type) {
    DbColumn key = getFilterKey(type);
    QLStringOperator operator = (QLStringOperator) filter.getOperator();
    QLStringOperator finalOperator = operator;
    if (filter.getValues().length > 0) {
      switch (operator) {
        case EQUALS:
          finalOperator = QLStringOperator.IN;
          logger.info("Changing simpleStringOperator from [{}] to [{}]", operator, finalOperator);
          break;
        default:
          finalOperator = operator;
      }
    }
    switch (finalOperator) {
      case EQUALS:
        selectQuery.addCondition(BinaryCondition.equalTo(key, filter.getValues()[0]));
        break;
      case IN:
        selectQuery.addCondition(new InCondition(key, (Object[]) filter.getValues()));
        break;
      default:
        throw new RuntimeException("String simple operator not supported" + operator);
    }
  }

  private void addSimpleIdOperator(SelectQuery selectQuery, Filter filter, QLDeploymentFilterType type) {
    DbColumn key = getFilterKey(type);
    QLIdOperator operator = (QLIdOperator) filter.getOperator();
    QLIdOperator finalOperator = operator;
    if (filter.getValues().length > 0) {
      switch (operator) {
        case EQUALS:
          finalOperator = QLIdOperator.IN;
          logger.info("Changing simpleStringOperator from [{}] to [{}]", operator, finalOperator);
          break;
        default:
          finalOperator = operator;
      }
    }
    switch (finalOperator) {
      case EQUALS:
        selectQuery.addCondition(BinaryCondition.equalTo(key, filter.getValues()[0]));
        break;
      case IN:
        selectQuery.addCondition(new InCondition(key, (Object[]) filter.getValues()));
        break;
      default:
        throw new RuntimeException("String simple operator not supported" + operator);
    }
  }

  private void addOrderBy(SelectQuery selectQuery, QLDeploymentSortCriteria sortCriteria) {
    QLDeploymentSortType sortType = sortCriteria.getSortType();
    OrderObject.Dir dir = sortCriteria.getSortOrder() == QLSortOrder.ASCENDING ? Dir.ASCENDING : Dir.DESCENDING;
    switch (sortType) {
      case Duration:
        selectQuery.addCustomOrdering(DeploymentMetaDataFields.DURATION.name(), dir);
        break;
      case Count:
        selectQuery.addCustomOrdering(DeploymentMetaDataFields.COUNT.name(), dir);
        break;
      case RollbackDuration:
        selectQuery.addCustomOrdering(DeploymentMetaDataFields.ROLLBACK_DURATION.name(), dir);
        break;
      default:
        throw new RuntimeException("Order type not supported " + sortType);
    }
  }

  private Map<DeploymentMetaDataFields, String[]> getFilterDeploymentMetaDataField(List<QLDeploymentFilter> filters) {
    Map<DeploymentMetaDataFields, String[]> filterMap = new HashMap<>();
    for (QLDeploymentFilter filter : filters) {
      if (filter.getService() != null) {
        filterMap.put(DeploymentMetaDataFields.SERVICEID, filter.getService().getValues());
      }

      if (filter.getEnvironment() != null) {
        filterMap.put(DeploymentMetaDataFields.ENVID, filter.getEnvironment().getValues());
      }
      if (filter.getEnvironmentType() != null) {
        filterMap.put(DeploymentMetaDataFields.ENVTYPE,
            Arrays.asList(filter.getEnvironmentType().getValues())
                .stream()
                .map(Enum::name)
                .collect(Collectors.toList())
                .toArray(new String[0]));
      }

      if (filter.getCloudProvider() != null) {
        filterMap.put(DeploymentMetaDataFields.CLOUDPROVIDERID, filter.getCloudProvider().getValues());
      }
    }

    return filterMap;
  }

  /**
   *
   *  EXECUTIONID TEXT NOT NULL,
   * 	STARTTIME TIMESTAMP NOT NULL,
   * 	ENDTIME TIMESTAMP NOT NULL,
   * 	ACCOUNTID TEXT NOT NULL,
   * 	APPID TEXT NOT NULL,
   * 	TRIGGERED_BY TEXT,
   * 	TRIGGER_ID TEXT,
   * 	STATUS VARCHAR(20),
   * 	SERVICES TEXT[],
   * 	WORKFLOWS TEXT[],
   * 	CLOUDPROVIDERS TEXT[],
   * 	ENVIRONMENTS TEXT[],
   * 	PIPELINE TEXT,
   * 	DURATION BIGINT NOT NULL,
   * 	ARTIFACTS TEXT[],
   * 	ROLLBACK_DURATION BIGINT
   **
   * @param type
   * @return
   */
  private DbColumn getFilterKey(QLDeploymentFilterType type) {
    switch (type) {
      case StartTime:
        return schema.getStartTime();
      case EndTime:
        return schema.getEndTime();
      case Application:
        return schema.getAppId();
      case Trigger:
        return schema.getTriggerId();
      case TriggeredBy:
        return schema.getTriggeredBy();
      case Status:
        return schema.getStatus();
      case Service:
        return schema.getServices();
      case Workflow:
        return schema.getWorkflows();
      case CloudProvider:
        return schema.getCloudProviders();
      case Environment:
        return schema.getEnvironments();
      case EnvironmentType:
        return schema.getEnvTypes();
      case Pipeline:
        return schema.getPipeline();
      case Duration:
        return schema.getDuration();
      case RollbackDuration:
        return schema.getRollbackDuration();
      default:
        throw new RuntimeException("Filter type not supported " + type);
    }
  }

  private void decorateQueryWithGroupBy(List<DeploymentMetaDataFields> fieldNames, SelectQuery selectQuery,
      List<QLDeploymentEntityAggregation> groupBy, List<DeploymentMetaDataFields> groupByFields) {
    for (QLDeploymentEntityAggregation aggregation : groupBy) {
      if (aggregation.getAggregationKind().equals(QLAggregationKind.SIMPLE)) {
        decorateSimpleGroupBy(fieldNames, selectQuery, aggregation, groupByFields);
      } else if (aggregation.getAggregationKind().equals(QLAggregationKind.ARRAY)) {
        decorateAggregateGroupBy(fieldNames, selectQuery, aggregation, groupByFields);
      }
    }
  }

  private void decorateAggregateGroupBy(List<DeploymentMetaDataFields> fieldNames, SelectQuery selectQuery,
      QLDeploymentEntityAggregation aggregation, List<DeploymentMetaDataFields> groupByFields) {
    /*
     * Service, Environment, CloudProvider, Workflow
     * */
    DeploymentMetaDataFields groupBy = null;
    String unnestField = null;
    switch (aggregation) {
      case Service:
        groupBy = DeploymentMetaDataFields.SERVICEID;
        unnestField = schema.getServices().getName();
        break;
      case Environment:
        groupBy = DeploymentMetaDataFields.ENVID;
        unnestField = schema.getEnvironments().getName();
        break;
      case EnvironmentType:
        groupBy = DeploymentMetaDataFields.ENVTYPE;
        unnestField = schema.getEnvTypes().getName();
        break;
      case CloudProvider:
        groupBy = DeploymentMetaDataFields.CLOUDPROVIDERID;
        unnestField = schema.getCloudProviders().getName();
        break;
      case Workflow:
        groupBy = DeploymentMetaDataFields.WORKFLOWID;
        unnestField = schema.getWorkflows().getName();
        break;
      default:
        throw new RuntimeException("No valid groupBy found");
    }
    String unnestClause = "unnest(" + unnestField + ") " + groupBy;
    selectQuery.addCustomFromTable(new CustomExpression(unnestClause).setDisableParens(true));
    selectQuery.addCustomColumns(new CustomExpression(groupBy).setDisableParens(true));
    selectQuery.addCustomGroupings(groupBy);
    fieldNames.add(groupBy);
    groupByFields.add(groupBy);
  }

  private void decorateSimpleGroupBy(List<DeploymentMetaDataFields> fieldNames, SelectQuery selectQuery,
      QLDeploymentEntityAggregation aggregation, List<DeploymentMetaDataFields> groupByFields) {
    /**
     * Application, Status, Trigger, TriggeredBy, Pipeline
     */
    DbColumn groupBy;
    switch (aggregation) {
      case Application:
        groupBy = schema.getAppId();
        break;
      case Status:
        groupBy = schema.getStatus();
        break;
      case Trigger:
        groupBy = schema.getTriggerId();
        break;
      case TriggeredBy:
        groupBy = schema.getTriggeredBy();
        break;
      case Pipeline:
        groupBy = schema.getPipeline();
        break;
      default:
        throw new RuntimeException("Invalid groupBy clause");
    }
    selectQuery.addColumns(groupBy);
    selectQuery.addGroupings(groupBy);
    fieldNames.add(DeploymentMetaDataFields.valueOf(groupBy.getName()));
    selectQuery.addCondition(UnaryCondition.isNotNull(groupBy));
    groupByFields.add(DeploymentMetaDataFields.valueOf(groupBy.getName()));
  }

  private boolean isValidGroupBy(List<QLDeploymentEntityAggregation> groupBy) {
    return EmptyPredicate.isNotEmpty(groupBy) && groupBy.size() <= 2;
  }

  private void decorateQueryWithGroupByTime(
      List<DeploymentMetaDataFields> fieldNames, SelectQuery selectQuery, QLTimeSeriesAggregation groupByTime) {
    String timeBucket = getGroupByTimeQuery(groupByTime, "endtime");

    selectQuery.addCustomColumns(Converter.toCustomColumnSqlObject(
        new CustomExpression(timeBucket).setDisableParens(true), DeploymentMetaDataFields.TIME_SERIES.getFieldName()));
    selectQuery.addCustomGroupings(DeploymentMetaDataFields.TIME_SERIES.getFieldName());
    selectQuery.addCustomOrdering(DeploymentMetaDataFields.TIME_SERIES.getFieldName(), Dir.ASCENDING);
    fieldNames.add(DeploymentMetaDataFields.TIME_SERIES);
  }

  private boolean isValidGroupByTime(QLTimeSeriesAggregation groupByTime) {
    return groupByTime != null && groupByTime.getTimeAggregationType() != null
        && groupByTime.getTimeAggregationValue() != null;
  }

  private FunctionCall getFunctionCall(QLDurationAggregateOperation operation) {
    switch (operation) {
      case MAX:
        return FunctionCall.max();
      case MIN:
        return FunctionCall.min();
      case AVERAGE:
        return FunctionCall.avg();
      default:
        return FunctionCall.max();
    }
  }

  @Override
  public String getEntityType() {
    return NameService.deployment;
  }

  @Override
  protected QLDeploymentTagAggregation getTagAggregation(QLDeploymentAggregation groupBy) {
    return groupBy.getTagAggregation();
  }

  @Override
  protected EntityType getEntityType(QLDeploymentTagType entityType) {
    return executionQueryHelper.getEntityType(entityType);
  }

  @Override
  protected QLDeploymentEntityAggregation getGroupByEntityFromTag(QLDeploymentTagAggregation groupByTag) {
    switch (groupByTag.getEntityType()) {
      case APPLICATION:
        return QLDeploymentEntityAggregation.Application;
      case SERVICE:
        return QLDeploymentEntityAggregation.Service;
      case ENVIRONMENT:
        return QLDeploymentEntityAggregation.Environment;
      default:
        logger.warn("Unsupported tag entity type {}", groupByTag.getEntityType());
        throw new InvalidRequestException(GENERIC_EXCEPTION_MSG);
    }
  }

  @Override
  protected QLDeploymentEntityAggregation getEntityAggregation(QLDeploymentAggregation groupBy) {
    return groupBy.getEntityAggregation();
  }
}
