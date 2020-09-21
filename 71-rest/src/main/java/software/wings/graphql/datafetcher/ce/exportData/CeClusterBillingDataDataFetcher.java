package software.wings.graphql.datafetcher.ce.exportData;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.SelectedField;
import io.harness.ccm.cluster.dao.K8sWorkloadDao;
import io.harness.ccm.cluster.entities.K8sWorkload;
import io.harness.exception.InvalidRequestException;
import io.harness.timescaledb.DBUtils;
import io.harness.timescaledb.TimeScaleDBService;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.EntityType;
import software.wings.graphql.datafetcher.AbstractStatsDataFetcherWithAggregationListAndTags;
import software.wings.graphql.datafetcher.billing.BillingDataQueryMetadata.BillingDataMetaDataFields;
import software.wings.graphql.datafetcher.billing.QLBillingStatsHelper;
import software.wings.graphql.datafetcher.ce.exportData.CEExportDataQueryMetadata.CEExportDataMetadataFields;
import software.wings.graphql.datafetcher.ce.exportData.dto.QLCEAggregation;
import software.wings.graphql.datafetcher.ce.exportData.dto.QLCEData;
import software.wings.graphql.datafetcher.ce.exportData.dto.QLCEDataEntry;
import software.wings.graphql.datafetcher.ce.exportData.dto.QLCEDataEntry.QLCEDataEntryBuilder;
import software.wings.graphql.datafetcher.ce.exportData.dto.QLCEEcsEntity;
import software.wings.graphql.datafetcher.ce.exportData.dto.QLCEEntityGroupBy;
import software.wings.graphql.datafetcher.ce.exportData.dto.QLCEFilter;
import software.wings.graphql.datafetcher.ce.exportData.dto.QLCEGroupBy;
import software.wings.graphql.datafetcher.ce.exportData.dto.QLCEHarnessEntity;
import software.wings.graphql.datafetcher.ce.exportData.dto.QLCEK8sEntity;
import software.wings.graphql.datafetcher.ce.exportData.dto.QLCEK8sLabels;
import software.wings.graphql.datafetcher.ce.exportData.dto.QLCELabelAggregation;
import software.wings.graphql.datafetcher.ce.exportData.dto.QLCESelect;
import software.wings.graphql.datafetcher.ce.exportData.dto.QLCESort;
import software.wings.graphql.datafetcher.ce.exportData.dto.QLCETagAggregation;
import software.wings.graphql.datafetcher.ce.exportData.dto.QLCETagType;
import software.wings.graphql.datafetcher.ce.exportData.dto.QLCETimeAggregation;
import software.wings.graphql.schema.type.aggregation.QLData;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;

@Slf4j
public class CeClusterBillingDataDataFetcher extends AbstractStatsDataFetcherWithAggregationListAndTags<QLCEAggregation,
    QLCEFilter, QLCEGroupBy, QLCESort, QLCETagType, QLCETagAggregation, QLCELabelAggregation, QLCEEntityGroupBy> {
  @Inject private TimeScaleDBService timeScaleDBService;
  @Inject CEExportDataQueryBuilder queryBuilder;
  @Inject QLBillingStatsHelper statsHelper;
  @Inject K8sWorkloadDao dao;
  private static final int LIMIT_THRESHOLD = 100;
  private static final String SELECT = "select";
  private static final String DEFAULT_SELECTED_LABEL = "-";

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.LOGGED_IN)
  protected QLData fetch(String accountId, List<QLCEAggregation> aggregateFunction, List<QLCEFilter> filters,
      List<QLCEGroupBy> groupBy, List<QLCESort> sort, Integer limit, Integer offset) {
    return null;
  }

  @Override
  protected QLData fetchSelectedFields(String accountId, List<QLCEAggregation> aggregateFunction,
      List<QLCEFilter> filters, List<QLCEGroupBy> groupBy, List<QLCESort> sort, Integer limit, Integer offset,
      DataFetchingEnvironment dataFetchingEnvironment) {
    if (limit > LIMIT_THRESHOLD) {
      limit = LIMIT_THRESHOLD;
    }
    try {
      if (timeScaleDBService.isValid()) {
        List<String> selectedFields = getSelectedFields(dataFetchingEnvironment);
        List<String> selectedLabels = getSelectedLabelColumns(dataFetchingEnvironment);
        return getData(
            accountId, filters, aggregateFunction, groupBy, sort, limit, offset, selectedFields, selectedLabels);
      } else {
        throw new InvalidRequestException("Cannot process request in CeClusterBillingDataDataFetcher");
      }
    } catch (Exception e) {
      throw new InvalidRequestException("Error while fetching billing data in CeClusterBillingDataDataFetcher {}", e);
    }
  }

  protected QLCEData getData(@NotNull String accountId, List<QLCEFilter> filters,
      List<QLCEAggregation> aggregateFunction, List<QLCEGroupBy> groupByList, List<QLCESort> sortCriteria,
      Integer limit, Integer offset, List<String> selectedFields, List<String> selectedLabels) {
    CEExportDataQueryMetadata queryData;
    ResultSet resultSet = null;
    boolean successful = false;
    int retryCount = 0;

    if (!queryBuilder.checkTimeFilter(filters)) {
      throw new InvalidRequestException(
          "Start time and End time filters are mandatory and selected time range should be less than 7 days");
    }

    // Separating groupBys
    List<QLCEEntityGroupBy> groupByEntityList = queryBuilder.getGroupByEntity(groupByList);
    List<QLCETagAggregation> groupByTagList = getGroupByTag(groupByList);
    List<QLCELabelAggregation> groupByLabelList = getGroupByLabel(groupByList);
    QLCETimeAggregation groupByTime = queryBuilder.getGroupByTime(groupByList);

    // Getting group by tags/labels
    if (!groupByTagList.isEmpty()) {
      groupByEntityList = getGroupByEntityListFromTags(groupByList, groupByEntityList, groupByTagList);
    } else if (!groupByLabelList.isEmpty()) {
      groupByEntityList = getGroupByEntityListFromLabels(groupByList, groupByEntityList, groupByLabelList);
    }

    // Not adding limit in case of group by tags/labels
    if (!groupByTagList.isEmpty() || !groupByLabelList.isEmpty()) {
      limit = Integer.MAX_VALUE - 1;
      offset = 0;
    }

    queryData = queryBuilder.formQuery(accountId, filters, aggregateFunction, groupByEntityList, groupByTime,
        sortCriteria, limit, offset, selectedFields);

    logger.info("CeClusterBillingDataDataFetcher query!! {}", queryData.getQuery());

    while (!successful && retryCount < MAX_RETRY) {
      try (Connection connection = timeScaleDBService.getDBConnection();
           Statement statement = connection.createStatement()) {
        resultSet = statement.executeQuery(queryData.getQuery());
        successful = true;
        return generateData(queryData, resultSet, accountId, selectedLabels);
      } catch (SQLException e) {
        retryCount++;
        if (retryCount >= MAX_RETRY) {
          logger.error(
              "Failed to execute query in CeClusterBillingDataDataFetcher, max retry count reached, query=[{}],accountId=[{}]",
              queryData.getQuery(), accountId, e);
        } else {
          logger.warn(
              "Failed to execute query in CeClusterBillingDataDataFetcher, query=[{}],accountId=[{}], retryCount=[{}]",
              queryData.getQuery(), accountId, retryCount);
        }
      } finally {
        DBUtils.close(resultSet);
      }
    }
    return null;
  }

  private QLCEData generateData(CEExportDataQueryMetadata queryData, ResultSet resultSet, String accountId,
      List<String> selectedLabels) throws SQLException {
    List<QLCEDataEntry> dataEntries = new ArrayList<>();
    Set<String> workloads = new HashSet<>();
    while (resultSet != null && resultSet.next()) {
      Double totalCost = null;
      Double idleCost = null;
      Double unallocatedCost = null;
      Double systemCost = null;
      Double maxCpuUtilization = null;
      Double maxMemoryUtilization = null;
      Double avgCpuUtilization = null;
      Double avgMemoryUtilization = null;
      Double cpuRequest = null;
      Double memoryRequest = null;
      Double cpuLimit = null;
      Double memoryLimit = null;
      String region = "";
      String namespace = "";
      String workload = "";
      String node = "";
      String pod = "";
      String task = "";
      String ecsService = "";
      String launchType = "";
      String application = "";
      String service = "";
      String environment = "";
      String cluster = "";
      String instanceType = "";
      String clusterType = "";
      String instanceName = "";
      Long startTime = null;

      for (CEExportDataMetadataFields field : queryData.getFieldNames()) {
        switch (field) {
          case APPID:
            application =
                statsHelper.getEntityName(BillingDataMetaDataFields.APPID, resultSet.getString(field.getFieldName()));
            break;
          case SERVICEID:
            service = statsHelper.getEntityName(
                BillingDataMetaDataFields.SERVICEID, resultSet.getString(field.getFieldName()));
            break;
          case ENVID:
            environment =
                statsHelper.getEntityName(BillingDataMetaDataFields.ENVID, resultSet.getString(field.getFieldName()));
            break;
          case SUM:
          case TOTALCOST:
            totalCost = resultSet.getDouble(field.getFieldName());
            break;
          case IDLECOST:
            idleCost = resultSet.getDouble(field.getFieldName());
            break;
          case UNALLOCATEDCOST:
            unallocatedCost = resultSet.getDouble(field.getFieldName());
            break;
          case SYSTEMCOST:
            systemCost = resultSet.getDouble(field.getFieldName());
            break;
          case REGION:
            region = resultSet.getString(field.getFieldName());
            break;
          case TASKID:
            task = resultSet.getString(field.getFieldName());
            break;
          case CLOUDSERVICENAME:
            ecsService = resultSet.getString(field.getFieldName());
            break;
          case LAUNCHTYPE:
            launchType = resultSet.getString(field.getFieldName());
            break;
          case WORKLOADNAME:
            workload = resultSet.getString(field.getFieldName());
            workloads.add(workload);
            break;
          case NAMESPACE:
            namespace
            = resultSet.getString(field.getFieldName());
            break;
          case CLUSTERID:
            cluster = statsHelper.getEntityName(
                BillingDataMetaDataFields.CLUSTERID, resultSet.getString(field.getFieldName()));
            break;
          case CLUSTERTYPE:
            clusterType = resultSet.getString(field.getFieldName());
            break;
          case INSTANCETYPE:
            instanceType = resultSet.getString(field.getFieldName());
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
          case CPUREQUEST:
            cpuRequest = resultSet.getDouble(field.getFieldName());
            break;
          case CPULIMIT:
            cpuLimit = resultSet.getDouble(field.getFieldName());
            break;
          case MEMORYREQUEST:
            memoryRequest = resultSet.getDouble(field.getFieldName());
            break;
          case MEMORYLIMIT:
            memoryLimit = resultSet.getDouble(field.getFieldName());
            break;
          case STARTTIME:
          case TIME_SERIES:
            startTime = resultSet.getTimestamp(field.getFieldName(), utils.getDefaultCalendar()).getTime();
            break;
          case INSTANCENAME:
            instanceName = resultSet.getString(field.getFieldName());
            break;
          default:
            break;
        }
      }

      if (instanceType.equals("K8S_NODE")) {
        node = instanceName;
      } else if (instanceType.equals("K8S_POD")) {
        pod = instanceName;
      }

      final QLCEDataEntryBuilder dataEntryBuilder = QLCEDataEntry.builder();
      dataEntryBuilder.totalCost(totalCost)
          .idleCost(idleCost)
          .unallocatedCost(unallocatedCost)
          .systemCost(systemCost)
          .maxCpuUtilization(maxCpuUtilization)
          .maxMemoryUtilization(maxMemoryUtilization)
          .avgCpuUtilization(avgCpuUtilization)
          .avgMemoryUtilization(avgMemoryUtilization)
          .cpuLimit(cpuLimit)
          .memoryLimit(memoryLimit)
          .cpuRequest(cpuRequest)
          .memoryRequest(memoryRequest)
          .region(region)
          .harness(
              QLCEHarnessEntity.builder().application(application).service(service).environment(environment).build())
          .k8s(QLCEK8sEntity.builder().namespace(namespace).node(node).pod(pod).workload(workload).build())
          .ecs(QLCEEcsEntity.builder().launchType(launchType).service(ecsService).taskId(task).build())
          .cluster(cluster)
          .clusterType(clusterType)
          .instanceType(instanceType)
          .startTime(startTime);

      dataEntries.add(dataEntryBuilder.build());
    }

    if (!workloads.isEmpty() && !selectedLabels.isEmpty()) {
      List<K8sWorkload> k8sWorkloads = dao.list(accountId, workloads);
      Map<String, Map<String, String>> labelsForWorkload =
          k8sWorkloads.stream().collect(Collectors.toMap(K8sWorkload::getName, K8sWorkload::getLabels));

      dataEntries.forEach(entry -> {
        Map<String, String> labels = new HashMap<>();
        List<QLCEK8sLabels> labelValues = new ArrayList<>();
        selectedLabels.forEach(label -> labels.put(label, DEFAULT_SELECTED_LABEL));
        Map<String, String> workloadLabels = labelsForWorkload.getOrDefault(entry.getK8s().getWorkload(), null);
        if (workloadLabels != null) {
          selectedLabels.forEach(
              label -> labels.put(label, workloadLabels.getOrDefault(label, DEFAULT_SELECTED_LABEL)));
        }
        selectedLabels.forEach(
            label -> labelValues.add(QLCEK8sLabels.builder().name(label).value(labels.get(label)).build()));
        entry.getK8s().setSelectedLabels(labelValues);
      });
    }

    return QLCEData.builder().data(dataEntries).build();
  }

  private List<String> getSelectedFields(DataFetchingEnvironment dataFetchingEnvironment) {
    Set<String> selectedFields = new HashSet<>();
    List<SelectedField> selectionSet = dataFetchingEnvironment.getSelectionSet().getFields();
    selectionSet.forEach(field -> selectedFields.add(field.getName()));
    return new ArrayList<>(selectedFields);
  }

  private List<String> getSelectedLabelColumns(DataFetchingEnvironment dataFetchingEnvironment) {
    Object object = dataFetchingEnvironment.getArguments().get(SELECT);
    if (object == null) {
      return new ArrayList<>();
    }
    Collection returnCollection = Lists.newArrayList();
    Collection collection = (Collection) object;
    collection.forEach(item -> returnCollection.add(convertToObject(item)));
    List<QLCESelect> selectedLabels = (List<QLCESelect>) returnCollection;
    Set<String> labels = new HashSet<>();
    selectedLabels.forEach(entry -> labels.addAll(entry.getLabels()));
    return new ArrayList<>(labels);
  }

  private QLCESelect convertToObject(Object fromValue) {
    ObjectMapper mapper = new ObjectMapper();
    return mapper.convertValue(fromValue, QLCESelect.class);
  }

  @Override
  protected QLCETagAggregation getTagAggregation(QLCEGroupBy groupBy) {
    return groupBy.getTagAggregation();
  }

  @Override
  protected QLCELabelAggregation getLabelAggregation(QLCEGroupBy groupBy) {
    return groupBy.getLabelAggregation();
  }

  @Override
  protected QLCEEntityGroupBy getEntityAggregation(QLCEGroupBy groupBy) {
    return groupBy.getEntity();
  }

  @Override
  protected EntityType getEntityType(QLCETagType entityType) {
    return queryBuilder.getEntityType(entityType);
  }

  @Override
  protected QLCEEntityGroupBy getGroupByEntityFromTag(QLCETagAggregation groupByTag) {
    switch (groupByTag.getEntityType()) {
      case APPLICATION:
        return QLCEEntityGroupBy.Application;
      case SERVICE:
        return QLCEEntityGroupBy.Service;
      case ENVIRONMENT:
        return QLCEEntityGroupBy.Environment;
      default:
        logger.warn("Unsupported tag entity type {}", groupByTag.getEntityType());
        throw new InvalidRequestException("Unsupported entity type " + groupByTag.getEntityType());
    }
  }

  @Override
  protected QLCEEntityGroupBy getGroupByEntityFromLabel(QLCELabelAggregation groupByLabel) {
    return QLCEEntityGroupBy.Workload;
  }

  @Override
  public String getEntityType() {
    return null;
  }
}
