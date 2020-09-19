package software.wings.graphql.datafetcher.billing;

import static java.lang.String.format;

import com.google.inject.Inject;

import graphql.schema.DataFetchingEnvironment;
import io.harness.exception.InvalidRequestException;
import io.harness.timescaledb.DBUtils;
import io.harness.timescaledb.TimeScaleDBService;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.EntityType;
import software.wings.graphql.datafetcher.AbstractStatsDataFetcherWithAggregationListAndTags;
import software.wings.graphql.datafetcher.billing.BillingDataQueryMetadata.BillingDataMetaDataFields;
import software.wings.graphql.schema.type.aggregation.QLData;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingDataFilter;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingDataLabelAggregation;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingDataTagAggregation;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingDataTagType;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingSortCriteria;
import software.wings.graphql.schema.type.aggregation.billing.QLCCMEntityGroupBy;
import software.wings.graphql.schema.type.aggregation.billing.QLCCMGroupBy;
import software.wings.graphql.schema.type.aggregation.billing.QLCCMTimeSeriesAggregation;
import software.wings.graphql.schema.type.aggregation.billing.QLEntityTableData;
import software.wings.graphql.schema.type.aggregation.billing.QLEntityTableData.QLEntityTableDataBuilder;
import software.wings.graphql.schema.type.aggregation.billing.QLEntityTableListData;
import software.wings.graphql.schema.type.aggregation.billing.QLStatsBreakdownInfo;
import software.wings.graphql.utils.nameservice.NameService;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.annotations.AuthRule;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;

@Slf4j
public class BillingStatsEntityDataFetcher
    extends AbstractStatsDataFetcherWithAggregationListAndTags<QLCCMAggregationFunction, QLBillingDataFilter,
        QLCCMGroupBy, QLBillingSortCriteria, QLBillingDataTagType, QLBillingDataTagAggregation,
        QLBillingDataLabelAggregation, QLCCMEntityGroupBy> {
  @Inject private TimeScaleDBService timeScaleDBService;
  @Inject BillingDataQueryBuilder billingDataQueryBuilder;
  @Inject QLBillingStatsHelper statsHelper;
  private static String OFFSET_AND_LIMIT_QUERY = " OFFSET %s LIMIT %s";

  @Override
  @AuthRule(permissionType = PermissionType.LOGGED_IN)
  protected QLData fetch(String accountId, List<QLCCMAggregationFunction> aggregateFunction,
      List<QLBillingDataFilter> filters, List<QLCCMGroupBy> groupBy, List<QLBillingSortCriteria> sortCriteria,
      Integer limit, Integer offset) {
    try {
      if (timeScaleDBService.isValid()) {
        return getEntityData(accountId, filters, aggregateFunction, groupBy, sortCriteria, limit, offset);
      } else {
        throw new InvalidRequestException("Cannot process request in BillingStatsEntityDataFetcher");
      }
    } catch (Exception e) {
      throw new InvalidRequestException("Error while fetching billing data {}", e);
    }
  }

  @Override
  protected QLData fetchSelectedFields(String accountId, List<QLCCMAggregationFunction> aggregateFunction,
      List<QLBillingDataFilter> filters, List<QLCCMGroupBy> groupBy, List<QLBillingSortCriteria> sort, Integer limit,
      Integer offset, DataFetchingEnvironment dataFetchingEnvironment) {
    return null;
  }

  protected QLEntityTableListData getEntityData(@NotNull String accountId, List<QLBillingDataFilter> filters,
      List<QLCCMAggregationFunction> aggregateFunction, List<QLCCMGroupBy> groupByList,
      List<QLBillingSortCriteria> sortCriteria, Integer limit, Integer offset) {
    BillingDataQueryMetadata queryData;
    ResultSet resultSet = null;
    boolean successful = false;
    int retryCount = 0;
    List<QLCCMEntityGroupBy> groupByEntityList = billingDataQueryBuilder.getGroupByEntity(groupByList);
    groupByEntityList = getReorderedGroupByEntityList(groupByEntityList);
    List<QLBillingDataTagAggregation> groupByTagList = getGroupByTag(groupByList);
    List<QLBillingDataLabelAggregation> groupByLabelList = getGroupByLabel(groupByList);
    QLCCMTimeSeriesAggregation groupByTime = billingDataQueryBuilder.getGroupByTime(groupByList);

    if (!groupByTagList.isEmpty()) {
      groupByEntityList = getGroupByEntityListFromTags(groupByList, groupByEntityList, groupByTagList);
    } else if (!groupByLabelList.isEmpty()) {
      groupByEntityList = getGroupByEntityListFromLabels(groupByList, groupByEntityList, groupByLabelList);
    }

    queryData = billingDataQueryBuilder.formQuery(
        accountId, filters, aggregateFunction, groupByEntityList, groupByTime, sortCriteria, true, true);
    // Not adding limit in case of group by labels/tags
    if (groupByTagList.isEmpty() && groupByLabelList.isEmpty()) {
      queryData.setQuery(queryData.getQuery() + format(OFFSET_AND_LIMIT_QUERY, offset, limit));
    }
    logger.info("BillingStatsEntityDataFetcher query!! {}", queryData.getQuery());

    Map<String, QLBillingAmountData> entityIdToPrevBillingAmountData =
        billingDataHelper.getBillingAmountDataForEntityCostTrend(
            accountId, aggregateFunction, filters, groupByEntityList, groupByTime, sortCriteria);

    while (!successful && retryCount < MAX_RETRY) {
      try (Connection connection = timeScaleDBService.getDBConnection();
           Statement statement = connection.createStatement()) {
        resultSet = statement.executeQuery(queryData.getQuery());
        successful = true;
        return generateEntityData(queryData, resultSet, entityIdToPrevBillingAmountData, filters);
      } catch (SQLException e) {
        retryCount++;
        if (retryCount >= MAX_RETRY) {
          logger.error(
              "Failed to execute query in BillingStatsEntityDataFetcher, max retry count reached, query=[{}],accountId=[{}]",
              queryData.getQuery(), accountId, e);
        } else {
          logger.warn(
              "Failed to execute query in BillingStatsEntityDataFetcher, query=[{}],accountId=[{}], retryCount=[{}]",
              queryData.getQuery(), accountId, retryCount);
        }
      } finally {
        DBUtils.close(resultSet);
      }
    }
    return null;
  }

  private QLEntityTableListData generateEntityData(BillingDataQueryMetadata queryData, ResultSet resultSet,
      Map<String, QLBillingAmountData> entityIdToPrevBillingAmountData, List<QLBillingDataFilter> filters)
      throws SQLException {
    List<QLEntityTableData> entityTableListData = new ArrayList<>();
    while (resultSet != null && resultSet.next()) {
      String entityId = BillingStatsDefaultKeys.ENTITYID;
      String type = BillingStatsDefaultKeys.TYPE;
      String name = BillingStatsDefaultKeys.NAME;
      Double totalCost = BillingStatsDefaultKeys.TOTALCOST;
      Double idleCost = BillingStatsDefaultKeys.IDLECOST;
      Double networkCost = BillingStatsDefaultKeys.NETWORKCOST;
      Double cpuIdleCost = BillingStatsDefaultKeys.CPUIDLECOST;
      Double memoryIdleCost = BillingStatsDefaultKeys.MEMORYIDLECOST;
      Double costTrend = BillingStatsDefaultKeys.COSTTREND;
      String trendType = BillingStatsDefaultKeys.TRENDTYPE;
      String region = BillingStatsDefaultKeys.REGION;
      String launchType = BillingStatsDefaultKeys.LAUNCHTYPE;
      String cloudServiceName = BillingStatsDefaultKeys.CLOUDSERVICENAME;
      String workloadName = BillingStatsDefaultKeys.WORKLOADNAME;
      String workloadType = BillingStatsDefaultKeys.WORKLOADTYPE;
      String namespace = BillingStatsDefaultKeys.NAMESPACE;
      String clusterType = BillingStatsDefaultKeys.CLUSTERTYPE;
      String clusterId = BillingStatsDefaultKeys.CLUSTERID;
      int totalWorkloads = BillingStatsDefaultKeys.TOTALWORKLOADS;
      int totalNamespaces = BillingStatsDefaultKeys.TOTALNAMESPACES;
      Double maxCpuUtilization = BillingStatsDefaultKeys.MAXCPUUTILIZATION;
      Double maxMemoryUtilization = BillingStatsDefaultKeys.MAXMEMORYUTILIZATION;
      Double avgCpuUtilization = BillingStatsDefaultKeys.AVGCPUUTILIZATION;
      Double avgMemoryUtilization = BillingStatsDefaultKeys.AVGMEMORYUTILIZATION;
      Double unallocatedCost = BillingStatsDefaultKeys.UNALLOCATEDCOST;
      // Used to recalculate cost trend in case of group by labels or tags
      Double prevBillingAmount = BillingStatsDefaultKeys.TOTALCOST;
      int efficiencyScore = BillingStatsDefaultKeys.EFFICIENCY_SCORE;
      int efficiencyScoreTrendPercentage = BillingStatsDefaultKeys.EFFICIENCY_SCORE_TREND;

      for (BillingDataMetaDataFields field : queryData.getFieldNames()) {
        switch (field) {
          case APPID:
          case SERVICEID:
          case CLUSTERNAME:
          case TASKID:
          case CLOUDPROVIDERID:
          case ENVID:
            type = field.getFieldName();
            entityId = resultSet.getString(field.getFieldName());
            name = statsHelper.getEntityName(field, entityId);
            break;
          case REGION:
            region = resultSet.getString(field.getFieldName());
            break;
          case SUM:
            totalCost = billingDataHelper.roundingDoubleFieldValue(field, resultSet);
            break;
          case CLOUDSERVICENAME:
            cloudServiceName = resultSet.getString(field.getFieldName());
            entityId = cloudServiceName;
            break;
          case LAUNCHTYPE:
            launchType = resultSet.getString(field.getFieldName());
            entityId = launchType;
            break;
          case WORKLOADNAME:
            workloadName = resultSet.getString(field.getFieldName());
            // Whenever group by workloadName is present, group bu namespace is also added in form query
            // To make sure that workloads with identical names across different namespaces are not grouped together
            entityId = resultSet.getString(BillingDataMetaDataFields.NAMESPACE.getFieldName())
                + BillingStatsDefaultKeys.TOKEN + resultSet.getString(field.getFieldName());
            break;
          case WORKLOADTYPE:
            workloadType = resultSet.getString(field.getFieldName());
            break;
          case NAMESPACE:
            namespace
            = resultSet.getString(field.getFieldName());
            entityId = resultSet.getString(field.getFieldName());
            break;
          case IDLECOST:
            idleCost = billingDataHelper.roundingDoubleFieldValue(field, resultSet);
            break;
          case CPUIDLECOST:
            cpuIdleCost = billingDataHelper.roundingDoubleFieldValue(field, resultSet);
            break;
          case NETWORKCOST:
            networkCost = billingDataHelper.roundingDoubleFieldValue(field, resultSet);
            break;
          case MEMORYIDLECOST:
            memoryIdleCost = billingDataHelper.roundingDoubleFieldValue(field, resultSet);
            break;
          case CLUSTERTYPE:
            clusterType = resultSet.getString(field.getFieldName());
            break;
          case CLUSTERID:
            type = field.getFieldName();
            entityId = resultSet.getString(field.getFieldName());
            name = statsHelper.getEntityName(field, entityId);
            clusterId = entityId;
            break;
          case MAXCPUUTILIZATION:
            maxCpuUtilization = billingDataHelper.roundingDoubleFieldPercentageValue(field, resultSet);
            break;
          case MAXMEMORYUTILIZATION:
            maxMemoryUtilization = billingDataHelper.roundingDoubleFieldPercentageValue(field, resultSet);
            break;
          case AVGCPUUTILIZATION:
            avgCpuUtilization = billingDataHelper.roundingDoubleFieldPercentageValue(field, resultSet);
            break;
          case AVGMEMORYUTILIZATION:
            avgMemoryUtilization = billingDataHelper.roundingDoubleFieldPercentageValue(field, resultSet);
            break;
          case TOTALNAMESPACES:
            // Todo: query db to get total namespace count
            break;
          case TOTALWORKLOADS:
            // Todo: query db to get total workloads in a given namespace
            break;
          case UNALLOCATEDCOST:
            unallocatedCost = billingDataHelper.roundingDoubleFieldValue(field, resultSet);
            break;
          default:
            break;
        }
      }

      if (totalCost > 0.0) {
        efficiencyScore =
            billingDataHelper.calculateEfficiencyScore(QLStatsBreakdownInfo.builder()
                                                           .total(totalCost)
                                                           .idle(idleCost)
                                                           .unallocated(unallocatedCost)
                                                           .utilized(totalCost - idleCost - unallocatedCost)
                                                           .build());
      }

      if (entityIdToPrevBillingAmountData != null && entityIdToPrevBillingAmountData.containsKey(entityId)) {
        QLBillingAmountData prevBillingAmountData = entityIdToPrevBillingAmountData.get(entityId);
        costTrend = billingDataHelper.getCostTrendForEntity(resultSet, prevBillingAmountData, filters);
        if (prevBillingAmountData != null && prevBillingAmountData.getCost() != null
            && prevBillingAmountData.getCost().compareTo(BigDecimal.ZERO) > 0
            && prevBillingAmountData.getIdleCost() != null && prevBillingAmountData.getUnallocatedCost() != null) {
          QLStatsBreakdownInfo prevCostStats = QLStatsBreakdownInfo.builder()
                                                   .total(prevBillingAmountData.getCost())
                                                   .idle(prevBillingAmountData.getIdleCost())
                                                   .unallocated(prevBillingAmountData.getUnallocatedCost())
                                                   .build();
          double prevUtilizedCost = billingDataHelper.getRoundedDoubleValue(prevCostStats.getTotal().doubleValue()
              - prevCostStats.getIdle().doubleValue() - prevCostStats.getUnallocated().doubleValue());
          prevCostStats.setUtilized(prevUtilizedCost);
          int oldEfficiencyScore = billingDataHelper.calculateEfficiencyScore(prevCostStats);
          efficiencyScoreTrendPercentage =
              billingDataHelper
                  .calculateTrendPercentage(BigDecimal.valueOf(efficiencyScore), BigDecimal.valueOf(oldEfficiencyScore))
                  .intValue();
          prevBillingAmount = prevBillingAmountData.getCost().doubleValue();
        }
      }

      final QLEntityTableDataBuilder entityTableDataBuilder = QLEntityTableData.builder();
      entityTableDataBuilder.id(entityId)
          .name(name)
          .type(type)
          .totalCost(totalCost)
          .networkCost(networkCost)
          .idleCost(idleCost)
          .cpuIdleCost(cpuIdleCost)
          .memoryIdleCost(memoryIdleCost)
          .costTrend(costTrend)
          .trendType(trendType)
          .region(region)
          .launchType(launchType)
          .cloudServiceName(cloudServiceName)
          .workloadName(workloadName)
          .workloadType(workloadType)
          .namespace(namespace)
          .clusterType(clusterType)
          .clusterId(clusterId)
          .label(BillingStatsDefaultKeys.LABEL)
          .totalNamespaces(totalNamespaces)
          .totalWorkloads(totalWorkloads)
          .maxCpuUtilization(maxCpuUtilization)
          .maxMemoryUtilization(maxMemoryUtilization)
          .avgCpuUtilization(avgCpuUtilization)
          .avgMemoryUtilization(avgMemoryUtilization)
          .unallocatedCost(unallocatedCost)
          .efficiencyScore(efficiencyScore)
          .efficiencyScoreTrendPercentage(efficiencyScoreTrendPercentage)
          .prevBillingAmount(prevBillingAmount);

      entityTableListData.add(entityTableDataBuilder.build());
    }

    return QLEntityTableListData.builder().data(entityTableListData).build();
  }

  protected Map<String, Double> getUnallocatedCostDataForClusters(String accountId,
      List<QLCCMAggregationFunction> aggregateFunction, List<QLBillingDataFilter> filters,
      List<QLCCMEntityGroupBy> groupBy, QLCCMTimeSeriesAggregation groupByTime,
      List<QLBillingSortCriteria> sortCriteria) {
    BillingDataQueryMetadata queryData = billingDataQueryBuilder.formQuery(accountId,
        billingDataQueryBuilder.prepareFiltersForUnallocatedCostData(filters), aggregateFunction, groupBy, groupByTime,
        sortCriteria, true);
    String query = queryData.getQuery();
    logger.info("Unallocated cost data query {}", query);
    ResultSet resultSet = null;
    try (Connection connection = timeScaleDBService.getDBConnection();
         Statement statement = connection.createStatement()) {
      resultSet = statement.executeQuery(query);
      return fetchUnallocatedCostForClusters(queryData, resultSet);
    } catch (SQLException e) {
      throw new InvalidRequestException("UnallocatedCost - IdleCostDataFetcher Exception ", e);
    } finally {
      DBUtils.close(resultSet);
    }
  }

  private Map<String, Double> fetchUnallocatedCostForClusters(BillingDataQueryMetadata queryData, ResultSet resultSet)
      throws SQLException {
    Map<String, Double> unallocatedCostForClusters = new HashMap<>();
    Double unallocatedCost = BillingStatsDefaultKeys.UNALLOCATEDCOST;
    String clusterId = BillingStatsDefaultKeys.CLUSTERID;
    while (null != resultSet && resultSet.next()) {
      for (BillingDataMetaDataFields field : queryData.getFieldNames()) {
        switch (field) {
          case SUM:
            unallocatedCost = Math.round(resultSet.getDouble(field.getFieldName()) * 100D) / 100D;
            break;
          case CLUSTERID:
            clusterId = resultSet.getString(field.getFieldName());
            break;
          default:
            break;
        }
      }
      unallocatedCostForClusters.put(clusterId, unallocatedCost);
    }
    return unallocatedCostForClusters;
  }

  // This is to reorder group by entity list if group by cloudServiceName/launchType/task is present
  // to obtain relevant ID in response
  private List<QLCCMEntityGroupBy> getReorderedGroupByEntityList(List<QLCCMEntityGroupBy> groupByEntityList) {
    List<QLCCMEntityGroupBy> reorderedGroupByList = new ArrayList<>();
    boolean isCloudServiceNamePresent = false;
    boolean isLaunchTypePresent = false;
    boolean isTaskIdPresent = false;

    for (QLCCMEntityGroupBy entityGroupBy : groupByEntityList) {
      switch (entityGroupBy) {
        case LaunchType:
          isLaunchTypePresent = true;
          break;
        case CloudServiceName:
          isCloudServiceNamePresent = true;
          break;
        case TaskId:
          isTaskIdPresent = true;
          break;
        default:
          reorderedGroupByList.add(entityGroupBy);
          break;
      }
    }

    if (isLaunchTypePresent) {
      reorderedGroupByList.add(QLCCMEntityGroupBy.LaunchType);
    }
    if (isCloudServiceNamePresent) {
      reorderedGroupByList.add(QLCCMEntityGroupBy.CloudServiceName);
    }
    if (isTaskIdPresent) {
      reorderedGroupByList.add(QLCCMEntityGroupBy.TaskId);
    }

    return reorderedGroupByList;
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
  protected EntityType getEntityType(QLBillingDataTagType entityType) {
    return billingDataQueryBuilder.getEntityType(entityType);
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
  protected QLCCMEntityGroupBy getEntityAggregation(QLCCMGroupBy groupBy) {
    return groupBy.getEntityGroupBy();
  }
}
