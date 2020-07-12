package software.wings.graphql.datafetcher.ce.exportData;

import com.google.inject.Inject;

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
import software.wings.graphql.datafetcher.ce.exportData.dto.QLCELabelAggregation;
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
import java.util.List;
import javax.validation.constraints.NotNull;

@Slf4j
public class BillingDataDataFetcher extends AbstractStatsDataFetcherWithAggregationListAndTags<QLCEAggregation,
    QLCEFilter, QLCEGroupBy, QLCESort, QLCETagType, QLCETagAggregation, QLCELabelAggregation, QLCEEntityGroupBy> {
  @Inject private TimeScaleDBService timeScaleDBService;
  @Inject CEExportDataQueryBuilder queryBuilder;
  @Inject QLBillingStatsHelper statsHelper;

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.LOGGED_IN)
  protected QLData fetch(String accountId, List<QLCEAggregation> aggregateFunction, List<QLCEFilter> filters,
      List<QLCEGroupBy> groupBy, List<QLCESort> sort, Integer limit, Integer offset) {
    try {
      if (timeScaleDBService.isValid()) {
        return getData(accountId, filters, aggregateFunction, groupBy, sort, limit, offset);
      } else {
        throw new InvalidRequestException("Cannot process request in BillingDataDataFetcher");
      }
    } catch (Exception e) {
      throw new InvalidRequestException("Error while fetching billing data in BillingDataDataFetcher {}", e);
    }
  }

  protected QLCEData getData(@NotNull String accountId, List<QLCEFilter> filters,
      List<QLCEAggregation> aggregateFunction, List<QLCEGroupBy> groupByList, List<QLCESort> sortCriteria,
      Integer limit, Integer offset) {
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

    queryData = queryBuilder.formQuery(
        accountId, filters, aggregateFunction, groupByEntityList, groupByTime, sortCriteria, limit, offset);

    logger.info("BillingDataDataFetcher query!! {}", queryData.getQuery());

    while (!successful && retryCount < MAX_RETRY) {
      try (Connection connection = timeScaleDBService.getDBConnection();
           Statement statement = connection.createStatement()) {
        resultSet = statement.executeQuery(queryData.getQuery());
        successful = true;
        return generateData(queryData, resultSet);
      } catch (SQLException e) {
        retryCount++;
        if (retryCount >= MAX_RETRY) {
          logger.error(
              "Failed to execute query in BillingDataDataFetcher, max retry count reached, query=[{}],accountId=[{}]",
              queryData.getQuery(), accountId, e);
        } else {
          logger.warn("Failed to execute query in BillingDataDataFetcher, query=[{}],accountId=[{}], retryCount=[{}]",
              queryData.getQuery(), accountId, retryCount);
        }
      } finally {
        DBUtils.close(resultSet);
      }
    }
    return null;
  }

  private QLCEData generateData(CEExportDataQueryMetadata queryData, ResultSet resultSet) throws SQLException {
    List<QLCEDataEntry> dataEntries = new ArrayList<>();
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
      String nodeId = "";
      String podId = "";
      String task = "";
      String ecsService = "";
      String launchType = "";
      String application = "";
      String service = "";
      String environment = "";
      String cluster = "";
      String clusterName = "";
      String instanceType = "";
      String clusterType = "";
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
            break;
          case NAMESPACE:
            namespace
            = resultSet.getString(field.getFieldName());
            break;
          case CLUSTERID:
            cluster = resultSet.getString(field.getFieldName());
            clusterName = statsHelper.getEntityName(BillingDataMetaDataFields.CLUSTERID, cluster);
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
          default:
            break;
        }
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
          .k8s(QLCEK8sEntity.builder().namespace(namespace).nodeId(nodeId).podId(podId).workload(workload).build())
          .ecs(QLCEEcsEntity.builder().launchType(launchType).service(ecsService).taskId(task).build())
          .cluster(cluster)
          .clusterName(clusterName)
          .clusterType(clusterType)
          .instanceType(instanceType)
          .startTime(startTime);

      dataEntries.add(dataEntryBuilder.build());
    }

    return QLCEData.builder().data(dataEntries).build();
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
