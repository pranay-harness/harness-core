package software.wings.graphql.datafetcher.billing;

import static software.wings.graphql.datafetcher.billing.BillingDataQueryBuilder.INVALID_FILTER_MSG;

import io.harness.ccm.cluster.InstanceDataServiceImpl;
import io.harness.ccm.commons.entities.InstanceData;
import io.harness.exception.InvalidRequestException;
import io.harness.timescaledb.DBUtils;
import io.harness.timescaledb.TimeScaleDBService;

import software.wings.graphql.datafetcher.AbstractStatsDataFetcherWithAggregationListAndLimit;
import software.wings.graphql.schema.type.aggregation.QLData;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingDataFilter;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingSortCriteria;
import software.wings.graphql.schema.type.aggregation.billing.QLCCMEntityGroupBy;
import software.wings.graphql.schema.type.aggregation.billing.QLCCMGroupBy;
import software.wings.graphql.schema.type.aggregation.billing.QLCCMTimeSeriesAggregation;
import software.wings.graphql.schema.type.aggregation.billing.QLNodeAndPodDetailsTableData;
import software.wings.graphql.schema.type.aggregation.billing.QLNodeAndPodDetailsTableRow;
import software.wings.graphql.schema.type.aggregation.billing.QLNodeAndPodDetailsTableRow.QLNodeAndPodDetailsTableRowBuilder;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.ce.CeAccountExpirationChecker;

import com.google.inject.Inject;
import graphql.schema.DataFetchingEnvironment;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NodeAndPodDetailsDataFetcher
    extends AbstractStatsDataFetcherWithAggregationListAndLimit<QLCCMAggregationFunction, QLBillingDataFilter,
        QLCCMGroupBy, QLBillingSortCriteria> {
  @Inject private TimeScaleDBService timeScaleDBService;
  @Inject BillingDataQueryBuilder billingDataQueryBuilder;
  @Inject BillingDataHelper billingDataHelper;
  @Inject InstanceDataServiceImpl instanceDataService;
  @Inject CeAccountExpirationChecker accountChecker;

  private static final String INSTANCE_CATEGORY = "instance_category";
  private static final String OPERATING_SYSTEM = "operating_system";
  private static final String INSTANCE_TYPE_NODE = "K8S_NODE";
  private static final String INSTANCE_TYPE_PODS = "K8S_POD";
  private static final String NAMESPACE = "namespace";
  private static final String WORKLOAD = "workload_name";
  private static final String PARENT_RESOURCE_ID = "parent_resource_id";
  private static final String NODE_POOL_NAME = "node_pool_name";
  private static final String K8S_POD_CAPACITY = "pod_capacity";
  private static final String DEFAULT_STRING_VALUE = "-";
  private static final InstanceData DEFAULT_INSTANCE_DATA = InstanceData.builder().metaData(new HashMap<>()).build();

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.LOGGED_IN)
  protected QLData fetch(String accountId, List<QLCCMAggregationFunction> aggregateFunction,
      List<QLBillingDataFilter> filters, List<QLCCMGroupBy> groupBy, List<QLBillingSortCriteria> sortCriteria,
      Integer limit, Integer offset) {
    accountChecker.checkIsCeEnabled(accountId);
    try {
      if (timeScaleDBService.isValid()) {
        return getData(accountId, filters, aggregateFunction, groupBy, sortCriteria, limit, offset);
      } else {
        throw new InvalidRequestException("Cannot process request in NodeAndPodDetailsDataFetcher");
      }
    } catch (Exception e) {
      throw new InvalidRequestException("Error while fetching billing data {}", e);
    }
  }

  protected QLNodeAndPodDetailsTableData getData(@NotNull String accountId, List<QLBillingDataFilter> filters,
      List<QLCCMAggregationFunction> aggregateFunction, List<QLCCMGroupBy> groupByList,
      List<QLBillingSortCriteria> sortCriteria, Integer limit, Integer offset) {
    BillingDataQueryMetadata queryData;
    ResultSet resultSet = null;
    QLNodeAndPodDetailsTableData costData = null;
    List<QLCCMEntityGroupBy> groupByEntityList = billingDataQueryBuilder.getGroupByEntity(groupByList);
    QLCCMTimeSeriesAggregation groupByTime = billingDataQueryBuilder.getGroupByTime(groupByList);

    if (!billingDataQueryBuilder.isFilterCombinationValid(filters, groupByEntityList)) {
      return QLNodeAndPodDetailsTableData.builder().data(null).info(INVALID_FILTER_MSG).build();
    }
    queryData = billingDataQueryBuilder.formNodeAndPodDetailsQuery(
        accountId, filters, aggregateFunction, groupByEntityList, groupByTime, sortCriteria, limit, offset);

    log.info("NodeAndPodDetailsDataFetcher query!! {}", queryData.getQuery());
    boolean successful = false;
    int retryCount = 0;
    while (!successful && retryCount < MAX_RETRY) {
      try (Connection connection = timeScaleDBService.getDBConnection();
           Statement statement = connection.createStatement()) {
        resultSet = statement.executeQuery(queryData.getQuery());
        successful = true;
        costData = generateCostData(queryData, resultSet);
      } catch (SQLException e) {
        retryCount++;
        if (retryCount >= MAX_RETRY) {
          log.error(
              "Failed to execute query in NodeAndPodDetailsDataFetcher, max retry count reached, query=[{}],accountId=[{}]",
              queryData.getQuery(), accountId, e);
        } else {
          log.warn(
              "Failed to execute query in NodeAndPodDetailsDataFetcher, query=[{}],accountId=[{}], retryCount=[{}]",
              queryData.getQuery(), accountId, retryCount);
        }
      } finally {
        DBUtils.close(resultSet);
      }
    }

    if (costData != null && !costData.getData().isEmpty()) {
      return getFieldsFromInstanceData(costData, filters);
    }

    return null;
  }

  private QLNodeAndPodDetailsTableData generateCostData(BillingDataQueryMetadata queryData, ResultSet resultSet)
      throws SQLException {
    List<QLNodeAndPodDetailsTableRow> entityTableListData = new ArrayList<>();
    while (resultSet != null && resultSet.next()) {
      String entityId = BillingStatsDefaultKeys.ENTITYID;
      String name = BillingStatsDefaultKeys.NAME;
      Double totalCost = BillingStatsDefaultKeys.TOTALCOST;
      Double idleCost = BillingStatsDefaultKeys.IDLECOST;
      Double systemCost = BillingStatsDefaultKeys.SYSTEMCOST;
      Double networkCost = BillingStatsDefaultKeys.NETWORKCOST;
      Double unallocatedCost = BillingStatsDefaultKeys.UNALLOCATEDCOST;
      String clusterName = BillingStatsDefaultKeys.CLUSTERNAME;
      String clusterId = BillingStatsDefaultKeys.CLUSTERID;

      for (BillingDataQueryMetadata.BillingDataMetaDataFields field : queryData.getFieldNames()) {
        switch (field) {
          case INSTANCEID:
            entityId = resultSet.getString(field.getFieldName());
            break;
          case SUM:
            totalCost = billingDataHelper.roundingDoubleFieldValue(field, resultSet);
            break;
          case IDLECOST:
            idleCost = billingDataHelper.roundingDoubleFieldValue(field, resultSet);
            break;
          case UNALLOCATEDCOST:
            unallocatedCost = billingDataHelper.roundingDoubleFieldValue(field, resultSet);
            break;
          case SYSTEMCOST:
            systemCost = billingDataHelper.roundingDoubleFieldValue(field, resultSet);
            break;
          case NETWORKCOST:
            networkCost = billingDataHelper.roundingDoubleFieldValue(field, resultSet);
            break;
          case CLUSTERNAME:
            clusterName = resultSet.getString(field.getFieldName());
            break;
          case CLUSTERID:
            clusterId = resultSet.getString(field.getFieldName());
            break;
          case INSTANCENAME:
            name = resultSet.getString(field.getFieldName());
            break;
          default:
            break;
        }
      }

      entityTableListData.add(QLNodeAndPodDetailsTableRow.builder()
                                  .name(name)
                                  .id(entityId)
                                  .totalCost(totalCost)
                                  .idleCost(idleCost)
                                  .systemCost(systemCost)
                                  .unallocatedCost(unallocatedCost)
                                  .networkCost(networkCost)
                                  .clusterName(clusterName)
                                  .clusterId(clusterId)
                                  .build());
    }
    return QLNodeAndPodDetailsTableData.builder().data(entityTableListData).build();
  }

  private QLNodeAndPodDetailsTableData getFieldsFromInstanceData(
      QLNodeAndPodDetailsTableData costData, List<QLBillingDataFilter> filters) {
    Set<String> instanceIds = new HashSet<>();
    List<String> instanceIdsWithCluster = new ArrayList<>();
    Map<String, QLNodeAndPodDetailsTableRow> instanceIdToCostData = new HashMap<>();
    String instanceType = getInstanceType(filters);

    if (instanceType.equals(INSTANCE_TYPE_NODE)) {
      costData.getData().forEach(entry -> {
        instanceIdToCostData.put(entry.getClusterId() + BillingStatsDefaultKeys.TOKEN + entry.getId(), entry);
        instanceIds.add(entry.getId());
        instanceIdsWithCluster.add(entry.getClusterId() + BillingStatsDefaultKeys.TOKEN + entry.getId());
      });
    } else if (instanceType.equals(INSTANCE_TYPE_PODS)) {
      costData.getData().forEach(entry -> {
        instanceIdToCostData.put(entry.getId(), entry);
        instanceIds.add(entry.getId());
        instanceIdsWithCluster.add(entry.getId());
      });
    }

    List<InstanceData> instanceData =
        instanceDataService.fetchInstanceDataForGivenInstances(new ArrayList<>(instanceIds));
    Map<String, InstanceData> instanceIdToInstanceData = new HashMap<>();

    if (instanceType.equals(INSTANCE_TYPE_NODE)) {
      instanceData.forEach(entry
          -> instanceIdToInstanceData.put(
              entry.getClusterId() + BillingStatsDefaultKeys.TOKEN + entry.getInstanceId(), entry));
      return QLNodeAndPodDetailsTableData.builder()
          .data(getDataForNodes(instanceIdToCostData, instanceIdToInstanceData, instanceIdsWithCluster))
          .build();
    } else if (instanceType.equals(INSTANCE_TYPE_PODS)) {
      instanceData.forEach(entry -> instanceIdToInstanceData.put(entry.getInstanceId(), entry));
      return QLNodeAndPodDetailsTableData.builder()
          .data(getDataForPods(instanceIdToCostData, instanceIdToInstanceData, instanceIdsWithCluster))
          .build();
    }

    return null;
  }

  private List<QLNodeAndPodDetailsTableRow> getDataForNodes(
      Map<String, QLNodeAndPodDetailsTableRow> instanceIdToCostData, Map<String, InstanceData> instanceIdToInstanceData,
      List<String> instanceIdsWithCluster) {
    List<QLNodeAndPodDetailsTableRow> entityTableListData = new ArrayList<>();
    instanceIdsWithCluster.forEach(instanceIdWithCluster -> {
      InstanceData entry = instanceIdToInstanceData.getOrDefault(instanceIdWithCluster, DEFAULT_INSTANCE_DATA);
      QLNodeAndPodDetailsTableRow costDataEntry = instanceIdToCostData.get(instanceIdWithCluster);
      QLNodeAndPodDetailsTableRowBuilder builder = QLNodeAndPodDetailsTableRow.builder();
      builder.name(costDataEntry.getName())
          .id(costDataEntry.getClusterId() + BillingStatsDefaultKeys.TOKEN + costDataEntry.getId())
          .nodeId(costDataEntry.getId())
          .clusterName(costDataEntry.getClusterName())
          .clusterId(costDataEntry.getClusterId())
          .nodePoolName(entry.getMetaData().getOrDefault(NODE_POOL_NAME, DEFAULT_STRING_VALUE))
          .podCapacity(entry.getMetaData().getOrDefault(K8S_POD_CAPACITY, DEFAULT_STRING_VALUE))
          .totalCost(costDataEntry.getTotalCost())
          .idleCost(costDataEntry.getIdleCost())
          .systemCost(costDataEntry.getSystemCost())
          .unallocatedCost(costDataEntry.getUnallocatedCost())
          .networkCost(costDataEntry.getNetworkCost())
          .cpuAllocatable(-1D)
          .memoryAllocatable(-1D)
          .machineType(entry.getMetaData().getOrDefault(OPERATING_SYSTEM, DEFAULT_STRING_VALUE))
          .instanceCategory(entry.getMetaData().getOrDefault(INSTANCE_CATEGORY, DEFAULT_STRING_VALUE));
      if (entry.getTotalResource() != null) {
        builder.cpuAllocatable(billingDataHelper.getRoundedDoubleValue(entry.getTotalResource().getCpuUnits() / 1024))
            .memoryAllocatable(billingDataHelper.getRoundedDoubleValue(entry.getTotalResource().getMemoryMb() / 1024));
      }
      if (entry.getUsageStopTime() != null) {
        builder.deleteTime(entry.getUsageStopTime().toEpochMilli());
      }
      if (entry.getUsageStartTime() != null) {
        builder.createTime(entry.getUsageStartTime().toEpochMilli());
      }
      entityTableListData.add(builder.build());
    });
    return entityTableListData;
  }

  private List<QLNodeAndPodDetailsTableRow> getDataForPods(
      Map<String, QLNodeAndPodDetailsTableRow> instanceIdToCostData, Map<String, InstanceData> instanceIdToInstanceData,
      List<String> instanceIds) {
    List<QLNodeAndPodDetailsTableRow> entityTableListData = new ArrayList<>();
    instanceIds.forEach(instanceId -> {
      InstanceData entry = instanceIdToInstanceData.getOrDefault(instanceId, DEFAULT_INSTANCE_DATA);
      QLNodeAndPodDetailsTableRow costDataEntry = instanceIdToCostData.get(instanceId);
      QLNodeAndPodDetailsTableRowBuilder builder = QLNodeAndPodDetailsTableRow.builder();
      builder.name(costDataEntry.getName())
          .id(instanceId)
          .namespace(entry.getMetaData().getOrDefault(NAMESPACE, DEFAULT_STRING_VALUE))
          .workload(entry.getMetaData().getOrDefault(WORKLOAD, DEFAULT_STRING_VALUE))
          .clusterName(costDataEntry.getClusterName())
          .clusterId(costDataEntry.getClusterId())
          .node(entry.getMetaData().getOrDefault(PARENT_RESOURCE_ID, DEFAULT_STRING_VALUE))
          .nodePoolName(entry.getMetaData().getOrDefault(NODE_POOL_NAME, DEFAULT_STRING_VALUE))
          .totalCost(costDataEntry.getTotalCost())
          .idleCost(costDataEntry.getIdleCost())
          .systemCost(costDataEntry.getSystemCost())
          .unallocatedCost(costDataEntry.getUnallocatedCost())
          .networkCost(costDataEntry.getNetworkCost())
          .cpuRequested(-1D)
          .memoryRequested(-1D);
      if (entry.getUsageStopTime() != null) {
        builder.deleteTime(entry.getUsageStopTime().toEpochMilli());
      }

      if (entry.getTotalResource() != null) {
        builder.cpuRequested(billingDataHelper.getRoundedDoubleValue(entry.getTotalResource().getCpuUnits() / 1024))
            .memoryRequested(billingDataHelper.getRoundedDoubleValue(entry.getTotalResource().getMemoryMb() / 1024));
      }

      if (entry.getUsageStartTime() != null) {
        builder.createTime(entry.getUsageStartTime().toEpochMilli());
      }
      entityTableListData.add(builder.build());
    });
    return entityTableListData;
  }

  private String getInstanceType(List<QLBillingDataFilter> filters) {
    for (QLBillingDataFilter filter : filters) {
      if (filter.getInstanceType() != null) {
        return filter.getInstanceType().getValues()[0];
      }
    }
    return "";
  }

  @Override
  protected QLData postFetch(String accountId, List<QLCCMGroupBy> groupByList,
      List<QLCCMAggregationFunction> aggregations, List<QLBillingSortCriteria> sort, QLData qlData, Integer limit,
      boolean includeOthers) {
    return null;
  }

  @Override
  public String getEntityType() {
    return null;
  }

  @Override
  protected QLData fetchSelectedFields(String accountId, List<QLCCMAggregationFunction> aggregateFunction,
      List<QLBillingDataFilter> filters, List<QLCCMGroupBy> groupBy, List<QLBillingSortCriteria> sort, Integer limit,
      Integer offset, DataFetchingEnvironment dataFetchingEnvironment) {
    return null;
  }

  @Override
  public boolean isCESampleAccountIdAllowed() {
    return true;
  }
}
