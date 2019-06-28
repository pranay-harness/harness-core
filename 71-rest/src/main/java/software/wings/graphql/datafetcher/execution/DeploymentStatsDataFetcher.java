package software.wings.graphql.datafetcher.execution;

import com.google.inject.Inject;

import com.healthmarketscience.sqlbuilder.BinaryCondition;
import com.healthmarketscience.sqlbuilder.Converter;
import com.healthmarketscience.sqlbuilder.CustomCondition;
import com.healthmarketscience.sqlbuilder.CustomExpression;
import com.healthmarketscience.sqlbuilder.FunctionCall;
import com.healthmarketscience.sqlbuilder.InCondition;
import com.healthmarketscience.sqlbuilder.OrderObject.Dir;
import com.healthmarketscience.sqlbuilder.SelectQuery;
import com.healthmarketscience.sqlbuilder.UnaryCondition;
import com.healthmarketscience.sqlbuilder.dbspec.basic.DbColumn;
import io.fabric8.utils.Lists;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.WingsException;
import io.harness.timescaledb.TimeScaleDBService;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import software.wings.graphql.datafetcher.AbstractStatsDataFetcher;
import software.wings.graphql.datafetcher.execution.DeploymentStatsQueryMetaData.DeploymentMetaDataFields;
import software.wings.graphql.datafetcher.execution.DeploymentStatsQueryMetaData.DeploymentStatsQueryMetaDataBuilder;
import software.wings.graphql.datafetcher.execution.DeploymentStatsQueryMetaData.ResultType;
import software.wings.graphql.schema.type.aggregation.QLAggregateFunction;
import software.wings.graphql.schema.type.aggregation.QLAggregatedData;
import software.wings.graphql.schema.type.aggregation.QLAggregatedData.QLAggregatedDataBuilder;
import software.wings.graphql.schema.type.aggregation.QLAggregationKind;
import software.wings.graphql.schema.type.aggregation.QLData;
import software.wings.graphql.schema.type.aggregation.QLDataPoint;
import software.wings.graphql.schema.type.aggregation.QLDataPoint.QLDataPointBuilder;
import software.wings.graphql.schema.type.aggregation.QLDataType;
import software.wings.graphql.schema.type.aggregation.QLFilterKind;
import software.wings.graphql.schema.type.aggregation.QLIntermediateStackDataPoint;
import software.wings.graphql.schema.type.aggregation.QLIntermediateStackDataPoint.QLIntermediateStackDataPointBuilder;
import software.wings.graphql.schema.type.aggregation.QLReference;
import software.wings.graphql.schema.type.aggregation.QLSinglePointData;
import software.wings.graphql.schema.type.aggregation.QLSinglePointData.QLSinglePointDataBuilder;
import software.wings.graphql.schema.type.aggregation.QLStackedData;
import software.wings.graphql.schema.type.aggregation.QLStackedData.QLStackedDataBuilder;
import software.wings.graphql.schema.type.aggregation.QLStackedDataPoint;
import software.wings.graphql.schema.type.aggregation.QLStackedDataPoint.QLStackedDataPointBuilder;
import software.wings.graphql.schema.type.aggregation.QLStackedTimeSeriesData;
import software.wings.graphql.schema.type.aggregation.QLStackedTimeSeriesData.QLStackedTimeSeriesDataBuilder;
import software.wings.graphql.schema.type.aggregation.QLStackedTimeSeriesDataPoint;
import software.wings.graphql.schema.type.aggregation.QLStackedTimeSeriesDataPoint.QLStackedTimeSeriesDataPointBuilder;
import software.wings.graphql.schema.type.aggregation.QLStringOperator;
import software.wings.graphql.schema.type.aggregation.QLTimeDataPoint;
import software.wings.graphql.schema.type.aggregation.QLTimeDataPoint.QLTimeDataPointBuilder;
import software.wings.graphql.schema.type.aggregation.QLTimeSeriesAggregation;
import software.wings.graphql.schema.type.aggregation.QLTimeSeriesData;
import software.wings.graphql.schema.type.aggregation.QLTimeSeriesData.QLTimeSeriesDataBuilder;
import software.wings.graphql.schema.type.aggregation.QLTimeSeriesDataPoint;
import software.wings.graphql.schema.type.aggregation.QLTimeSeriesDataPoint.QLTimeSeriesDataPointBuilder;
import software.wings.graphql.schema.type.aggregation.deployment.QLDeploymentAggregation;
import software.wings.graphql.schema.type.aggregation.deployment.QLDeploymentFilter;
import software.wings.graphql.schema.type.aggregation.deployment.QLDeploymentFilterType;
import software.wings.graphql.utils.nameservice.NameService;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;

@Slf4j
public class DeploymentStatsDataFetcher extends AbstractStatsDataFetcher<QLAggregateFunction, QLDeploymentFilter,
    QLDeploymentAggregation, QLTimeSeriesAggregation> {
  @Inject TimeScaleDBService timeScaleDBService;
  @Inject QLStatsHelper statsHelper;
  private DeploymentTableSchema schema = new DeploymentTableSchema();
  private static final int MAX_RETRY = 5;

  @Override
  protected QLData fetch(String accountId, QLAggregateFunction aggregateFunction, List<QLDeploymentFilter> filters,
      List<QLDeploymentAggregation> groupBy, QLTimeSeriesAggregation groupByTime) {
    try {
      if (timeScaleDBService.isValid()) {
        return getData(accountId, aggregateFunction, filters, groupBy, groupByTime);
      } else {
        return getMockData(groupBy, groupByTime);
      }
    } catch (Exception e) {
      throw new WingsException(e);
    }
  }

  @Nullable
  private QLData getMockData(List<QLDeploymentAggregation> groupBy, QLTimeSeriesAggregation groupByTime) {
    int groupBySize = groupBy != null ? groupBy.size() : 0;
    if (groupBySize == 0) {
      return StatsStubDataHelper.getSinglePointData();
    } else if (groupBySize == 1) {
      if (groupByTime == null) {
        return StatsStubDataHelper.getAggregatedData();
      } else {
        return StatsStubDataHelper.getTimeAggregatedData();
      }
    } else if (groupBySize == 2) {
      if (groupByTime == null) {
        return StatsStubDataHelper.getStackedAggregatedData();
      } else {
        return StatsStubDataHelper.getStackedTimeAggregatedData();
      }
    } else {
      return null;
    }
  }

  private QLData getData(@NotNull String accountId, QLAggregateFunction aggregateFunction,
      List<QLDeploymentFilter> filters, List<QLDeploymentAggregation> groupBy, QLTimeSeriesAggregation groupByTime) {
    DeploymentStatsQueryMetaData queryData = null;
    boolean successful = false;
    int retryCount = 0;
    while (!successful && retryCount < MAX_RETRY) {
      try (Connection connection = timeScaleDBService.getDBConnection();
           Statement statement = connection.createStatement()) {
        preValidateInput(groupBy, groupByTime);
        queryData = formQuery(accountId, aggregateFunction, filters, groupBy, groupByTime);
        logger.info("Query : [{}]", queryData.getQuery());

        long startTime = System.currentTimeMillis();
        ResultSet resultSet = statement.executeQuery(queryData.getQuery());
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
      }
    }
    return null;
  }

  private QLStackedData generateStackedBarChartData(DeploymentStatsQueryMetaData queryData, ResultSet resultSet)
      throws SQLException {
    Map<String, List<QLIntermediateStackDataPoint>> qlTimeDataPointMap = new HashMap<>();
    while (resultSet != null && resultSet.next()) {
      QLIntermediateStackDataPointBuilder dataPointBuilder = QLIntermediateStackDataPoint.builder();
      for (DeploymentMetaDataFields field : queryData.getFieldNames()) {
        switch (field.getDataType()) {
          case INTEGER:
            dataPointBuilder.value(resultSet.getInt(field.getFieldName()));
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

    return generateStackedChart(queryData.getGroupByFields().get(0), qlTimeDataPointMap);
  }

  private QLStackedData generateStackedChart(
      DeploymentMetaDataFields groupBy1, Map<String, List<QLIntermediateStackDataPoint>> intermediateMap) {
    QLStackedDataBuilder builder = QLStackedData.builder();
    List<QLStackedDataPoint> stackedDataPoints = new ArrayList<>();
    intermediateMap.keySet().forEach(key -> {
      QLStackedDataPointBuilder stackedDataPointBuilder = QLStackedDataPoint.builder();
      List<QLDataPoint> dataPoints = intermediateMap.get(key)
                                         .parallelStream()
                                         .map(QLIntermediateStackDataPoint::getDataPoint)
                                         .collect(Collectors.toList());
      stackedDataPointBuilder.values(dataPoints).key(buildQLReference(groupBy1, key));
      stackedDataPoints.add(stackedDataPointBuilder.build());
    });

    return builder.dataPoints(stackedDataPoints).build();
  }

  private QLReference buildQLReference(DeploymentMetaDataFields field, String key) {
    return QLReference.builder().type(field.getFieldName()).id(key).name(statsHelper.getEntityName(field, key)).build();
  }

  private QLData generateStackedTimeSeriesData(DeploymentStatsQueryMetaData queryData, ResultSet resultSet)
      throws SQLException {
    Map<Long, List<QLTimeDataPoint>> qlTimeDataPointMap = new HashMap<>();
    while (resultSet != null && resultSet.next()) {
      QLTimeDataPointBuilder dataPointBuilder = QLTimeDataPoint.builder();
      for (DeploymentMetaDataFields field : queryData.getFieldNames()) {
        switch (field.getDataType()) {
          case INTEGER:
          case LONG:
            dataPointBuilder.value(resultSet.getInt(field.getFieldName()));
            break;
          case STRING:
            final String entityId = resultSet.getString(field.getFieldName());
            dataPointBuilder.key(buildQLReference(field, entityId));
            break;
          case TIMESTAMP:
            long time = resultSet.getTimestamp(field.getFieldName()).getTime();
            dataPointBuilder.time(resultSet.getTimestamp(field.getFieldName()).getTime());
            break;
          default:
            throw new RuntimeException("UnsupportedType " + field.getDataType());
        }
      }

      QLTimeDataPoint dataPoint = dataPointBuilder.build();
      qlTimeDataPointMap.computeIfAbsent(dataPoint.getTime(), k -> new ArrayList<>());
      qlTimeDataPointMap.get(dataPoint.getTime()).add(dataPoint);
    }

    return prepareStackedTimeSeriesData(qlTimeDataPointMap);
  }

  private QLData prepareStackedTimeSeriesData(Map<Long, List<QLTimeDataPoint>> qlTimeDataPointMap) {
    QLStackedTimeSeriesDataBuilder timeSeriesDataBuilder = QLStackedTimeSeriesData.builder();
    List<QLStackedTimeSeriesDataPoint> timeSeriesDataPoints = new ArrayList<>();
    qlTimeDataPointMap.keySet().forEach(time -> {
      List<QLTimeDataPoint> timeDataPoints = qlTimeDataPointMap.get(time);
      QLStackedTimeSeriesDataPointBuilder builder = QLStackedTimeSeriesDataPoint.builder();
      builder.values(timeDataPoints.parallelStream().map(QLTimeDataPoint::getQLDataPoint).collect(Collectors.toList()))
          .time(time);
      timeSeriesDataPoints.add(builder.build());
    });
    return timeSeriesDataBuilder.data(timeSeriesDataPoints).build();
  }

  private QLData generateTimeSeriesData(DeploymentStatsQueryMetaData queryData, ResultSet resultSet)
      throws SQLException {
    QLTimeSeriesDataBuilder builder = QLTimeSeriesData.builder();
    List<QLTimeSeriesDataPoint> dataPoints = new ArrayList<>();
    while (resultSet != null && resultSet.next()) {
      QLTimeSeriesDataPointBuilder dataPointBuilder = QLTimeSeriesDataPoint.builder();
      for (DeploymentMetaDataFields field : queryData.getFieldNames()) {
        switch (field.getDataType()) {
          case INTEGER:
            dataPointBuilder.data(resultSet.getInt(field.getFieldName()));
            break;
          case TIMESTAMP:
            dataPointBuilder.time(resultSet.getTimestamp(field.getFieldName()).getTime());
            break;
          default:
            throw new RuntimeException("UnsupportedType " + field.getDataType());
        }
      }
      dataPoints.add(dataPointBuilder.build());
    }
    return builder.dataPoints(dataPoints).build();
  }

  private QLData generateAggregateData(DeploymentStatsQueryMetaData queryData, ResultSet resultSet)
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
    return builder.dataPoints(dataPoints).build();
  }

  private QLData generateSinglePointData(DeploymentStatsQueryMetaData queryData, ResultSet resultSet)
      throws SQLException {
    QLSinglePointDataBuilder builder = QLSinglePointData.builder();
    while (resultSet != null && resultSet.next()) {
      DeploymentMetaDataFields queryField = queryData.getFieldNames().get(0);
      Integer data;
      switch (queryField.getDataType()) {
        case INTEGER:
          data = resultSet.getInt(queryField.getFieldName());
          break;
        default:
          throw new RuntimeException("Single Data Type data type not supported " + queryField.getDataType());
      }
      builder.dataPoint(QLDataPoint.builder()
                            .value(data)
                            .key(QLReference.builder().type(getEntityType()).id(getEntityType()).build())
                            .build());
    }
    return builder.build();
  }

  private DeploymentStatsQueryMetaData formQuery(String accountId, QLAggregateFunction aggregateFunction,
      List<QLDeploymentFilter> filters, List<QLDeploymentAggregation> groupBy, QLTimeSeriesAggregation groupByTime) {
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

    if (aggregateFunction == null
        || !aggregateFunction.getAggregateValue().equals(QLDeploymentFilterType.Duration.name())) {
      selectQuery.addCustomColumns(
          Converter.toColumnSqlObject(FunctionCall.countAll(), DeploymentMetaDataFields.COUNT.getFieldName()));
      fieldNames.add(DeploymentMetaDataFields.COUNT);
    } else if (aggregateFunction.getAggregateValue().equals(QLDeploymentFilterType.Duration.name())) {
      FunctionCall functionCall = getFunctionCall(aggregateFunction);
      selectQuery.addCustomColumns(Converter.toColumnSqlObject(
          functionCall.addColumnParams(schema.getDuration()), DeploymentMetaDataFields.DURATION.getFieldName()));
      fieldNames.add(DeploymentMetaDataFields.DURATION);
    }
    selectQuery.addCustomFromTable(schema.getDeploymentTable());

    if (isValidFilters(filters)) {
      decorateQueryWithFilters(selectQuery, filters);
    }

    if (isValidGroupByTime(groupByTime)) {
      decorateQueryWithGroupByTime(fieldNames, selectQuery, groupByTime);
    }

    if (isValidGroupBy(groupBy)) {
      decorateQueryWithGroupBy(fieldNames, selectQuery, groupBy, groupByFields);
    }

    addAccountFilter(selectQuery, accountId);

    selectQuery.getWhereClause().setDisableParens(true);
    queryMetaDataBuilder.fieldNames(fieldNames);
    queryMetaDataBuilder.query(selectQuery.toString());
    queryMetaDataBuilder.groupByFields(groupByFields);
    return queryMetaDataBuilder.build();
  }

  private void preValidateInput(List<QLDeploymentAggregation> groupBy, QLTimeSeriesAggregation groupByTime) {
    if (isValidGroupBy(groupBy) && groupBy.size() > 2) {
      throw new RuntimeException("More than 2 group bys not supported + " + groupBy);
    }

    if (isValidGroupBy(groupBy) && isValidGroupByTime(groupByTime) && groupBy.size() > 1) {
      throw new RuntimeException("For a time series aggregation, only a single groupBy is allowed");
    }
  }

  private void addAccountFilter(SelectQuery selectQuery, String accountId) {
    selectQuery.addCondition(BinaryCondition.equalTo(schema.getAccountId(), accountId));
  }

  private void decorateQueryWithFilters(SelectQuery selectQuery, List<QLDeploymentFilter> filters) {
    boolean timeFilterAdded = false;
    for (QLDeploymentFilter filter : filters) {
      if (filter.getType().getFilterKind().equals(QLFilterKind.SIMPLE)) {
        decorateSimpleFilter(selectQuery, filter);
      } else if (filter.getType().getFilterKind().equals(QLFilterKind.ARRAY)) {
        decorateArrayFilter(selectQuery, filter);
      } else if (filter.getType().getFilterKind().equals(QLFilterKind.TIME)) {
        decorateTimeFilter(selectQuery, filter);
        timeFilterAdded = true;
      }
    }
  }

  private void decorateTimeFilter(SelectQuery selectQuery, QLDeploymentFilter filter) {
    DbColumn key = getFilterKey(filter.getType());
    switch (filter.getNumberFilter().getOperator()) {
      case LESS_THAN:
        selectQuery.addCondition(
            BinaryCondition.lessThan(key, new Timestamp((Long) (filter.getNumberFilter().getValues()[0]))));
        break;
      case LESS_THAN_OR_EQUALS:
        selectQuery.addCondition(
            BinaryCondition.lessThanOrEq(key, new Timestamp((Long) (filter.getNumberFilter().getValues()[0]))));
        break;
      case GREATER_THAN:
        selectQuery.addCondition(
            BinaryCondition.greaterThan(key, new Timestamp((Long) (filter.getNumberFilter().getValues()[0]))));
        break;
      case GREATER_THAN_OR_EQUALS:
        selectQuery.addCondition(
            BinaryCondition.greaterThanOrEq(key, new Timestamp((Long) (filter.getNumberFilter().getValues()[0]))));
        break;
      default:
        throw new RuntimeException("Invalid TimeFilter operator: " + filter.getNumberFilter().getOperator());
    }
  }

  private void decorateArrayFilter(SelectQuery selectQuery, QLDeploymentFilter filter) {
    /**
     * We only support String based arrays over here
     */
    addArrayStringFilter(selectQuery, filter);
  }

  private void addArrayStringFilter(SelectQuery selectQuery, QLDeploymentFilter filter) {
    DbColumn key = getFilterKey(filter.getType());
    String filterValue = "{" + String.join(",", filter.getStringFilter().getValues()) + "}";
    switch (filter.getStringFilter().getOperator()) {
      case IN:
      case EQUALS:
        selectQuery.addCondition(new CustomCondition(key + " @>"
            + "'" + filterValue + "'"));
        break;
      case NOT_EQUALS:
      case NOT_IN:
        selectQuery.addCondition(new CustomCondition("NOT " + key + " @>"
            + "'" + filterValue + "'"));
        break;
      default:
        throw new RuntimeException(
            "Unsupported operator for ArrayStringFilter" + filter.getStringFilter().getOperator());
    }
  }

  private void decorateSimpleFilter(SelectQuery selectQuery, QLDeploymentFilter filter) {
    if (filter.getType().getDataType().equals(QLDataType.STRING)) {
      addSimpleStringFilter(selectQuery, filter);
    } else if (filter.getType().getDataType().equals(QLDataType.NUMBER)) {
      addSimpleNumberFilter(selectQuery, filter);
    }
  }

  private void addSimpleNumberFilter(SelectQuery selectQuery, QLDeploymentFilter filter) {
    DbColumn key = getFilterKey(filter.getType());
    switch (filter.getNumberFilter().getOperator()) {
      case EQUALS:
        selectQuery.addCondition(BinaryCondition.equalTo(key, filter.getNumberFilter().getValues()[0]));
        break;
      case NOT_EQUALS:
        selectQuery.addCondition(BinaryCondition.notEqualTo(key, filter.getNumberFilter().getValues()[0]));
        break;
      case IN:
        selectQuery.addCondition(new InCondition(key, (Object[]) filter.getNumberFilter().getValues()));
        break;
      case NOT_IN:
        selectQuery.addCondition(new InCondition(key, (Object[]) filter.getNumberFilter().getValues()).setNegate(true));
        break;
      case LESS_THAN:
        selectQuery.addCondition(BinaryCondition.lessThan(key, filter.getNumberFilter().getValues()[0]));
        break;
      case LESS_THAN_OR_EQUALS:
        selectQuery.addCondition(BinaryCondition.lessThanOrEq(key, filter.getNumberFilter().getValues()[0]));
        break;
      case GREATER_THAN:
        selectQuery.addCondition(BinaryCondition.greaterThan(key, filter.getNumberFilter().getValues()[0]));
        break;
      case GREATER_THAN_OR_EQUALS:
        selectQuery.addCondition(BinaryCondition.greaterThanOrEq(key, filter.getNumberFilter().getValues()[0]));
        break;
      default:
        throw new RuntimeException("String simple operator not supported" + filter.getNumberFilter().getOperator());
    }
  }

  private void addSimpleStringFilter(SelectQuery selectQuery, QLDeploymentFilter filter) {
    DbColumn key = getFilterKey(filter.getType());
    QLStringOperator operator = filter.getStringFilter().getOperator();
    QLStringOperator finalOperator = operator;
    if (filter.getStringFilter().getValues().length > 0) {
      switch (operator) {
        case EQUALS:
          finalOperator = QLStringOperator.IN;
          logger.info("Changing simpleStringOperator from [{}] to [{}]", operator, finalOperator);
          break;
        case NOT_EQUALS:
          finalOperator = QLStringOperator.NOT_IN;
          logger.info("Changing simpleStringOperator from [{}] to [{}]", operator, finalOperator);
          break;
        default:
          finalOperator = operator;
      }
    }
    switch (finalOperator) {
      case EQUALS:
        selectQuery.addCondition(BinaryCondition.equalTo(key, filter.getStringFilter().getValues()[0]));
        break;
      case NOT_EQUALS:
        selectQuery.addCondition(BinaryCondition.notEqualTo(key, filter.getStringFilter().getValues()[0]));
        break;
      case IN:
        selectQuery.addCondition(new InCondition(key, (Object[]) filter.getStringFilter().getValues()));
        break;
      case NOT_IN:
        selectQuery.addCondition(new InCondition(key, (Object[]) filter.getStringFilter().getValues()).setNegate(true));
        break;
      default:
        throw new RuntimeException("String simple operator not supported" + operator);
    }
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
   * 	ARTIFACTS TEXT[]
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
      case Triggered_By:
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
      case Pipeline:
        return schema.getPipeline();
      case Duration:
        return schema.getDuration();
      default:
        throw new RuntimeException("Filter type not supported " + type);
    }
  }

  private boolean isValidFilters(List<QLDeploymentFilter> filters) {
    if (Lists.isNullOrEmpty(filters)) {
      return false;
    }
    for (QLDeploymentFilter deploymentFilter : filters) {
      if (deploymentFilter.getType().getDataType().equals(QLDataType.NUMBER)) {
        if (deploymentFilter.getNumberFilter().getOperator() == null
            || EmptyPredicate.isEmpty(deploymentFilter.getNumberFilter().getValues())) {
          logger.error("Offending number deployment filter is [{}]", deploymentFilter);
          return false;
        }
      } else {
        if (deploymentFilter.getStringFilter().getOperator() == null
            || EmptyPredicate.isEmpty(deploymentFilter.getStringFilter().getValues())) {
          logger.error("Offending string deployment filter is [{}]", deploymentFilter);
          return false;
        }
      }
    }

    return true;
  }

  private void decorateQueryWithGroupBy(List<DeploymentMetaDataFields> fieldNames, SelectQuery selectQuery,
      List<QLDeploymentAggregation> groupBy, List<DeploymentMetaDataFields> groupByFields) {
    for (QLDeploymentAggregation aggregation : groupBy) {
      if (aggregation.getAggregationKind().equals(QLAggregationKind.SIMPLE)) {
        decorateSimpleGroupBy(fieldNames, selectQuery, aggregation, groupByFields);
      } else if (aggregation.getAggregationKind().equals(QLAggregationKind.ARRAY)) {
        decorateAggregateGroupBy(fieldNames, selectQuery, aggregation, groupByFields);
      }
    }
  }

  private void decorateAggregateGroupBy(List<DeploymentMetaDataFields> fieldNames, SelectQuery selectQuery,
      QLDeploymentAggregation aggregation, List<DeploymentMetaDataFields> groupByFields) {
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
      QLDeploymentAggregation aggregation, List<DeploymentMetaDataFields> groupByFields) {
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

  private boolean isValidGroupBy(List<QLDeploymentAggregation> groupBy) {
    return groupBy != null && groupBy.size() <= 2;
  }

  private void decorateQueryWithGroupByTime(
      List<DeploymentMetaDataFields> fieldNames, SelectQuery selectQuery, QLTimeSeriesAggregation groupByTime) {
    String unit = "days";
    int value = groupByTime.getTimeAggregationValue();

    switch (groupByTime.getTimeAggregationType()) {
      case DAY:
        unit = "days";
        break;
      case HOUR:
        unit = "hours";
        break;
      case MONTH:
        value = value * 30;
        unit = "days";
        break;
      default:
        logger.info("Unsupported timeAggregationType " + groupByTime.getTimeAggregationType());
    }

    String timeBucket = "time_bucket('" + value + " " + unit + "',endtime)";

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

  private FunctionCall getFunctionCall(QLAggregateFunction aggregateFunction) {
    switch (aggregateFunction.getAggregateOperation()) {
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

  //  public static void main(String[] args) {
  //    DeploymentStatsDataFetcher fetcher = new DeploymentStatsDataFetcher();
  //    TimeScaleDBConfig config = TimeScaleDBConfig.builder()
  //                                   .timescaledbUrl("jdbc:postgresql://localhost:5432/harness")
  //                                   .timescaledbUsername("admin")
  //                                   .timescaledbPassword("password")
  //                                   .build();
  //    fetcher.timeScaleDBService = new TimeScaleDBServiceImpl(config);
  //
  //    final QLTimeSeriesAggregation timeSeriesAggregation = QLTimeSeriesAggregation.builder()
  //                                                              .timeAggregationType(QLTimeAggregationType.HOUR)
  //                                                              .timeAggregationValue(1)
  //                                                              .build();
  //    final QLAggregateFunction aggregateFunction = QLAggregateFunction.builder()
  //                                                      .aggregateOperation(QLAggregateOperation.MAX)
  //                                                      .aggregateValue(QLDeploymentFilterType.Duration.name())
  //                                                      .build();
  //
  //    List<QLDeploymentAggregation> aggregations = Lists.newArrayList(Environment,Application);
  //
  //    QLDeploymentFilter deploymentFilter1 = QLDeploymentFilter.builder()
  //                                               .type(QLDeploymentFilterType.Pipeline)
  //                                               .stringFilter(QLStringFilter.builder()
  //                                                                 .operator(QLStringOperator.EQUALS)
  //                                                                 .values(Arrays.asList("Test").toArray(new
  //                                                                 String[1])) .build())
  //                                               .build();
  //
  //    QLDeploymentFilter deploymentFilter2 =
  //        QLDeploymentFilter.builder()
  //            .type(QLDeploymentFilterType.Service)
  //            .stringFilter(QLStringFilter.builder()
  //                              .operator(QLStringOperator.IN)
  //                              .values(Arrays.asList("Test1", "Test2").toArray(new String[1]))
  //                              .build())
  //            .build();
  //
  //    QLDeploymentFilter deploymentFilter3 =
  //        QLDeploymentFilter.builder()
  //            .type(QLDeploymentFilterType.EndTime)
  //            .numberFilter(QLNumberFilter.builder()
  //                              .operator(QLNumberOperator.LESS_THAN)
  //                              .values(Arrays.asList(System.currentTimeMillis()).toArray(new Number[1]))
  //                              .build())
  //            .build();
  //    List<QLDeploymentFilter> filters = Lists.newArrayList(deploymentFilter1,deploymentFilter2,deploymentFilter3);
  //    System.out.println(fetcher.getData("kmpySmUISimoRrJL6NL73w", null, null, aggregations, null));
  //  }
}
