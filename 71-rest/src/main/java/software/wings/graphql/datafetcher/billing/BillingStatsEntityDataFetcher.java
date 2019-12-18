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
import java.util.List;
import javax.validation.constraints.NotNull;

@Slf4j
public class BillingStatsEntityDataFetcher extends AbstractStatsDataFetcherWithAggregationList<QLCCMAggregationFunction,
    QLBillingDataFilter, QLCCMGroupBy, QLBillingSortCriteria> {
  @Inject private TimeScaleDBService timeScaleDBService;
  @Inject BillingDataQueryBuilder billingDataQueryBuilder;
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

    queryData =
        billingDataQueryBuilder.formQuery(accountId, filters, aggregateFunction, groupByEntityList, sortCriteria);
    logger.info("BillingStatsEntityDataFetcher query!! {}", queryData.getQuery());
    logger.info(queryData.getQuery());

    try (Connection connection = timeScaleDBService.getDBConnection();
         Statement statement = connection.createStatement()) {
      resultSet = statement.executeQuery(queryData.getQuery());
      return generateEntityData(queryData, resultSet);
    } catch (SQLException e) {
      logger.error("BillingStatsTimeSeriesDataFetcher Error exception {}", e);
    } finally {
      DBUtils.close(resultSet);
    }
    return null;
  }

  private QLEntityTableListData generateEntityData(BillingDataQueryMetadata queryData, ResultSet resultSet)
      throws SQLException {
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
      String environment = BillingStatsDefaultKeys.ENVIRONMENT;
      String cloudProvider = BillingStatsDefaultKeys.CLOUDPROVIDER;
      Double unallocatedCost = BillingStatsDefaultKeys.UNALLOCATEDCOST;

      for (BillingDataMetaDataFields field : queryData.getFieldNames()) {
        switch (field) {
          case APPID:
          case SERVICEID:
          case CLUSTERNAME:
          case INSTANCEID:
            type = field.getFieldName();
            entityId = resultSet.getString(field.getFieldName());
            name = statsHelper.getEntityName(field, entityId);
            break;
          case REGION:
            region = resultSet.getString(field.getFieldName());
            break;
          case SUM:
            totalCost = Math.round(resultSet.getDouble(field.getFieldName()) * 100D) / 100D;
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
            idleCost = Math.round(resultSet.getDouble(field.getFieldName()) * 100D) / 100D;
            break;
          case CPUIDLECOST:
            cpuIdleCost = Math.round(resultSet.getDouble(field.getFieldName()) * 100D) / 100D;
            break;
          case MEMORYIDLECOST:
            memoryIdleCost = Math.round(resultSet.getDouble(field.getFieldName()) * 100D) / 100D;
            break;
          case CLUSTERTYPE:
            clusterType = resultSet.getString(field.getFieldName());
            break;
          case CLUSTERID:
            clusterId = resultSet.getString(field.getFieldName());
            break;
          case MAXCPUUTILIZATION:
            maxCpuUtilization = resultSet.getDouble(field.getFieldName());
            break;
          case MAXMEMORYUTILIZATION:
            maxMemoryUtilization = resultSet.getDouble(field.getFieldName());
            break;
          case AVGCPUUTILIZATION:
            avgCpuUtilization = resultSet.getDouble(field.getFieldName());
            break;
          case AVGMEMORYUTILIZATION:
            avgMemoryUtilization = resultSet.getDouble(field.getFieldName());
            break;
          case TOTALNAMESPACES:
            // Todo: query db to get total namespace count
            break;
          case TOTALWORKLOADS:
            // Todo: query db to get total workloads in a given namespace
            break;
          case CLOUDPROVIDER:
            cloudProvider = resultSet.getString(field.getFieldName());
            break;
          case ENVID:
            environment = resultSet.getString(field.getFieldName());
            break;
          case UNALLOCATEDCOST:
            // Todo: get unallocated resource cost here
            break;
          default:
            break;
        }
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
          .environment(environment)
          .cloudProvider(cloudProvider)
          .unallocatedCost(unallocatedCost);

      entityTableListData.add(entityTableDataBuilder.build());
    }

    return QLEntityTableListData.builder().data(entityTableListData).build();
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
