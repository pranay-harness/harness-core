package software.wings.graphql.datafetcher.billing;

import com.google.inject.Inject;

import io.harness.ccm.cluster.InstanceDataServiceImpl;
import io.harness.ccm.cluster.entities.InstanceData;
import io.harness.exception.InvalidRequestException;
import io.harness.timescaledb.DBUtils;
import io.harness.timescaledb.TimeScaleDBService;
import lombok.extern.slf4j.Slf4j;
import software.wings.graphql.datafetcher.AbstractStatsDataFetcherWithAggregationList;
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
public class NodeAndPodDetailsDataFetcher extends AbstractStatsDataFetcherWithAggregationList<QLCCMAggregationFunction,
    QLBillingDataFilter, QLCCMGroupBy, QLBillingSortCriteria> {
  @Inject private TimeScaleDBService timeScaleDBService;
  @Inject QLBillingStatsHelper statsHelper;
  @Inject BillingDataQueryBuilder billingDataQueryBuilder;
  @Inject BillingDataHelper billingDataHelper;
  @Inject InstanceDataServiceImpl instanceDataService;

  private static final String INSTANCE_CATEGORY = "instance_category";
  private static final String OPERATING_SYSTEM = "operating_system";
  private static final String INSTANCE_TYPE_NODE = "K8S_NODE";
  private static final String INSTANCE_TYPE_PODS = "K8S_POD";
  private static final String NAMESPACE = "namespace";
  private static final String WORKLOAD = "workload";
  private static final String PARENT_RESOURCE_ID = "parent_resource_id";

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.LOGGED_IN)
  protected QLData fetch(String accountId, List<QLCCMAggregationFunction> aggregateFunction,
      List<QLBillingDataFilter> filters, List<QLCCMGroupBy> groupBy, List<QLBillingSortCriteria> sortCriteria) {
    try {
      if (timeScaleDBService.isValid()) {
        return getData(accountId, filters, aggregateFunction, groupBy, sortCriteria);
      } else {
        throw new InvalidRequestException("Cannot process request in NodeAndPodDetailsDataFetcher");
      }
    } catch (Exception e) {
      throw new InvalidRequestException("Error while fetching billing data {}", e);
    }
  }

  protected QLNodeAndPodDetailsTableData getData(@NotNull String accountId, List<QLBillingDataFilter> filters,
      List<QLCCMAggregationFunction> aggregateFunction, List<QLCCMGroupBy> groupByList,
      List<QLBillingSortCriteria> sortCriteria) {
    BillingDataQueryMetadata queryData;
    ResultSet resultSet = null;
    QLNodeAndPodDetailsTableData costData = null;
    List<QLCCMEntityGroupBy> groupByEntityList = billingDataQueryBuilder.getGroupByEntity(groupByList);
    QLCCMTimeSeriesAggregation groupByTime = billingDataQueryBuilder.getGroupByTime(groupByList);

    queryData = billingDataQueryBuilder.formNodeAndPodDetailsQuery(
        accountId, filters, aggregateFunction, groupByEntityList, groupByTime, sortCriteria);

    logger.info("NodeAndPodDetailsDataFetcher query!! {}", queryData.getQuery());

    try (Connection connection = timeScaleDBService.getDBConnection();
         Statement statement = connection.createStatement()) {
      resultSet = statement.executeQuery(queryData.getQuery());
      costData = generateCostData(queryData, resultSet);
    } catch (SQLException e) {
      logger.error("NodeAndPodDetailsDataFetcher Error exception {}", e);
    } finally {
      DBUtils.close(resultSet);
    }

    if (costData != null && !costData.getData().isEmpty()) {
      return getFieldsFromInstanceData(accountId, costData, filters);
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
      Double unallocatedCost = BillingStatsDefaultKeys.UNALLOCATEDCOST;

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
                                  .build());
    }
    return QLNodeAndPodDetailsTableData.builder().data(entityTableListData).build();
  }

  private QLNodeAndPodDetailsTableData getFieldsFromInstanceData(
      String accountId, QLNodeAndPodDetailsTableData costData, List<QLBillingDataFilter> filters) {
    List<String> instanceIds = new ArrayList<>();
    Map<String, QLNodeAndPodDetailsTableRow> instanceIdToCostData = new HashMap<>();
    costData.getData().forEach(entry -> {
      instanceIdToCostData.put(entry.getId(), entry);
      instanceIds.add(entry.getId());
    });

    String instanceType = getInstanceType(filters);

    if (instanceType.equals(INSTANCE_TYPE_NODE)) {
      List<InstanceData> instanceData =
          instanceDataService.fetchInstanceDataForGivenInstances(accountId, getClusterId(filters), instanceIds);
      return QLNodeAndPodDetailsTableData.builder().data(getDataForNodes(instanceIdToCostData, instanceData)).build();
    } else if (instanceType.equals(INSTANCE_TYPE_PODS)) {
      List<InstanceData> instanceData =
          instanceDataService.fetchInstanceDataForGivenInstances(accountId, getClusterId(filters), instanceIds);
      return QLNodeAndPodDetailsTableData.builder().data(getDataForPods(instanceIdToCostData, instanceData)).build();
    }

    return null;
  }

  private List<QLNodeAndPodDetailsTableRow> getDataForNodes(
      Map<String, QLNodeAndPodDetailsTableRow> instanceIdToCostData, List<InstanceData> instanceData) {
    List<QLNodeAndPodDetailsTableRow> entityTableListData = new ArrayList<>();
    instanceData.forEach(entry -> {
      QLNodeAndPodDetailsTableRow costDataEntry = instanceIdToCostData.get(entry.getInstanceId());
      QLNodeAndPodDetailsTableRowBuilder builder = QLNodeAndPodDetailsTableRow.builder();
      builder.name(entry.getInstanceName())
          .id(entry.getInstanceId())
          .totalCost(costDataEntry.getTotalCost())
          .idleCost(costDataEntry.getIdleCost())
          .systemCost(costDataEntry.getSystemCost())
          .unallocatedCost(costDataEntry.getUnallocatedCost())
          .cpuAllocatable(entry.getTotalResource().getCpuUnits())
          .memoryAllocatable(entry.getTotalResource().getMemoryMb())
          .machineType(entry.getMetaData().get(OPERATING_SYSTEM))
          .instanceCategory(entry.getMetaData().get(INSTANCE_CATEGORY))
          .createTime(entry.getUsageStartTime().toEpochMilli());
      if (entry.getUsageStopTime() != null) {
        builder.deleteTime(entry.getUsageStopTime().toEpochMilli());
      }
      entityTableListData.add(builder.build());
    });
    return entityTableListData;
  }

  private List<QLNodeAndPodDetailsTableRow> getDataForPods(
      Map<String, QLNodeAndPodDetailsTableRow> instanceIdToCostData, List<InstanceData> instanceData) {
    List<QLNodeAndPodDetailsTableRow> entityTableListData = new ArrayList<>();
    instanceData.forEach(entry -> {
      QLNodeAndPodDetailsTableRow costDataEntry = instanceIdToCostData.get(entry.getInstanceId());
      QLNodeAndPodDetailsTableRowBuilder builder = QLNodeAndPodDetailsTableRow.builder();
      builder.name(entry.getInstanceName())
          .id(entry.getInstanceId())
          .namespace(entry.getMetaData().get(NAMESPACE))
          .workload(entry.getMetaData().get(WORKLOAD))
          .node(statsHelper.getInstanceName(entry.getMetaData().get(PARENT_RESOURCE_ID)))
          .totalCost(costDataEntry.getTotalCost())
          .idleCost(costDataEntry.getIdleCost())
          .systemCost(costDataEntry.getSystemCost())
          .unallocatedCost(costDataEntry.getUnallocatedCost())
          .cpuRequested(entry.getTotalResource().getCpuUnits())
          .memoryRequested(entry.getTotalResource().getMemoryMb())
          .createTime(entry.getUsageStartTime().toEpochMilli());
      if (entry.getUsageStopTime() != null) {
        builder.deleteTime(entry.getUsageStopTime().toEpochMilli());
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

  private String getClusterId(List<QLBillingDataFilter> filters) {
    for (QLBillingDataFilter filter : filters) {
      if (filter.getCluster() != null) {
        return filter.getCluster().getValues()[0];
      }
    }
    return "";
  }

  @Override
  protected QLData postFetch(String accountId, List<QLCCMGroupBy> groupByList,
      List<QLCCMAggregationFunction> aggregationFunctions, List<QLBillingSortCriteria> sortCriteria, QLData qlData) {
    return null;
  }

  @Override
  public String getEntityType() {
    return null;
  }
}
