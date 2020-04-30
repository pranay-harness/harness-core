package software.wings.graphql.datafetcher.billing;

import com.google.inject.Inject;

import io.harness.ccm.cluster.BatchJobIntervalServiceImpl;
import io.harness.ccm.cluster.InstanceDataServiceImpl;
import io.harness.ccm.cluster.entities.InstanceData;
import io.harness.exception.InvalidRequestException;
import io.harness.timescaledb.DBUtils;
import io.harness.timescaledb.TimeScaleDBService;
import lombok.extern.slf4j.Slf4j;
import software.wings.graphql.datafetcher.AbstractStatsDataFetcherWithAggregationList;
import software.wings.graphql.datafetcher.billing.BillingDataQueryMetadata.BillingDataMetaDataFields;
import software.wings.graphql.datafetcher.k8sLabel.K8sLabelConnectionDataFetcher;
import software.wings.graphql.schema.type.QLK8sLabel;
import software.wings.graphql.schema.type.aggregation.QLData;
import software.wings.graphql.schema.type.aggregation.QLIdFilter;
import software.wings.graphql.schema.type.aggregation.QLIdOperator;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingDataFilter;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingSortCriteria;
import software.wings.graphql.schema.type.aggregation.billing.QLCCMEntityGroupBy;
import software.wings.graphql.schema.type.aggregation.billing.QLCCMGroupBy;
import software.wings.graphql.schema.type.aggregation.billing.QLFilterValuesData;
import software.wings.graphql.schema.type.aggregation.billing.QLFilterValuesData.QLFilterValuesDataBuilder;
import software.wings.graphql.schema.type.aggregation.billing.QLFilterValuesListData;
import software.wings.graphql.schema.type.aggregation.k8sLabel.QLK8sLabelFilter;
import software.wings.graphql.schema.type.aggregation.k8sLabel.QLK8sLabelFilter.QLK8sLabelFilterBuilder;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.annotations.AuthRule;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;

@Slf4j
public class BillingStatsFilterValuesDataFetcher
    extends AbstractStatsDataFetcherWithAggregationList<QLCCMAggregationFunction, QLBillingDataFilter, QLCCMGroupBy,
        QLBillingSortCriteria> {
  @Inject private TimeScaleDBService timeScaleDBService;
  @Inject BillingDataHelper billingDataHelper;
  @Inject QLBillingStatsHelper statsHelper;
  @Inject BillingDataQueryBuilder billingDataQueryBuilder;
  @Inject K8sLabelConnectionDataFetcher k8sLabelConnectionDataFetcher;
  @Inject InstanceDataServiceImpl instanceDataService;
  @Inject BatchJobIntervalServiceImpl batchJobIntervalService;

  private static String BATCH_JOB_TYPE = "INSTANCE_BILLING";

  @Override
  @AuthRule(permissionType = PermissionType.LOGGED_IN)
  protected QLData fetch(String accountId, List<QLCCMAggregationFunction> aggregateFunction,
      List<QLBillingDataFilter> filters, List<QLCCMGroupBy> groupBy, List<QLBillingSortCriteria> sortCriteria) {
    try {
      if (timeScaleDBService.isValid()) {
        return getEntityData(accountId, filters, groupBy);
      } else {
        throw new InvalidRequestException("Cannot process request in BillingStatsFilterValuesDataFetcher");
      }
    } catch (Exception e) {
      throw new InvalidRequestException("Error while fetching billing data {}", e);
    }
  }

  protected QLFilterValuesListData getEntityData(
      @NotNull String accountId, List<QLBillingDataFilter> filters, List<QLCCMGroupBy> groupByList) {
    BillingDataQueryMetadata queryData;
    ResultSet resultSet = null;
    boolean successful = false;
    int retryCount = 0;
    List<QLCCMEntityGroupBy> groupByEntityList = billingDataQueryBuilder.getGroupByEntity(groupByList);
    List<QLCCMEntityGroupBy> groupByNodeAndPodList = new ArrayList<>();
    List<QLCCMEntityGroupBy> groupByEntityListExcludingNodeAndPod = new ArrayList<>();

    groupByEntityList.forEach(entityGroupBy -> {
      if (entityGroupBy == QLCCMEntityGroupBy.Node || entityGroupBy == QLCCMEntityGroupBy.Pod) {
        groupByNodeAndPodList.add(entityGroupBy);
      } else {
        groupByEntityListExcludingNodeAndPod.add(entityGroupBy);
      }
    });

    Set<String> instanceIds = new HashSet<>();
    if (!groupByNodeAndPodList.isEmpty()) {
      instanceIds = getInstanceIdValues(accountId,
          getFiltersForInstanceIdQuery(filters, isGroupByPodPresent(groupByNodeAndPodList)), groupByNodeAndPodList);
      filters = getFiltersExcludingInstanceTypeFilter(filters);
    }

    queryData = billingDataQueryBuilder.formFilterValuesQuery(accountId, filters, groupByEntityListExcludingNodeAndPod);
    logger.info("BillingStatsFilterValuesDataFetcher query!! {}", queryData.getQuery());

    while (!successful && retryCount < MAX_RETRY) {
      try (Connection connection = timeScaleDBService.getDBConnection();
           Statement statement = connection.createStatement()) {
        resultSet = statement.executeQuery(queryData.getQuery());
        successful = true;
        return generateFilterValuesData(queryData, resultSet, instanceIds, accountId);
      } catch (SQLException e) {
        retryCount++;
        if (retryCount >= MAX_RETRY) {
          logger.error(
              "Failed to execute query in BillingStatsFilterValuesDataFetcher, max retry count reached, query=[{}],accountId=[{}]",
              queryData.getQuery(), accountId, e);
        } else {
          logger.warn(
              "Failed to execute query in BillingStatsFilterValuesDataFetcher, query=[{}],accountId=[{}], retryCount=[{}]",
              queryData.getQuery(), accountId, retryCount);
        }
      } finally {
        DBUtils.close(resultSet);
      }
    }
    return null;
  }

  private Set<String> getInstanceIdValues(
      String accountId, List<QLBillingDataFilter> filters, List<QLCCMEntityGroupBy> groupByNodeAndPodList) {
    ResultSet resultSet = null;
    boolean successful = false;
    int retryCount = 0;
    BillingDataQueryMetadata queryData =
        billingDataQueryBuilder.formFilterValuesQuery(accountId, filters, groupByNodeAndPodList);
    logger.info("BillingStatsFilterValuesDataFetcher query to get InstanceIds!! {}", queryData.getQuery());
    Set<String> instanceIds = new HashSet<>();
    while (!successful && retryCount < MAX_RETRY) {
      try (Connection connection = timeScaleDBService.getDBConnection();
           Statement statement = connection.createStatement()) {
        resultSet = statement.executeQuery(queryData.getQuery());
        successful = true;
        while (resultSet != null && resultSet.next()) {
          for (BillingDataMetaDataFields field : queryData.getFieldNames()) {
            switch (field) {
              case INSTANCEID:
                instanceIds.add(resultSet.getString(field.getFieldName()));
                break;
              default:
                break;
            }
          }
        }
        return instanceIds;
      } catch (SQLException e) {
        if (retryCount >= MAX_RETRY) {
          logger.error(
              "Failed to execute query in BillingStatsFilterValuesDataFetcher to get InstanceIds, max retry count reached, query=[{}],accountId=[{}]",
              queryData.getQuery(), accountId, e);
        } else {
          logger.warn(
              "Failed to execute query in BillingStatsFilterValuesDataFetcher to get InstanceIds, query=[{}],accountId=[{}], retryCount=[{}]",
              queryData.getQuery(), accountId, retryCount);
        }
        retryCount++;
      } finally {
        DBUtils.close(resultSet);
      }
    }
    return instanceIds;
  }

  private QLFilterValuesListData generateFilterValuesData(BillingDataQueryMetadata queryData, ResultSet resultSet,
      Set<String> instanceIds, String accountId) throws SQLException {
    QLFilterValuesDataBuilder filterValuesDataBuilder = QLFilterValuesData.builder();
    Set<String> cloudServiceNames = new HashSet<>();
    Set<String> workloadNames = new HashSet<>();
    Set<String> launchTypes = new HashSet<>();
    Set<String> taskIds = new HashSet<>();
    Set<String> namespaces = new HashSet<>();
    Set<String> applicationIds = new HashSet<>();
    Set<String> environmentIds = new HashSet<>();
    Set<String> cloudProviders = new HashSet<>();
    Set<String> serviceIds = new HashSet<>();
    List<QLEntityData> clusters = new ArrayList<>();
    List<QLEntityData> instances = new ArrayList<>();
    List<QLK8sLabel> k8sLabels = new ArrayList<>();

    while (resultSet != null && resultSet.next()) {
      for (BillingDataMetaDataFields field : queryData.getFieldNames()) {
        switch (field) {
          case CLOUDSERVICENAME:
            cloudServiceNames.add(resultSet.getString(field.getFieldName()));
            break;
          case LAUNCHTYPE:
            launchTypes.add(resultSet.getString(field.getFieldName()));
            break;
          case TASKID:
            taskIds.add(resultSet.getString(field.getFieldName()));
            break;
          case CLUSTERID:
            String entityId = resultSet.getString(field.getFieldName());
            clusters.add(QLEntityData.builder()
                             .name(statsHelper.getEntityName(field, entityId))
                             .id(entityId)
                             .type(resultSet.getString(BillingDataMetaDataFields.CLUSTERTYPE.getFieldName()))
                             .build());
            break;
          case NAMESPACE:
            namespaces.add(resultSet.getString(field.getFieldName()));
            break;
          case WORKLOADNAME:
            workloadNames.add(resultSet.getString(field.getFieldName()));
            break;
          case APPID:
            applicationIds.add(resultSet.getString(field.getFieldName()));
            break;
          case CLOUDPROVIDERID:
            cloudProviders.add(resultSet.getString(field.getFieldName()));
            break;
          case ENVID:
            environmentIds.add(resultSet.getString(field.getFieldName()));
            break;
          case SERVICEID:
            serviceIds.add(resultSet.getString(field.getFieldName()));
            break;
          default:
            break;
        }
      }
    }

    // Fetching K8s labels if workload names are fetched
    if (!workloadNames.isEmpty()) {
      k8sLabels = k8sLabelConnectionDataFetcher.fetchAllLabels(
          Arrays.asList(prepareLabelFilters(getClusterIdsFromFilters(queryData.getFilters()),
              workloadNames.toArray(new String[0]), namespaces.toArray(new String[0]))));
    }

    if (!instanceIds.isEmpty()) {
      List<QLBillingDataFilter> filters = queryData.getFilters();
      String clusterId = BillingStatsDefaultKeys.CLUSTERID;
      for (QLBillingDataFilter filter : filters) {
        if (filter.getCluster() != null) {
          clusterId = filter.getCluster().getValues()[0];
        }
      }
      if (!clusterId.equals(BillingStatsDefaultKeys.CLUSTERID)) {
        List<InstanceData> instanceData = instanceDataService.fetchInstanceDataForGivenInstances(
            accountId, clusterId, instanceIds.stream().collect(Collectors.toList()));
        instanceData.forEach(entry
            -> instances.add(QLEntityData.builder()
                                 .name(entry.getInstanceName())
                                 .id(entry.getInstanceId())
                                 .type(BillingDataMetaDataFields.INSTANCEID.getFieldName())
                                 .build()));
      }
    }

    filterValuesDataBuilder.cloudServiceNames(getEntity(BillingDataMetaDataFields.CLOUDSERVICENAME, cloudServiceNames))
        .taskIds(getEntity(BillingDataMetaDataFields.TASKID, taskIds))
        .launchTypes(getEntity(BillingDataMetaDataFields.LAUNCHTYPE, launchTypes))
        .clusters(clusters)
        .namespaces(getEntity(BillingDataMetaDataFields.NAMESPACE, namespaces))
        .workloadNames(getEntity(BillingDataMetaDataFields.WORKLOADNAME, workloadNames))
        .applications(getEntity(BillingDataMetaDataFields.APPID, applicationIds))
        .environments(getEntity(BillingDataMetaDataFields.ENVID, environmentIds))
        .services(getEntity(BillingDataMetaDataFields.SERVICEID, serviceIds))
        .cloudProviders(getEntity(BillingDataMetaDataFields.CLOUDPROVIDERID, cloudProviders))
        .k8sLabels(k8sLabels)
        .instances(instances);

    List<QLFilterValuesData> filterValuesDataList = new ArrayList<>();
    filterValuesDataList.add(filterValuesDataBuilder.build());
    return QLFilterValuesListData.builder()
        .data(filterValuesDataList)
        .isHourlyDataPresent(batchJobIntervalService.isIntervalUnitHours(accountId, BATCH_JOB_TYPE))
        .build();
  }

  private List<QLEntityData> getEntity(BillingDataMetaDataFields field, Set<String> entityIds) {
    List<QLEntityData> entityData = new ArrayList<>();
    for (String entityId : entityIds) {
      entityData.add(QLEntityData.builder()
                         .name(statsHelper.getEntityName(field, entityId))
                         .id(entityId)
                         .type(field.toString())
                         .build());
    }
    return entityData;
  }

  private String[] getClusterIdsFromFilters(List<QLBillingDataFilter> filters) {
    List<String> clusterIds = new ArrayList<>();
    filters.forEach(filter -> {
      if (filter.getCluster() != null) {
        for (String value : filter.getCluster().getValues()) {
          clusterIds.add(value);
        }
      }
    });
    return clusterIds.toArray(new String[0]);
  }

  private QLK8sLabelFilter prepareLabelFilters(String[] clusterIds, String[] workloadNames, String[] namespaces) {
    QLK8sLabelFilterBuilder builder = QLK8sLabelFilter.builder();
    if (clusterIds.length != 0) {
      builder.cluster(QLIdFilter.builder().operator(QLIdOperator.IN).values(clusterIds).build());
    }
    if (workloadNames.length != 0) {
      builder.workloadName(QLIdFilter.builder().operator(QLIdOperator.IN).values(workloadNames).build());
    }
    if (namespaces.length != 0) {
      builder.namespace(QLIdFilter.builder().operator(QLIdOperator.IN).values(namespaces).build());
    }
    return builder.build();
  }

  private List<QLBillingDataFilter> getFiltersExcludingInstanceTypeFilter(List<QLBillingDataFilter> filters) {
    List<QLBillingDataFilter> filtersExcludingInstanceTypeFilter = new ArrayList<>();
    filters.forEach(filter -> {
      if (filter.getInstanceType() == null) {
        filtersExcludingInstanceTypeFilter.add(filter);
      }
    });
    return filtersExcludingInstanceTypeFilter;
  }

  private List<QLBillingDataFilter> getFiltersForInstanceIdQuery(
      List<QLBillingDataFilter> filters, boolean isGroupByPodPresent) {
    if (isGroupByPodPresent) {
      return filters;
    }
    List<QLBillingDataFilter> filtersForInstanceIdQuery = new ArrayList<>();
    filters.forEach(filter -> {
      if (filter.getInstanceType() != null || filter.getCluster() != null || filter.getStartTime() != null
          || filter.getEndTime() != null) {
        filtersForInstanceIdQuery.add(filter);
      }
    });
    return filtersForInstanceIdQuery;
  }

  private boolean isGroupByPodPresent(List<QLCCMEntityGroupBy> groupByNodeAndPodList) {
    for (QLCCMEntityGroupBy entityGroupBy : groupByNodeAndPodList) {
      if (entityGroupBy == QLCCMEntityGroupBy.Pod) {
        return true;
      }
    }
    return false;
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
