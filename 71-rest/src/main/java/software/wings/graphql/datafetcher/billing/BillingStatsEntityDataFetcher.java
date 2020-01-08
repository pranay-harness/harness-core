package software.wings.graphql.datafetcher.billing;

import com.google.inject.Inject;

import io.harness.exception.InvalidRequestException;
import io.harness.timescaledb.DBUtils;
import io.harness.timescaledb.TimeScaleDBService;
import lombok.extern.slf4j.Slf4j;
import software.wings.graphql.datafetcher.AbstractStatsDataFetcherWithAggregationList;
import software.wings.graphql.datafetcher.billing.BillingDataQueryMetadata.BillingDataMetaDataFields;
import software.wings.graphql.schema.type.aggregation.QLData;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingDataFilter;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingSortCriteria;
import software.wings.graphql.schema.type.aggregation.billing.QLCCMEntityGroupBy;
import software.wings.graphql.schema.type.aggregation.billing.QLCCMGroupBy;
import software.wings.graphql.schema.type.aggregation.billing.QLCCMTimeSeriesAggregation;
import software.wings.graphql.schema.type.aggregation.billing.QLEntityTableData;
import software.wings.graphql.schema.type.aggregation.billing.QLEntityTableData.QLEntityTableDataBuilder;
import software.wings.graphql.schema.type.aggregation.billing.QLEntityTableListData;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.annotations.AuthRule;

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
public class BillingStatsEntityDataFetcher extends AbstractStatsDataFetcherWithAggregationList<QLCCMAggregationFunction,
    QLBillingDataFilter, QLCCMGroupBy, QLBillingSortCriteria> {
  @Inject private TimeScaleDBService timeScaleDBService;
  @Inject BillingDataQueryBuilder billingDataQueryBuilder;
  @Inject BillingDataHelper billingDataHelper;
  @Inject QLBillingStatsHelper statsHelper;

  @Override
  @AuthRule(permissionType = PermissionType.LOGGED_IN)
  protected QLData fetch(String accountId, List<QLCCMAggregationFunction> aggregateFunction,
      List<QLBillingDataFilter> filters, List<QLCCMGroupBy> groupBy, List<QLBillingSortCriteria> sortCriteria) {
    try {
      if (timeScaleDBService.isValid()) {
        return getEntityData(accountId, filters, aggregateFunction, groupBy, sortCriteria);
      } else {
        throw new InvalidRequestException("Cannot process request in BillingStatsEntityDataFetcher");
      }
    } catch (Exception e) {
      throw new InvalidRequestException("Error while fetching billing data {}", e);
    }
  }

  protected QLEntityTableListData getEntityData(@NotNull String accountId, List<QLBillingDataFilter> filters,
      List<QLCCMAggregationFunction> aggregateFunction, List<QLCCMGroupBy> groupByList,
      List<QLBillingSortCriteria> sortCriteria) {
    BillingDataQueryMetadata queryData;
    ResultSet resultSet = null;
    List<QLCCMEntityGroupBy> groupByEntityList = billingDataQueryBuilder.getGroupByEntity(groupByList);
    QLCCMTimeSeriesAggregation groupByTime = billingDataQueryBuilder.getGroupByTime(groupByList);

    queryData = billingDataQueryBuilder.formQuery(
        accountId, filters, aggregateFunction, groupByEntityList, groupByTime, sortCriteria);

    logger.info("BillingStatsEntityDataFetcher query!! {}", queryData.getQuery());
    logger.info(queryData.getQuery());

    // Calculate Unallocated Cost for Clusters
    Map<String, Double> unallocatedCostForClusters = new HashMap<>();
    if (billingDataQueryBuilder.isUnallocatedCostAggregationPresent(aggregateFunction)) {
      unallocatedCostForClusters = getUnallocatedCostDataForClusters(
          accountId, aggregateFunction, filters, groupByEntityList, groupByTime, sortCriteria);
    }

    try (Connection connection = timeScaleDBService.getDBConnection();
         Statement statement = connection.createStatement()) {
      resultSet = statement.executeQuery(queryData.getQuery());
      return generateEntityData(queryData, resultSet, unallocatedCostForClusters);
    } catch (SQLException e) {
      logger.error("BillingStatsTimeSeriesDataFetcher Error exception {}", e);
    } finally {
      DBUtils.close(resultSet);
    }
    return null;
  }

  private QLEntityTableListData generateEntityData(BillingDataQueryMetadata queryData, ResultSet resultSet,
      Map<String, Double> unallocatedCostForCluster) throws SQLException {
    List<QLEntityTableData> entityTableListData = new ArrayList<>();
    while (resultSet != null && resultSet.next()) {
      String entityId = BillingStatsDefaultKeys.ENTITYID;
      String type = BillingStatsDefaultKeys.TYPE;
      String name = BillingStatsDefaultKeys.NAME;
      Double totalCost = BillingStatsDefaultKeys.TOTALCOST;
      Double idleCost = BillingStatsDefaultKeys.IDLECOST;
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
            break;
          case LAUNCHTYPE:
            launchType = resultSet.getString(field.getFieldName());
            break;
          case WORKLOADNAME:
            workloadName = resultSet.getString(field.getFieldName());
            break;
          case WORKLOADTYPE:
            workloadType = resultSet.getString(field.getFieldName());
            break;
          case NAMESPACE:
            namespace
            = resultSet.getString(field.getFieldName());
            break;
          case IDLECOST:
            idleCost = billingDataHelper.roundingDoubleFieldValue(field, resultSet);
            break;
          case CPUIDLECOST:
            cpuIdleCost = billingDataHelper.roundingDoubleFieldValue(field, resultSet);
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
          default:
            break;
        }
      }

      if (unallocatedCostForCluster.containsKey(clusterId)) {
        unallocatedCost = unallocatedCostForCluster.get(clusterId);
      }

      final QLEntityTableDataBuilder entityTableDataBuilder = QLEntityTableData.builder();
      entityTableDataBuilder.id(entityId)
          .name(name)
          .type(type)
          .totalCost(totalCost)
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
          .totalNamespaces(totalNamespaces)
          .totalWorkloads(totalWorkloads)
          .maxCpuUtilization(maxCpuUtilization)
          .maxMemoryUtilization(maxMemoryUtilization)
          .avgCpuUtilization(avgCpuUtilization)
          .avgMemoryUtilization(avgMemoryUtilization)
          .unallocatedCost(unallocatedCost);

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
        sortCriteria);
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

  @Override
  protected QLData postFetch(String accountId, List<QLCCMGroupBy> groupByList, QLData qlData) {
    return null;
  }

  @Override
  public String getEntityType() {
    return null;
  }
}
