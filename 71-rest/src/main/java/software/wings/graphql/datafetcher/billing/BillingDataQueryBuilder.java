package software.wings.graphql.datafetcher.billing;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.google.inject.Inject;

import com.healthmarketscience.sqlbuilder.BinaryCondition;
import com.healthmarketscience.sqlbuilder.Converter;
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
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.EntityType;
import software.wings.graphql.datafetcher.billing.BillingDataQueryMetadata.BillingDataMetaDataFields;
import software.wings.graphql.datafetcher.billing.BillingDataQueryMetadata.BillingDataQueryMetadataBuilder;
import software.wings.graphql.datafetcher.billing.BillingDataQueryMetadata.ResultType;
import software.wings.graphql.datafetcher.k8sLabel.K8sLabelHelper;
import software.wings.graphql.datafetcher.tag.TagHelper;
import software.wings.graphql.schema.type.aggregation.Filter;
import software.wings.graphql.schema.type.aggregation.QLAggregationKind;
import software.wings.graphql.schema.type.aggregation.QLFilterKind;
import software.wings.graphql.schema.type.aggregation.QLIdFilter;
import software.wings.graphql.schema.type.aggregation.QLIdOperator;
import software.wings.graphql.schema.type.aggregation.QLSortOrder;
import software.wings.graphql.schema.type.aggregation.QLTimeFilter;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingDataFilter;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingDataFilterType;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingDataLabelAggregation;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingDataLabelFilter;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingDataTagAggregation;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingDataTagFilter;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingDataTagType;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingSortCriteria;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingSortType;
import software.wings.graphql.schema.type.aggregation.billing.QLCCMEntityGroupBy;
import software.wings.graphql.schema.type.aggregation.billing.QLCCMGroupBy;
import software.wings.graphql.schema.type.aggregation.billing.QLCCMTimeSeriesAggregation;
import software.wings.graphql.schema.type.aggregation.billing.QLCEEnvironmentTypeFilter;
import software.wings.service.impl.EnvironmentServiceImpl;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

@Slf4j
public class BillingDataQueryBuilder {
  private BillingDataTableSchema schema = new BillingDataTableSchema();
  private static final String STANDARD_TIME_ZONE = "GMT";
  private static final String DEFAULT_ENVIRONMENT_TYPE = "ALL";
  @Inject TagHelper tagHelper;
  @Inject K8sLabelHelper k8sLabelHelper;
  @Inject EnvironmentServiceImpl environmentService;

  protected BillingDataQueryMetadata formQuery(String accountId, List<QLBillingDataFilter> filters,
      List<QLCCMAggregationFunction> aggregateFunction, List<QLCCMEntityGroupBy> groupBy,
      QLCCMTimeSeriesAggregation groupByTime, List<QLBillingSortCriteria> sortCriteria, boolean addInstanceTypeFilter) {
    return formQuery(
        accountId, filters, aggregateFunction, groupBy, groupByTime, sortCriteria, addInstanceTypeFilter, false);
  }

  protected BillingDataQueryMetadata formQuery(String accountId, List<QLBillingDataFilter> filters,
      List<QLCCMAggregationFunction> aggregateFunction, List<QLCCMEntityGroupBy> groupBy,
      QLCCMTimeSeriesAggregation groupByTime, List<QLBillingSortCriteria> sortCriteria, boolean addInstanceTypeFilter,
      boolean isCostTrendQuery) {
    BillingDataQueryMetadataBuilder queryMetaDataBuilder = BillingDataQueryMetadata.builder();
    SelectQuery selectQuery = new SelectQuery();

    ResultType resultType;
    resultType = ResultType.STACKED_TIME_SERIES;

    queryMetaDataBuilder.resultType(resultType);
    List<BillingDataMetaDataFields> fieldNames = new ArrayList<>();
    List<BillingDataMetaDataFields> groupByFields = new ArrayList<>();

    if (addInstanceTypeFilter
        && (isGroupByClusterPresent(groupBy) || isNoneGroupBySelectedWithoutFilterInClusterView(groupBy, filters))) {
      addInstanceTypeFilter(filters);
    }

    // To handle the cases of same workloadNames across different namespaces
    if (isGroupByEntityPresent(groupBy, QLCCMEntityGroupBy.WorkloadName)
        && !isGroupByEntityPresent(groupBy, QLCCMEntityGroupBy.Namespace)) {
      groupBy.add(0, QLCCMEntityGroupBy.Namespace);
    }

    decorateQueryWithAggregations(selectQuery, aggregateFunction, fieldNames);
    if (isCostTrendQuery) {
      decorateQueryWithMinMaxStartTime(selectQuery, fieldNames);
    }

    selectQuery.addCustomFromTable(schema.getBillingDataTable());

    if (isValidGroupByTime(groupByTime)) {
      decorateQueryWithGroupByTime(fieldNames, selectQuery, groupByTime, groupByFields);
    }

    if (isValidGroupBy(groupBy)) {
      decorateQueryWithGroupBy(fieldNames, selectQuery, groupBy, groupByFields);
      decorateQueryWithNodeOrPodGroupBy(fieldNames, selectQuery, groupBy, groupByFields, filters);
    }

    if (!Lists.isNullOrEmpty(filters)) {
      filters = processFilterForTagsAndLabels(accountId, filters);
      decorateQueryWithFilters(selectQuery, filters);
    }

    List<QLBillingSortCriteria> finalSortCriteria = validateAndAddSortCriteria(selectQuery, sortCriteria, fieldNames);
    addAccountFilter(selectQuery, accountId);
    selectQuery.getWhereClause().setDisableParens(true);
    queryMetaDataBuilder.fieldNames(fieldNames);
    queryMetaDataBuilder.query(selectQuery.toString());
    queryMetaDataBuilder.groupByFields(groupByFields);
    queryMetaDataBuilder.sortCriteria(finalSortCriteria);
    queryMetaDataBuilder.filters(filters);
    return queryMetaDataBuilder.build();
  }

  protected BillingDataQueryMetadata formTrendStatsQuery(
      String accountId, QLCCMAggregationFunction aggregateFunction, List<QLBillingDataFilter> filters) {
    List<QLCCMAggregationFunction> aggregationFunctions = new ArrayList<>();
    aggregationFunctions.add(aggregateFunction);
    return formTrendStatsQuery(accountId, aggregationFunctions, filters);
  }

  protected BillingDataQueryMetadata formTrendStatsQuery(
      String accountId, List<QLCCMAggregationFunction> aggregateFunction, List<QLBillingDataFilter> filters) {
    BillingDataQueryMetadataBuilder queryMetaDataBuilder = BillingDataQueryMetadata.builder();
    SelectQuery selectQuery = new SelectQuery();

    List<BillingDataMetaDataFields> fieldNames = new ArrayList<>();

    decorateQueryWithAggregations(selectQuery, aggregateFunction, fieldNames);

    decorateQueryWithMinMaxStartTime(selectQuery, fieldNames);

    selectQuery.addCustomFromTable(schema.getBillingDataTable());

    if (isClusterFilterPresent(filters) && !checkForAdditionalFilterInClusterDrillDown(filters)) {
      addInstanceTypeFilter(filters);
    }

    if (!Lists.isNullOrEmpty(filters)) {
      filters = processFilterForTagsAndLabels(accountId, filters);
      decorateQueryWithFilters(selectQuery, filters);
    }

    addAccountFilter(selectQuery, accountId);

    selectQuery.getWhereClause().setDisableParens(true);
    queryMetaDataBuilder.fieldNames(fieldNames);
    queryMetaDataBuilder.query(selectQuery.toString());
    queryMetaDataBuilder.filters(filters);
    return queryMetaDataBuilder.build();
  }

  protected BillingDataQueryMetadata formFilterValuesQuery(
      String accountId, List<QLBillingDataFilter> filters, List<QLCCMEntityGroupBy> groupBy) {
    BillingDataQueryMetadataBuilder queryMetaDataBuilder = BillingDataQueryMetadata.builder();
    SelectQuery selectQuery = new SelectQuery();
    ResultType resultType;
    resultType = ResultType.STACKED_TIME_SERIES;

    queryMetaDataBuilder.resultType(resultType);
    List<BillingDataMetaDataFields> fieldNames = new ArrayList<>();
    List<BillingDataMetaDataFields> groupByFields = new ArrayList<>();

    if (isGroupByClusterPresent(groupBy)) {
      if (!isGroupByClusterTypePresent(groupBy)) {
        addClusterTypeGroupBy(groupBy);
      }
      if (!isGroupByClusterNamePresent(groupBy)) {
        addClusterNameGroupBy(groupBy);
      }
    }

    selectQuery.addCustomFromTable(schema.getBillingDataTable());

    if (isValidGroupBy(groupBy)) {
      decorateQueryWithGroupBy(fieldNames, selectQuery, groupBy, groupByFields);
      decorateQueryWithNodeOrPodGroupBy(fieldNames, selectQuery, groupBy, groupByFields, filters);
    }

    if (!Lists.isNullOrEmpty(filters)) {
      filters = processFilterForTagsAndLabels(accountId, filters);
      decorateQueryWithFilters(selectQuery, filters);
    }

    addAccountFilter(selectQuery, accountId);

    selectQuery.getWhereClause().setDisableParens(true);
    queryMetaDataBuilder.fieldNames(fieldNames);
    queryMetaDataBuilder.query(selectQuery.toString());
    queryMetaDataBuilder.groupByFields(groupByFields);
    queryMetaDataBuilder.filters(filters);
    return queryMetaDataBuilder.build();
  }

  public BillingDataQueryMetadata formBudgetInsightQuery(String accountId, List<QLBillingDataFilter> filters,
      QLCCMAggregationFunction aggregateFunction, QLCCMTimeSeriesAggregation groupBy,
      List<QLBillingSortCriteria> sortCriteria) {
    BillingDataQueryMetadataBuilder queryMetaDataBuilder = BillingDataQueryMetadata.builder();
    SelectQuery selectQuery = new SelectQuery();
    ResultType resultType;
    resultType = ResultType.STACKED_TIME_SERIES;

    queryMetaDataBuilder.resultType(resultType);
    List<BillingDataMetaDataFields> fieldNames = new ArrayList<>();
    decorateQueryWithAggregation(selectQuery, aggregateFunction, fieldNames);

    selectQuery.addCustomFromTable(schema.getBillingDataTable());

    if (!Lists.isNullOrEmpty(filters)) {
      decorateQueryWithFilters(selectQuery, filters);
    }

    if (isValidGroupByTime(groupBy)) {
      List<BillingDataMetaDataFields> groupByFields = new ArrayList<>();
      decorateQueryWithGroupByTime(fieldNames, selectQuery, groupBy, groupByFields);
    }

    List<QLBillingSortCriteria> finalSortCriteria = validateAndAddSortCriteria(selectQuery, sortCriteria, fieldNames);

    addAccountFilter(selectQuery, accountId);

    selectQuery.getWhereClause().setDisableParens(true);
    queryMetaDataBuilder.fieldNames(fieldNames);
    queryMetaDataBuilder.query(selectQuery.toString());
    queryMetaDataBuilder.sortCriteria(finalSortCriteria);
    queryMetaDataBuilder.filters(filters);
    return queryMetaDataBuilder.build();
  }

  protected BillingDataQueryMetadata formNodeAndPodDetailsQuery(String accountId, List<QLBillingDataFilter> filters,
      List<QLCCMAggregationFunction> aggregateFunction, List<QLCCMEntityGroupBy> groupBy,
      QLCCMTimeSeriesAggregation groupByTime, List<QLBillingSortCriteria> sortCriteria) {
    BillingDataQueryMetadataBuilder queryMetaDataBuilder = BillingDataQueryMetadata.builder();
    SelectQuery selectQuery = new SelectQuery();

    ResultType resultType;
    resultType = ResultType.NODE_AND_POD_DETAILS;

    queryMetaDataBuilder.resultType(resultType);
    List<BillingDataMetaDataFields> fieldNames = new ArrayList<>();
    List<BillingDataMetaDataFields> groupByFields = new ArrayList<>();

    decorateQueryWithAggregations(selectQuery, aggregateFunction, fieldNames);

    selectQuery.addCustomFromTable(schema.getBillingDataTable());

    if (isValidGroupByTime(groupByTime)) {
      decorateQueryWithGroupByTime(fieldNames, selectQuery, groupByTime, groupByFields);
    }

    if (isValidGroupBy(groupBy)) {
      decorateQueryWithNodeOrPodGroupBy(fieldNames, selectQuery, groupBy, groupByFields, filters);
    }

    if (!Lists.isNullOrEmpty(filters)) {
      filters = processFilterForTagsAndLabels(accountId, filters);
      decorateQueryWithFilters(selectQuery, filters);
    }

    List<QLBillingSortCriteria> finalSortCriteria = validateAndAddSortCriteria(selectQuery, sortCriteria, fieldNames);

    addAccountFilter(selectQuery, accountId);

    selectQuery.getWhereClause().setDisableParens(true);
    queryMetaDataBuilder.fieldNames(fieldNames);
    queryMetaDataBuilder.query(selectQuery.toString());
    queryMetaDataBuilder.groupByFields(groupByFields);
    queryMetaDataBuilder.sortCriteria(finalSortCriteria);
    queryMetaDataBuilder.filters(filters);
    return queryMetaDataBuilder.build();
  }

  private void addAccountFilter(SelectQuery selectQuery, String accountId) {
    selectQuery.addCondition(BinaryCondition.equalTo(schema.getAccountId(), accountId));
  }

  private void decorateQueryWithAggregations(SelectQuery selectQuery, List<QLCCMAggregationFunction> aggregateFunctions,
      List<BillingDataMetaDataFields> fieldNames) {
    for (QLCCMAggregationFunction aggregationFunction : aggregateFunctions) {
      decorateQueryWithAggregation(selectQuery, aggregationFunction, fieldNames);
    }
  }

  private void decorateQueryWithAggregation(SelectQuery selectQuery, QLCCMAggregationFunction aggregationFunction,
      List<BillingDataMetaDataFields> fieldNames) {
    if (aggregationFunction != null && aggregationFunction.getOperationType() == QLCCMAggregateOperation.SUM) {
      if (aggregationFunction.getColumnName().equals(schema.getBillingAmount().getColumnNameSQL())) {
        selectQuery.addCustomColumns(
            Converter.toColumnSqlObject(FunctionCall.sum().addColumnParams(schema.getBillingAmount()),
                BillingDataMetaDataFields.SUM.getFieldName()));
        fieldNames.add(BillingDataMetaDataFields.SUM);
      } else if (aggregationFunction.getColumnName().equals(schema.getIdleCost().getColumnNameSQL())) {
        selectQuery.addCustomColumns(
            Converter.toColumnSqlObject(FunctionCall.sum().addColumnParams(schema.getActualIdleCost()),
                BillingDataMetaDataFields.IDLECOST.getFieldName()));
        fieldNames.add(BillingDataMetaDataFields.IDLECOST);
      } else if (aggregationFunction.getColumnName().equals(schema.getUnallocatedCost().getColumnNameSQL())) {
        selectQuery.addCustomColumns(
            Converter.toColumnSqlObject(FunctionCall.sum().addColumnParams(schema.getUnallocatedCost()),
                BillingDataMetaDataFields.UNALLOCATEDCOST.getFieldName()));
        fieldNames.add(BillingDataMetaDataFields.UNALLOCATEDCOST);
      } else if (aggregationFunction.getColumnName().equals(schema.getCpuIdleCost().getColumnNameSQL())) {
        selectQuery.addCustomColumns(
            Converter.toColumnSqlObject(FunctionCall.sum().addColumnParams(schema.getCpuActualIdleCost()),
                BillingDataMetaDataFields.CPUIDLECOST.getFieldName()));
        fieldNames.add(BillingDataMetaDataFields.CPUIDLECOST);
      } else if (aggregationFunction.getColumnName().equals(schema.getMemoryIdleCost().getColumnNameSQL())) {
        selectQuery.addCustomColumns(
            Converter.toColumnSqlObject(FunctionCall.sum().addColumnParams(schema.getMemoryActualIdleCost()),
                BillingDataMetaDataFields.MEMORYIDLECOST.getFieldName()));
        fieldNames.add(BillingDataMetaDataFields.MEMORYIDLECOST);
      } else if (aggregationFunction.getColumnName().equals(schema.getCpuBillingAmount().getColumnNameSQL())) {
        selectQuery.addCustomColumns(
            Converter.toColumnSqlObject(FunctionCall.sum().addColumnParams(schema.getCpuBillingAmount()),
                BillingDataMetaDataFields.CPUBILLINGAMOUNT.getFieldName()));
        fieldNames.add(BillingDataMetaDataFields.CPUBILLINGAMOUNT);
      } else if (aggregationFunction.getColumnName().equals(schema.getMemoryBillingAmount().getColumnNameSQL())) {
        selectQuery.addCustomColumns(
            Converter.toColumnSqlObject(FunctionCall.sum().addColumnParams(schema.getMemoryBillingAmount()),
                BillingDataMetaDataFields.MEMORYBILLINGAMOUNT.getFieldName()));
        fieldNames.add(BillingDataMetaDataFields.MEMORYBILLINGAMOUNT);
      } else if (aggregationFunction.getColumnName().equals(schema.getSystemCost().getColumnNameSQL())) {
        selectQuery.addCustomColumns(
            Converter.toColumnSqlObject(FunctionCall.sum().addColumnParams(schema.getSystemCost()),
                BillingDataMetaDataFields.SYSTEMCOST.getFieldName()));
        fieldNames.add(BillingDataMetaDataFields.SYSTEMCOST);
      }
    } else if (aggregationFunction != null && aggregationFunction.getOperationType() == QLCCMAggregateOperation.MAX) {
      if (aggregationFunction.getColumnName().equals(schema.getMaxCpuUtilization().getColumnNameSQL())) {
        selectQuery.addCustomColumns(
            Converter.toColumnSqlObject(FunctionCall.max().addColumnParams(schema.getMaxCpuUtilization()),
                BillingDataMetaDataFields.MAXCPUUTILIZATION.getFieldName()));
        fieldNames.add(BillingDataMetaDataFields.MAXCPUUTILIZATION);
      } else if (aggregationFunction.getColumnName().equals(schema.getMaxMemoryUtilization().getColumnNameSQL())) {
        selectQuery.addCustomColumns(
            Converter.toColumnSqlObject(FunctionCall.max().addColumnParams(schema.getMaxMemoryUtilization()),
                BillingDataMetaDataFields.MAXMEMORYUTILIZATION.getFieldName()));
        fieldNames.add(BillingDataMetaDataFields.MAXMEMORYUTILIZATION);
      }
    } else if (aggregationFunction != null && aggregationFunction.getOperationType() == QLCCMAggregateOperation.AVG) {
      if (aggregationFunction.getColumnName().equals(schema.getAvgCpuUtilization().getColumnNameSQL())) {
        selectQuery.addCustomColumns(
            Converter.toColumnSqlObject(FunctionCall.avg().addColumnParams(schema.getAvgCpuUtilization()),
                BillingDataMetaDataFields.AVGCPUUTILIZATION.getFieldName()));
        fieldNames.add(BillingDataMetaDataFields.AVGCPUUTILIZATION);
      } else if (aggregationFunction.getColumnName().equals(schema.getAvgMemoryUtilization().getColumnNameSQL())) {
        selectQuery.addCustomColumns(
            Converter.toColumnSqlObject(FunctionCall.avg().addColumnParams(schema.getAvgMemoryUtilization()),
                BillingDataMetaDataFields.AVGMEMORYUTILIZATION.getFieldName()));
        fieldNames.add(BillingDataMetaDataFields.AVGMEMORYUTILIZATION);
      }
    }
  }

  private void decorateQueryWithMinMaxStartTime(SelectQuery selectQuery, List<BillingDataMetaDataFields> fieldNames) {
    selectQuery.addCustomColumns(Converter.toColumnSqlObject(FunctionCall.min().addColumnParams(schema.getStartTime()),
        BillingDataMetaDataFields.MIN_STARTTIME.getFieldName()));
    fieldNames.add(BillingDataMetaDataFields.MIN_STARTTIME);
    selectQuery.addCustomColumns(Converter.toColumnSqlObject(FunctionCall.max().addColumnParams(schema.getStartTime()),
        BillingDataMetaDataFields.MAX_STARTTIME.getFieldName()));
    fieldNames.add(BillingDataMetaDataFields.MAX_STARTTIME);
  }

  private void decorateQueryWithFilters(SelectQuery selectQuery, List<QLBillingDataFilter> filters) {
    for (QLBillingDataFilter filter : filters) {
      Set<QLBillingDataFilterType> filterTypes = QLBillingDataFilter.getFilterTypes(filter);
      for (QLBillingDataFilterType type : filterTypes) {
        if (type.getMetaDataFields().getFilterKind() == QLFilterKind.SIMPLE) {
          decorateSimpleFilter(selectQuery, filter, type);
        } else {
          logger.error("Failed to apply filter :[{}]", filter);
        }
      }
    }
  }

  private void decorateSimpleFilter(SelectQuery selectQuery, QLBillingDataFilter filter, QLBillingDataFilterType type) {
    Filter f = QLBillingDataFilter.getFilter(type, filter);
    if (checkFilter(f)) {
      if (isIdFilter(f)) {
        addSimpleIdOperator(selectQuery, f, type);
      } else if (isTimeFilter(f)) {
        addSimpleTimeFilter(selectQuery, f, type);
      }
    } else {
      logger.info("Not adding filter since it is not valid " + f);
    }
  }

  private void addSimpleTimeFilter(SelectQuery selectQuery, Filter filter, QLBillingDataFilterType type) {
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
        throw new InvalidRequestException("Invalid TimeFilter operator: " + filter.getOperator());
    }
  }

  private void addSimpleIdOperator(SelectQuery selectQuery, Filter filter, QLBillingDataFilterType type) {
    DbColumn key = getFilterKey(type);
    QLIdOperator operator = (QLIdOperator) filter.getOperator();
    QLIdOperator finalOperator = operator;
    if (filter.getValues().length > 0) {
      if (operator == QLIdOperator.EQUALS) {
        finalOperator = QLIdOperator.IN;
        logger.info("Changing simpleStringOperator from [{}] to [{}]", operator, finalOperator);
      } else {
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
      case NOT_NULL:
        selectQuery.addCondition(UnaryCondition.isNotNull(key));
        break;
      case NOT_IN:
        InCondition inCondition = new InCondition(key, (Object[]) filter.getValues());
        inCondition.setNegate(true);
        selectQuery.addCondition(inCondition);
        break;
      default:
        throw new InvalidRequestException("String simple operator not supported" + operator);
    }
  }

  private boolean isIdFilter(Filter f) {
    return f instanceof QLIdFilter;
  }

  private boolean isTimeFilter(Filter f) {
    return f instanceof QLTimeFilter;
  }

  private boolean checkFilter(Filter f) {
    return f.getOperator() != null && EmptyPredicate.isNotEmpty(f.getValues());
  }

  private DbColumn getFilterKey(QLBillingDataFilterType type) {
    switch (type) {
      case EndTime:
      case StartTime:
        return schema.getStartTime();
      case Application:
        return schema.getAppId();
      case Service:
        return schema.getServiceId();
      case Environment:
        return schema.getEnvId();
      case Cluster:
        return schema.getClusterId();
      case CloudServiceName:
        return schema.getCloudServiceName();
      case LaunchType:
        return schema.getLaunchType();
      case TaskId:
        return schema.getTaskId();
      case InstanceType:
        return schema.getInstanceType();
      case WorkloadName:
        return schema.getWorkloadName();
      case Namespace:
        return schema.getNamespace();
      case CloudProvider:
        return schema.getCloudProviderId();
      case NodeInstanceId:
      case PodInstanceId:
        return schema.getInstanceId();
      case ParentInstanceId:
        return schema.getParentInstanceId();
      default:
        throw new InvalidRequestException("Filter type not supported " + type);
    }
  }

  private void decorateQueryWithGroupBy(List<BillingDataMetaDataFields> fieldNames, SelectQuery selectQuery,
      List<QLCCMEntityGroupBy> groupBy, List<BillingDataMetaDataFields> groupByFields) {
    for (QLCCMEntityGroupBy aggregation : groupBy) {
      if (aggregation.getAggregationKind() == QLAggregationKind.SIMPLE) {
        decorateSimpleGroupBy(fieldNames, selectQuery, aggregation, groupByFields);
      }
    }
  }

  private void decorateSimpleGroupBy(List<BillingDataMetaDataFields> fieldNames, SelectQuery selectQuery,
      QLCCMEntityGroupBy aggregation, List<BillingDataMetaDataFields> groupByFields) {
    DbColumn groupBy;
    switch (aggregation) {
      case Application:
        groupBy = schema.getAppId();
        break;
      case StartTime:
        groupBy = schema.getStartTime();
        break;
      case Service:
        groupBy = schema.getServiceId();
        break;
      case Region:
        groupBy = schema.getRegion();
        break;
      case Cluster:
        groupBy = schema.getClusterId();
        break;
      case ClusterName:
        groupBy = schema.getClusterName();
        break;
      case Environment:
        groupBy = schema.getEnvId();
        break;
      case InstanceType:
        groupBy = schema.getInstanceType();
        break;
      case CloudServiceName:
        groupBy = schema.getCloudServiceName();
        break;
      case TaskId:
        groupBy = schema.getTaskId();
        break;
      case LaunchType:
        groupBy = schema.getLaunchType();
        break;
      case WorkloadName:
        groupBy = schema.getWorkloadName();
        break;
      case WorkloadType:
        groupBy = schema.getWorkloadType();
        break;
      case Namespace:
        groupBy = schema.getNamespace();
        break;
      case ClusterType:
        groupBy = schema.getClusterType();
        break;
      case CloudProvider:
        groupBy = schema.getCloudProviderId();
        break;
      case Pod:
      case Node:
        return;
      default:
        throw new InvalidRequestException("Invalid groupBy clause");
    }
    selectQuery.addColumns(groupBy);
    selectQuery.addGroupings(groupBy);
    fieldNames.add(BillingDataMetaDataFields.valueOf(groupBy.getName().toUpperCase()));
    selectQuery.addCondition(UnaryCondition.isNotNull(groupBy));
    groupByFields.add(BillingDataMetaDataFields.valueOf(groupBy.getName().toUpperCase()));
  }

  private void decorateQueryWithNodeOrPodGroupBy(List<BillingDataMetaDataFields> fieldNames, SelectQuery selectQuery,
      List<QLCCMEntityGroupBy> groupBy, List<BillingDataMetaDataFields> groupByFields,
      List<QLBillingDataFilter> filters) {
    for (QLCCMEntityGroupBy aggregation : groupBy) {
      DbColumn groupByColumn;
      List<String> instanceType = new ArrayList<>();
      switch (aggregation) {
        case Node:
          groupByColumn = schema.getInstanceId();
          instanceType.add("K8S_NODE");
          break;
        case Pod:
          groupByColumn = schema.getInstanceId();
          instanceType.add("K8S_POD");
          break;
        default:
          continue;
      }
      filters.add(QLBillingDataFilter.builder()
                      .instanceType(QLIdFilter.builder()
                                        .operator(QLIdOperator.EQUALS)
                                        .values(instanceType.toArray(new String[0]))
                                        .build())
                      .build());
      selectQuery.addColumns(groupByColumn);
      selectQuery.addGroupings(groupByColumn);
      fieldNames.add(BillingDataMetaDataFields.valueOf(groupByColumn.getName().toUpperCase()));
      selectQuery.addCondition(UnaryCondition.isNotNull(groupByColumn));
      groupByFields.add(BillingDataMetaDataFields.valueOf(groupByColumn.getName().toUpperCase()));
    }
  }

  protected List<QLCCMEntityGroupBy> getGroupByEntity(List<QLCCMGroupBy> groupBy) {
    return groupBy != null ? groupBy.stream()
                                 .filter(g -> g.getEntityGroupBy() != null)
                                 .map(QLCCMGroupBy::getEntityGroupBy)
                                 .collect(Collectors.toList())
                           : Collections.emptyList();
  }

  private boolean isValidGroupBy(List<QLCCMEntityGroupBy> groupBy) {
    return EmptyPredicate.isNotEmpty(groupBy) && groupBy.size() <= 5;
  }

  private List<QLBillingSortCriteria> validateAndAddSortCriteria(
      SelectQuery selectQuery, List<QLBillingSortCriteria> sortCriteria, List<BillingDataMetaDataFields> fieldNames) {
    if (isEmpty(sortCriteria)) {
      return new ArrayList<>();
    }

    sortCriteria.removeIf(qlBillingSortCriteria
        -> qlBillingSortCriteria.getSortOrder() == null
            || !fieldNames.contains(qlBillingSortCriteria.getSortType().getBillingMetaData()));

    if (EmptyPredicate.isNotEmpty(sortCriteria)) {
      sortCriteria.forEach(s -> addOrderBy(selectQuery, s));
    }
    return sortCriteria;
  }

  private void addOrderBy(SelectQuery selectQuery, QLBillingSortCriteria sortCriteria) {
    QLBillingSortType sortType = sortCriteria.getSortType();
    OrderObject.Dir dir = sortCriteria.getSortOrder() == QLSortOrder.ASCENDING ? Dir.ASCENDING : Dir.DESCENDING;
    switch (sortType) {
      case Time:
        selectQuery.addCustomOrdering(BillingDataMetaDataFields.STARTTIME.getFieldName(), dir);
        break;
      case Amount:
        selectQuery.addCustomOrdering(BillingDataMetaDataFields.SUM.getFieldName(), dir);
        break;
      case IdleCost:
        selectQuery.addCustomOrdering(BillingDataMetaDataFields.IDLECOST.getFieldName(), dir);
        break;
      default:
        throw new InvalidRequestException("Order type not supported " + sortType);
    }
  }

  private boolean isGroupByClusterPresent(List<QLCCMEntityGroupBy> groupByList) {
    return groupByList.stream().anyMatch(groupBy -> groupBy == QLCCMEntityGroupBy.Cluster);
  }

  private boolean isGroupByClusterNamePresent(List<QLCCMEntityGroupBy> groupByList) {
    return groupByList.stream().anyMatch(groupBy -> groupBy == QLCCMEntityGroupBy.ClusterName);
  }

  private boolean isGroupByClusterTypePresent(List<QLCCMEntityGroupBy> groupByList) {
    return groupByList.stream().anyMatch(groupBy -> groupBy == QLCCMEntityGroupBy.ClusterType);
  }

  public void addClusterNameGroupBy(List<QLCCMEntityGroupBy> groupByList) {
    groupByList.add(QLCCMEntityGroupBy.ClusterName);
  }

  public void addClusterTypeGroupBy(List<QLCCMEntityGroupBy> groupByList) {
    groupByList.add(QLCCMEntityGroupBy.ClusterType);
  }

  private boolean isGroupByStartTimePresent(List<QLCCMEntityGroupBy> groupByList) {
    return groupByList.stream().anyMatch(groupBy -> groupBy == QLCCMEntityGroupBy.StartTime);
  }

  private boolean isNoneGroupBySelectedWithoutFilterInClusterView(
      List<QLCCMEntityGroupBy> groupByList, List<QLBillingDataFilter> filters) {
    return isClusterFilterPresent(filters) && !checkForAdditionalFilterInClusterDrillDown(filters)
        && isGroupByNonePresent(groupByList);
  }

  private boolean isGroupByNonePresent(List<QLCCMEntityGroupBy> groupByList) {
    return isEmpty(groupByList) || (groupByList.size() == 1 && isGroupByStartTimePresent(groupByList));
  }

  private boolean checkForAdditionalFilterInClusterDrillDown(List<QLBillingDataFilter> filters) {
    for (QLBillingDataFilter filter : filters) {
      Set<QLBillingDataFilterType> filterTypes = QLBillingDataFilter.getFilterTypes(filter);
      if (!filterTypes.isEmpty()
          && (!(filter.getCluster() != null || filter.getStartTime() != null || filter.getEndTime() != null))) {
        return true;
      }
    }
    return false;
  }

  private boolean isClusterFilterPresent(List<QLBillingDataFilter> filters) {
    return filters.stream().anyMatch(filter -> filter.getCluster() != null);
  }

  protected boolean isUnallocatedCostAggregationPresent(List<QLCCMAggregationFunction> aggregationFunctions) {
    return aggregationFunctions.stream().anyMatch(agg -> agg.getColumnName().equals("unallocatedcost"));
  }

  private void addInstanceTypeFilter(List<QLBillingDataFilter> filters) {
    if (!isInstanceTypeFilterPresent(filters)) {
      List<String> instanceTypeValues = new ArrayList<>();
      instanceTypeValues.add("ECS_TASK_FARGATE");
      instanceTypeValues.add("ECS_CONTAINER_INSTANCE");
      instanceTypeValues.add("K8S_NODE");
      addInstanceTypeFilter(filters, instanceTypeValues);
    }
  }

  private void addInstanceTypeFilter(List<QLBillingDataFilter> filters, List<String> instanceTypeValues) {
    QLBillingDataFilter instanceTypeFilter = QLBillingDataFilter.builder()
                                                 .instanceType(QLIdFilter.builder()
                                                                   .operator(QLIdOperator.EQUALS)
                                                                   .values(instanceTypeValues.toArray(new String[0]))
                                                                   .build())
                                                 .build();
    filters.add(instanceTypeFilter);
  }

  private boolean isInstanceTypeFilterPresent(List<QLBillingDataFilter> filters) {
    return filters.stream().anyMatch(filter -> filter.getInstanceType() != null);
  }

  protected QLCCMTimeSeriesAggregation getGroupByTime(List<QLCCMGroupBy> groupBy) {
    if (groupBy != null) {
      Optional<QLCCMTimeSeriesAggregation> first = groupBy.stream()
                                                       .filter(g -> g.getTimeAggregation() != null)
                                                       .map(QLCCMGroupBy::getTimeAggregation)
                                                       .findFirst();
      return first.orElse(null);
    }
    return null;
  }

  private void decorateQueryWithGroupByTime(List<BillingDataMetaDataFields> fieldNames, SelectQuery selectQuery,
      QLCCMTimeSeriesAggregation groupByTime, List<BillingDataMetaDataFields> groupByFields) {
    String timeBucket = getGroupByTimeQueryWithDateTrunc(groupByTime, "starttime");

    selectQuery.addCustomColumns(Converter.toCustomColumnSqlObject(
        new CustomExpression(timeBucket).setDisableParens(true), BillingDataMetaDataFields.TIME_SERIES.getFieldName()));
    selectQuery.addCustomGroupings(BillingDataMetaDataFields.TIME_SERIES.getFieldName());
    selectQuery.addCustomOrdering(BillingDataMetaDataFields.TIME_SERIES.getFieldName(), Dir.ASCENDING);
    fieldNames.add(BillingDataMetaDataFields.TIME_SERIES);
    groupByFields.add(BillingDataMetaDataFields.TIME_SERIES);
  }

  private boolean isValidGroupByTime(QLCCMTimeSeriesAggregation groupByTime) {
    return groupByTime != null && groupByTime.getTimeGroupType() != null;
  }

  public String getGroupByTimeQueryWithDateTrunc(QLCCMTimeSeriesAggregation groupByTime, String dbFieldName) {
    String unit;
    switch (groupByTime.getTimeGroupType()) {
      case HOUR:
        unit = "hour";
        break;
      case DAY:
        unit = "day";
        break;
      case WEEK:
        unit = "week";
        break;
      case MONTH:
        unit = "month";
        break;
      default:
        logger.warn("Unsupported timeGroupType " + groupByTime.getTimeGroupType());
        throw new InvalidRequestException("Cant apply time group by");
    }

    return new StringBuilder("date_trunc('")
        .append(unit)
        .append("',")
        .append(dbFieldName)
        .append(" at time zone '")
        .append(STANDARD_TIME_ZONE)
        .append("')")
        .toString();
  }

  protected QLTimeFilter getStartTimeFilter(List<QLBillingDataFilter> filters) {
    Optional<QLBillingDataFilter> startTimeDataFilter =
        filters.stream().filter(qlBillingDataFilter -> qlBillingDataFilter.getStartTime() != null).findFirst();
    if (startTimeDataFilter.isPresent()) {
      return startTimeDataFilter.get().getStartTime();
    } else {
      throw new InvalidRequestException("Start time cannot be null");
    }
  }

  protected QLBillingDataFilter getInstanceTypeFilter() {
    String[] instanceTypeIdFilterValues = new String[] {"CLUSTER_UNALLOCATED"};
    QLIdFilter instanceTypeFilter =
        QLIdFilter.builder().operator(QLIdOperator.EQUALS).values(instanceTypeIdFilterValues).build();
    return QLBillingDataFilter.builder().instanceType(instanceTypeFilter).build();
  }

  protected List<QLBillingDataFilter> prepareFiltersForUnallocatedCostData(List<QLBillingDataFilter> filters) {
    List<QLBillingDataFilter> modifiedFilterList =
        filters.stream()
            .filter(qlBillingDataFilter -> qlBillingDataFilter.getInstanceType() == null)
            .collect(Collectors.toList());

    modifiedFilterList.add(getInstanceTypeFilter());
    return modifiedFilterList;
  }

  private List<QLBillingDataFilter> processFilterForTagsAndLabels(String accountId, List<QLBillingDataFilter> filters) {
    List<QLBillingDataFilter> newList = new ArrayList<>();
    for (QLBillingDataFilter filter : filters) {
      Set<QLBillingDataFilterType> filterTypes = QLBillingDataFilter.getFilterTypes(filter);
      for (QLBillingDataFilterType type : filterTypes) {
        if (type == QLBillingDataFilterType.Tag) {
          QLBillingDataTagFilter tagFilter = filter.getTag();

          if (tagFilter != null) {
            Set<String> entityIds = tagHelper.getEntityIdsFromTags(
                accountId, tagFilter.getTags(), getEntityType(tagFilter.getEntityType()));
            if (isNotEmpty(entityIds)) {
              switch (tagFilter.getEntityType()) {
                case APPLICATION:
                  newList.add(QLBillingDataFilter.builder()
                                  .application(QLIdFilter.builder()
                                                   .operator(QLIdOperator.IN)
                                                   .values(entityIds.toArray(new String[0]))
                                                   .build())
                                  .build());
                  break;
                case SERVICE:
                  newList.add(QLBillingDataFilter.builder()
                                  .service(QLIdFilter.builder()
                                               .operator(QLIdOperator.IN)
                                               .values(entityIds.toArray(new String[0]))
                                               .build())
                                  .build());
                  break;
                case ENVIRONMENT:
                  newList.add(QLBillingDataFilter.builder()
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
        } else if (type == QLBillingDataFilterType.Label) {
          QLBillingDataLabelFilter labelFilter = filter.getLabel();
          if (labelFilter != null) {
            Set<String> workloadNamesWithNamespaces =
                k8sLabelHelper.getWorkloadNamesWithNamespacesFromLabels(accountId, labelFilter);
            Set<String> workloadNames = new HashSet<>();
            Set<String> namespaces = new HashSet<>();
            workloadNamesWithNamespaces.forEach(workloadNameWithNamespace -> {
              StringTokenizer tokenizer = new StringTokenizer(workloadNameWithNamespace, BillingStatsDefaultKeys.TOKEN);
              workloadNames.add(tokenizer.nextToken());
              namespaces.add(tokenizer.nextToken());
            });
            if (isNotEmpty(workloadNames)) {
              newList.add(QLBillingDataFilter.builder()
                              .workloadName(QLIdFilter.builder()
                                                .operator(QLIdOperator.IN)
                                                .values(workloadNames.toArray(new String[0]))
                                                .build())
                              .build());
            }
            if (isNotEmpty(namespaces)) {
              newList.add(QLBillingDataFilter.builder()
                              .namespace(QLIdFilter.builder()
                                             .operator(QLIdOperator.IN)
                                             .values(namespaces.toArray(new String[0]))
                                             .build())
                              .build());
            }
          }
        } else if (type == QLBillingDataFilterType.EnvironmentType) {
          newList.add(getEnvironmentIdFilter(filters, filter.getEnvType()));
        } else {
          newList.add(filter);
        }
      }
    }

    return newList;
  }

  private QLBillingDataFilter getEnvironmentIdFilter(
      List<QLBillingDataFilter> filters, QLCEEnvironmentTypeFilter environmentTypeFilter) {
    String envType = DEFAULT_ENVIRONMENT_TYPE;
    if (environmentTypeFilter.getValues() != null) {
      envType = (String) environmentTypeFilter.getValues()[0];
    }
    List<String> envIds = environmentService.getEnvIdsByAppsAndType(getAppIdsFromFilter(filters), envType);
    return QLBillingDataFilter.builder()
        .environment(QLIdFilter.builder().operator(QLIdOperator.IN).values(envIds.toArray(new String[0])).build())
        .build();
  }

  private List<String> getAppIdsFromFilter(List<QLBillingDataFilter> filters) {
    for (QLBillingDataFilter filter : filters) {
      if (filter.getApplication() != null) {
        return Arrays.asList(filter.getApplication().getValues());
      }
    }
    return new ArrayList<>();
  }

  public EntityType getEntityType(QLBillingDataTagType entityType) {
    switch (entityType) {
      case APPLICATION:
        return EntityType.APPLICATION;
      case SERVICE:
        return EntityType.SERVICE;
      case ENVIRONMENT:
        return EntityType.ENVIRONMENT;
      default:
        logger.error("Unsupported entity type {} for tag ", entityType);
        throw new InvalidRequestException("Unsupported entity type " + entityType);
    }
  }

  protected QLCCMEntityGroupBy getGroupByEntityFromTag(QLBillingDataTagAggregation groupByTag) {
    switch (groupByTag.getEntityType()) {
      case APPLICATION:
        return QLCCMEntityGroupBy.Application;
      case SERVICE:
        return QLCCMEntityGroupBy.Service;
      case ENVIRONMENT:
        return QLCCMEntityGroupBy.Environment;
      default:
        logger.warn("Unsupported tag entity type {}", groupByTag.getEntityType());
        throw new InvalidRequestException("Unsupported entity type " + groupByTag.getEntityType());
    }
  }

  protected QLCCMEntityGroupBy getGroupByEntityFromLabel(QLBillingDataLabelAggregation groupByLabel) {
    return QLCCMEntityGroupBy.WorkloadName;
  }

  protected boolean isGroupByEntityPresent(List<QLCCMEntityGroupBy> groupByList, QLCCMEntityGroupBy entityGroupBy) {
    return groupByList.stream().anyMatch(groupBy -> groupBy == entityGroupBy);
  }
}
