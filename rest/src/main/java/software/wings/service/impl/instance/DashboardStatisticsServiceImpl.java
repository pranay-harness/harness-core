package software.wings.service.impl.instance;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.stream.Collectors.toList;
import static org.mongodb.morphia.aggregation.Accumulator.accumulator;
import static org.mongodb.morphia.aggregation.Group.grouping;
import static org.mongodb.morphia.aggregation.Projection.projection;
import static org.mongodb.morphia.query.Sort.ascending;
import static org.mongodb.morphia.query.Sort.descending;
import static software.wings.beans.EntityType.APPLICATION;
import static software.wings.beans.EntityType.ARTIFACT;
import static software.wings.beans.ErrorCode.NO_APPS_ASSIGNED;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.beans.SearchFilter.Operator.IN;
import static software.wings.beans.WorkflowType.ORCHESTRATION;
import static software.wings.beans.WorkflowType.PIPELINE;
import static software.wings.beans.instance.dashboard.service.PipelineExecutionHistory.Builder.aPipelineExecutionHistory;
import static software.wings.dl.PageRequest.PageRequestBuilder.aPageRequest;
import static software.wings.exception.WingsException.ExecutionContext.MANAGER;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.mongodb.morphia.aggregation.Group;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.ElementExecutionSummary;
import software.wings.beans.EmbeddedUser;
import software.wings.beans.EntityType;
import software.wings.beans.Environment;
import software.wings.beans.Environment.EnvironmentType;
import software.wings.beans.ErrorCode;
import software.wings.beans.ExecutionArgs;
import software.wings.beans.ResponseMessage;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute.Category;
import software.wings.beans.SortOrder.OrderType;
import software.wings.beans.User;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.infrastructure.instance.Instance;
import software.wings.beans.infrastructure.instance.SyncStatus;
import software.wings.beans.instance.dashboard.ArtifactSummary;
import software.wings.beans.instance.dashboard.EntitySummary;
import software.wings.beans.instance.dashboard.EntitySummaryStats;
import software.wings.beans.instance.dashboard.EnvironmentSummary;
import software.wings.beans.instance.dashboard.InstanceStats;
import software.wings.beans.instance.dashboard.InstanceStatsByArtifact;
import software.wings.beans.instance.dashboard.InstanceStatsByEnvironment;
import software.wings.beans.instance.dashboard.InstanceStatsByService;
import software.wings.beans.instance.dashboard.InstanceSummaryStats;
import software.wings.beans.instance.dashboard.ServiceSummary;
import software.wings.beans.instance.dashboard.service.CurrentActiveInstances;
import software.wings.beans.instance.dashboard.service.DeploymentHistory;
import software.wings.beans.instance.dashboard.service.PipelineExecutionHistory;
import software.wings.beans.instance.dashboard.service.PipelineExecutionHistory.Builder;
import software.wings.beans.instance.dashboard.service.ServiceInstanceDashboard;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.HarnessException;
import software.wings.exception.WingsException;
import software.wings.exception.WingsExceptionMapper;
import software.wings.security.UserRequestContext;
import software.wings.security.UserThreadLocal;
import software.wings.service.impl.instance.DashboardStatisticsServiceImpl.AggregationInfo.ArtifactInfo;
import software.wings.service.impl.instance.DashboardStatisticsServiceImpl.AggregationInfo.EnvInfo;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.instance.DashboardStatisticsService;
import software.wings.service.intfc.instance.InstanceService;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.InfraMappingSummary;
import software.wings.sm.PipelineSummary;
import software.wings.utils.Validator;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
/**
 * @author rktummala on 8/13/17
 */
@Singleton
public class DashboardStatisticsServiceImpl implements DashboardStatisticsService {
  private static final Logger logger = LoggerFactory.getLogger(DashboardStatisticsServiceImpl.class);
  @Inject private WingsPersistence wingsPersistence;
  @Inject private InstanceService instanceService;
  @Inject private AppService appService;
  @Inject private WorkflowExecutionService workflowExecutionService;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private EnvironmentService environmentService;

  @Override
  public InstanceSummaryStats getAppInstanceSummaryStats(List<String> appIds, List<String> groupByEntityTypes) {
    long instanceCount = 0;
    try {
      instanceCount = getInstanceCount(getQuery(appIds));
    } catch (Exception exception) {
      handleException(exception);
      return InstanceSummaryStats.Builder.anInstanceSummaryStats().countMap(null).totalCount(instanceCount).build();
    }

    Map<String, List<EntitySummaryStats>> instanceSummaryMap = new HashMap<>();
    for (String groupByEntityType : groupByEntityTypes) {
      String entityIdColumn;
      String entityNameColumn;
      List<EntitySummaryStats> entitySummaryStatsList;
      if (EntityType.SERVICE.name().equals(groupByEntityType)) {
        entityIdColumn = "serviceId";
        entityNameColumn = "serviceName";
        entitySummaryStatsList = getEntitySummaryStats(appIds, entityIdColumn, entityNameColumn, groupByEntityType);
      } else if (EntityType.ENVIRONMENT.name().equals(groupByEntityType)) {
        // TODO: Make UI pass ENVIRONMENT_TYPE instead of ENVIRONMENT since that's what are we are really displaying
        entitySummaryStatsList = getEnvironmentTypeSummaryStats(appIds);
      } else if (Category.CLOUD_PROVIDER.name().equals(groupByEntityType)) {
        entityIdColumn = "computeProviderId";
        entityNameColumn = "computeProviderName";
        entitySummaryStatsList = getEntitySummaryStats(appIds, entityIdColumn, entityNameColumn, groupByEntityType);
      } else {
        throw new WingsException("Unsupported groupBy entity type:" + groupByEntityType);
      }

      instanceSummaryMap.put(groupByEntityType, entitySummaryStatsList);
    }

    return InstanceSummaryStats.Builder.anInstanceSummaryStats()
        .countMap(instanceSummaryMap)
        .totalCount(instanceCount)
        .build();
  }

  private List<EntitySummaryStats> getEntitySummaryStats(
      List<String> appIds, String entityIdColumn, String entityNameColumn, String groupByEntityType) {
    List<EntitySummaryStats> entitySummaryStatsList = new ArrayList<>();
    Query<Instance> query;
    try {
      query = getQuery(appIds);
    } catch (Exception exception) {
      handleException(exception);
      return Lists.newArrayList();
    }

    wingsPersistence.getDatastore()
        .createAggregation(Instance.class)
        .match(query)
        .group(Group.id(grouping(entityIdColumn)), grouping("count", accumulator("$sum", 1)),
            grouping(entityNameColumn, grouping("$first", entityNameColumn)))
        .project(projection("_id").suppress(), projection("entityId", "_id." + entityIdColumn),
            projection("entityName", entityNameColumn), projection("count"))
        .sort(ascending("_id." + entityIdColumn))
        .aggregate(FlatEntitySummaryStats.class)
        .forEachRemaining(flatEntitySummaryStats -> {
          EntitySummaryStats entitySummaryStats = getEntitySummaryStats(flatEntitySummaryStats, groupByEntityType);
          entitySummaryStatsList.add(entitySummaryStats);
        });
    return entitySummaryStatsList;
  }

  private long getInstanceCount(Query<Instance> query) {
    AtomicLong totalCount = new AtomicLong();
    wingsPersistence.getDatastore()
        .createAggregation(Instance.class)
        .match(query)
        .group("_id", grouping("count", accumulator("$sum", 1)))
        .aggregate(InstanceCount.class)
        .forEachRemaining(instanceCount -> { totalCount.addAndGet(instanceCount.getCount()); });
    return totalCount.get();
  }

  private List<EntitySummaryStats> getServiceSummaryStats(
      String serviceId, String entityIdColumn, String entityNameColumn, String groupByEntityType) {
    List<EntitySummaryStats> entitySummaryStatsList = new ArrayList<>();
    Query<Instance> query = null;
    try {
      query = getQuery(null).filter("serviceId", serviceId);
    } catch (Exception exception) {
      handleException(exception);
      return Lists.newArrayList();
    }
    wingsPersistence.getDatastore()
        .createAggregation(Instance.class)
        .match(query)
        .group(Group.id(grouping(entityIdColumn)), grouping("count", accumulator("$sum", 1)),
            grouping(entityNameColumn, grouping("$first", entityNameColumn)))
        .project(projection("_id").suppress(), projection("entityId", "_id." + entityIdColumn),
            projection("entityName", entityNameColumn), projection("count"))
        .sort(ascending("_id." + entityIdColumn))
        .aggregate(FlatEntitySummaryStats.class)
        .forEachRemaining(flatEntitySummaryStats -> {
          EntitySummaryStats entitySummaryStats = getEntitySummaryStats(flatEntitySummaryStats, groupByEntityType);
          entitySummaryStatsList.add(entitySummaryStats);
        });
    return entitySummaryStatsList;
  }

  private List<EntitySummaryStats> getEnvironmentTypeSummaryStats(List<String> appIds) {
    List<EntitySummaryStats> entitySummaryStatsList = Lists.newArrayList();
    Query<Instance> query;
    try {
      query = getQuery(appIds);
    } catch (Exception exception) {
      handleException(exception);
      return Lists.newArrayList();
    }
    wingsPersistence.getDatastore()
        .createAggregation(Instance.class)
        .match(query)
        .group(Group.id(grouping("envType")), grouping("count", accumulator("$sum", 1)))
        .project(projection("_id").suppress(), projection("envType", "_id.envType"), projection("count"))
        .sort(ascending("_id.envType"))
        .aggregate(EnvironmentSummaryStats.class)
        .forEachRemaining(environmentSummaryStats -> {
          String envType = environmentSummaryStats.getEnvType();
          EntitySummary entitySummary = EntitySummary.Builder.anEntitySummary()
                                            .name(envType)
                                            .type(EntityType.ENVIRONMENT.name())
                                            .id(envType)
                                            .build();
          EntitySummaryStats entitySummaryStats = EntitySummaryStats.Builder.anEntitySummaryStats()
                                                      .entitySummary(entitySummary)
                                                      .count(environmentSummaryStats.getCount())
                                                      .build();
          entitySummaryStatsList.add(entitySummaryStats);
        });

    return entitySummaryStatsList;
  }

  private EntitySummaryStats getEntitySummaryStats(FlatEntitySummaryStats flatEntitySummaryStats, String entityType) {
    EntitySummary entitySummary = EntitySummary.Builder.anEntitySummary()
                                      .id(flatEntitySummaryStats.entityId)
                                      .name(flatEntitySummaryStats.entityName)
                                      .type(entityType)
                                      .build();
    return EntitySummaryStats.Builder.anEntitySummaryStats()
        .count(flatEntitySummaryStats.count)
        .entitySummary(entitySummary)
        .build();
  }

  @Override
  public InstanceSummaryStats getServiceInstanceSummaryStats(String serviceId, List<String> groupByEntityTypes) {
    Query<Instance> query;
    try {
      query = getQuery(null).filter("serviceId", serviceId);
    } catch (Exception exception) {
      handleException(exception);
      return InstanceSummaryStats.Builder.anInstanceSummaryStats().countMap(null).totalCount(0).build();
    }

    long instanceCount = getInstanceCount(query);
    Map<String, List<EntitySummaryStats>> instanceSummaryMap = new HashMap<>();
    for (String groupByEntityType : groupByEntityTypes) {
      String entityIdColumn;
      String entityNameColumn;
      List<EntitySummaryStats> entitySummaryStatsList;
      if (ARTIFACT.name().equals(groupByEntityType)) {
        entityIdColumn = "lastArtifactId";
        entityNameColumn = "lastArtifactBuildNum";
      } else if (EntityType.ENVIRONMENT.name().equals(groupByEntityType)) {
        entityIdColumn = "envId";
        entityNameColumn = "envName";
      } else if ("INFRASTRUCTURE".equals(groupByEntityType)) {
        entityIdColumn = "infraMappingId";
        entityNameColumn = "infraMappingType";
      } else {
        throw new WingsException("Unsupported groupBy entity type:" + groupByEntityType);
      }

      entitySummaryStatsList = getServiceSummaryStats(serviceId, entityIdColumn, entityNameColumn, groupByEntityType);
      instanceSummaryMap.put(groupByEntityType, entitySummaryStatsList);
    }

    return InstanceSummaryStats.Builder.anInstanceSummaryStats()
        .countMap(instanceSummaryMap)
        .totalCount(instanceCount)
        .build();
  }

  private void handleException(Exception exception) {
    if (exception instanceof HarnessException) {
      HarnessException harnessException = (HarnessException) exception;
      List<ResponseMessage> responseMessageList = harnessException.getResponseMessageList();
      if (isNotEmpty(responseMessageList)) {
        ResponseMessage responseMessage = responseMessageList.get(0);
        if (!responseMessage.getCode().equals(ErrorCode.NO_APPS_ASSIGNED)) {
          logger.error("Unable to get instance stats", exception);
        }
      }

    } else if (exception instanceof WingsException) {
      WingsExceptionMapper.logProcessedMessages((WingsException) exception, MANAGER, logger);
    } else {
      logger.error("Unable to get instance stats", exception);
    }
  }

  @Override
  public List<InstanceStatsByService> getAppInstanceStats(List<String> appIds) {
    Query<Instance> query;
    try {
      query = getQuery(appIds);
    } catch (Exception exception) {
      handleException(exception);
      return Lists.newArrayList();
    }

    List<AggregationInfo> instanceInfoList = new ArrayList<>();
    wingsPersistence.getDatastore()
        .createAggregation(Instance.class)
        .match(query)
        .group(Group.id(grouping("serviceId"), grouping("envId"), grouping("lastArtifactId")),
            grouping("count", accumulator("$sum", 1)),
            grouping("appInfo", grouping("$first", projection("id", "appId"), projection("name", "appName"))),
            grouping(
                "serviceInfo", grouping("$first", projection("id", "serviceId"), projection("name", "serviceName"))),
            grouping("envInfo",
                grouping(
                    "$first", projection("id", "envId"), projection("name", "envName"), projection("type", "envType"))),
            grouping("artifactInfo",
                grouping("$first", projection("id", "lastArtifactId"), projection("name", "lastArtifactName"),
                    projection("buildNo", "lastArtifactBuildNum"), projection("streamId", "lastArtifactStreamId"),
                    projection("deployedAt", "lastDeployedAt"), projection("sourceName", "lastArtifactSourceName"))),
            grouping(
                "instanceInfoList", grouping("$addToSet", projection("id", "_id"), projection("name", "hostName"))))
        .sort(ascending("_id.serviceId"), ascending("_id.envId"), descending("count"))
        .aggregate(AggregationInfo.class)
        .forEachRemaining(instanceInfo -> {
          instanceInfoList.add(instanceInfo);
          logger.info(instanceInfo.toString());
        });

    return constructInstanceStatsByService(instanceInfoList);
  }

  private List<InstanceStatsByService> constructInstanceStatsByService(List<AggregationInfo> aggregationInfoList) {
    List<InstanceStatsByService> instanceStatsByServiceList = Lists.newArrayList();
    InstanceStatsByService currentService = null;
    InstanceStatsByEnvironment currentEnv = null;
    InstanceStatsByArtifact currentArtifact;

    List<InstanceStatsByEnvironment> currentEnvList = Lists.newArrayList();
    List<InstanceStatsByArtifact> currentArtifactList = Lists.newArrayList();
    AtomicLong totalInstanceCountForService = new AtomicLong();
    for (AggregationInfo aggregationInfo : aggregationInfoList) {
      int size = aggregationInfo.getInstanceInfoList().size();
      List<EntitySummary> instanceList = Lists.newArrayListWithExpectedSize(size);
      for (EntitySummary instanceSummary : aggregationInfo.getInstanceInfoList()) {
        // We have to clone the entity summary because type is not present in database.
        EntitySummary newInstanceSummary = EntitySummary.Builder.anEntitySummary()
                                               .name(instanceSummary.getName())
                                               .id(instanceSummary.getId())
                                               .type(EntityType.INSTANCE.name())
                                               .build();
        instanceList.add(newInstanceSummary);
      }

      InstanceStats instanceStats = InstanceStats.Builder.anInstanceSummaryStats()
                                        .withEntitySummaryList(instanceList)
                                        .withTotalCount(size)
                                        .build();

      if (currentService == null) {
        currentService = getInstanceStatsByService(aggregationInfo, totalInstanceCountForService, currentEnvList);
      } else if (!compareService(currentService, aggregationInfo.getServiceInfo())) {
        currentEnvList = Lists.newArrayList();
        currentEnv = null;
        // before moving on to the next service in the result set, set the totalInstanceCount to the current service.
        // To preserve immutability, we clone and set the new count.
        currentService = currentService.clone(totalInstanceCountForService.get());
        instanceStatsByServiceList.add(currentService);
        totalInstanceCountForService = new AtomicLong();

        currentService = getInstanceStatsByService(aggregationInfo, totalInstanceCountForService, currentEnvList);
      }

      if (currentEnv == null || !compareEnvironment(currentEnv, aggregationInfo.getEnvInfo())) {
        currentArtifactList = Lists.newArrayList();
        currentEnv = getInstanceStatsByEnvironment(currentService.getServiceSummary().getAppSummary().getId(),
            currentService.getServiceSummary().getId(), aggregationInfo, currentArtifactList);
        currentEnvList.add(currentEnv);
      }

      currentArtifact = getInstanceStatsByArtifact(aggregationInfo, instanceStats);
      currentArtifactList.add(currentArtifact);
      totalInstanceCountForService.addAndGet(size);
    }

    // For the last service in the result set, the compareService() logic wouldn't trigger, we need to add the service
    // to the return list
    if (currentService != null) {
      currentService = currentService.clone(totalInstanceCountForService.get());
      instanceStatsByServiceList.add(currentService);
    }

    return instanceStatsByServiceList;
  }

  private InstanceStatsByArtifact getInstanceStatsByArtifact(
      AggregationInfo instanceInfo, InstanceStats instanceStats) {
    ArtifactInfo newArtifactInfo = instanceInfo.getArtifactInfo();
    ArtifactSummary.Builder builder = ArtifactSummary.Builder.anArtifactSummary();
    builder.buildNo(newArtifactInfo.buildNo)
        .artifactSourceName(newArtifactInfo.sourceName)
        .name(newArtifactInfo.getName())
        .type(ARTIFACT.name())
        .id(newArtifactInfo.getId());
    ArtifactSummary artifactSummary = builder.build();

    InstanceStatsByArtifact.Builder artifactBuilder = InstanceStatsByArtifact.Builder.anInstanceStatsByArtifact();
    artifactBuilder.withEntitySummary(artifactSummary);
    artifactBuilder.withInstanceStats(instanceStats);
    return artifactBuilder.build();
  }

  private InstanceStatsByEnvironment getInstanceStatsByEnvironment(
      String appId, String serviceId, AggregationInfo instanceInfo, List<InstanceStatsByArtifact> currentArtifactList) {
    EnvironmentSummary.Builder builder = EnvironmentSummary.Builder.anEnvironmentSummary();
    AggregationInfo.EnvInfo envInfo = instanceInfo.getEnvInfo();
    builder.prod("PROD".equals(envInfo.getType()))
        .id(envInfo.getId())
        .type(EntityType.ENVIRONMENT.name())
        .name(envInfo.getName());
    List<SyncStatus> syncStatusList = instanceService.getSyncStatus(appId, serviceId, envInfo.getId());
    InstanceStatsByEnvironment.Builder instanceStatsByEnvironmentBuilder =
        InstanceStatsByEnvironment.Builder.anInstanceStatsByEnvironment()
            .environmentSummary(builder.build())
            .instanceStatsByArtifactList(currentArtifactList);
    if (isNotEmpty(syncStatusList)) {
      boolean hasSyncIssues = hasSyncIssues(syncStatusList);
      instanceStatsByEnvironmentBuilder.infraMappingSyncStatusList(syncStatusList);
      instanceStatsByEnvironmentBuilder.hasSyncIssues(hasSyncIssues);
    }

    return instanceStatsByEnvironmentBuilder.build();
  }

  private boolean hasSyncIssues(List<SyncStatus> syncStatusList) {
    return syncStatusList.stream().anyMatch(
        syncStatus -> syncStatus.getLastSyncedAt() != syncStatus.getLastSuccessfullySyncedAt());
  }

  private InstanceStatsByService getInstanceStatsByService(AggregationInfo instanceInfo,
      AtomicLong totalInstanceCountForService, List<InstanceStatsByEnvironment> currentEnvList) {
    ServiceSummary.Builder serviceBuilder = ServiceSummary.Builder.aServiceSummary();
    EntitySummary serviceInfo = instanceInfo.getServiceInfo();
    EntitySummary appInfo = instanceInfo.getAppInfo();
    EntitySummary appSummary = EntitySummary.Builder.anEntitySummary()
                                   .name(appInfo.getName())
                                   .id(appInfo.getId())
                                   .type(APPLICATION.name())
                                   .build();

    serviceBuilder.appSummary(appSummary)
        .id(serviceInfo.getId())
        .type(EntityType.SERVICE.name())
        .name(serviceInfo.getName());

    return InstanceStatsByService.Builder.anInstanceStatsByService()
        .withServiceSummary(serviceBuilder.build())
        .withTotalCount(totalInstanceCountForService.get())
        .withInstanceStatsByEnvList(currentEnvList)
        .build();
  }

  private boolean compareEnvironment(InstanceStatsByEnvironment currentEnv, AggregationInfo.EnvInfo envInfo) {
    return currentEnv != null && envInfo != null && envInfo.getId().equals(currentEnv.getEnvironmentSummary().getId());
  }

  private boolean compareService(InstanceStatsByService currentService, EntitySummary serviceInfo) {
    return currentService != null && serviceInfo != null
        && serviceInfo.getId().equals(currentService.getServiceSummary().getId());
  }

  @Override
  public ServiceInstanceDashboard getServiceInstanceDashboard(String appId, String serviceId) {
    List<CurrentActiveInstances> currentActiveInstances = getCurrentActiveInstances(appId, serviceId);
    List<PipelineExecutionHistory> pipelineExecutionHistory = getPipelineExecutionHistory(appId, serviceId);
    List<DeploymentHistory> deploymentHistoryList = getDeploymentHistory(appId, serviceId);
    Service service = serviceResourceService.get(appId, serviceId);
    Validator.notNullCheck("Service not found", service);
    EntitySummary serviceSummary = getEntitySummary(service.getName(), serviceId, EntityType.SERVICE.name());
    return ServiceInstanceDashboard.Builder.aServiceInstanceDashboard()
        .withServiceSummary(serviceSummary)
        .withCurrentActiveInstancesList(currentActiveInstances)
        .withDeploymentHistoryList(deploymentHistoryList)
        .withPipelineExecutionHistoryList(pipelineExecutionHistory)
        .build();
  }

  private List<PipelineExecutionHistory> getPipelineExecutionHistory(String appId, String serviceId) {
    PageRequest<WorkflowExecution> pageRequest = aPageRequest()
                                                     .withLimit("10")
                                                     .addFilter("appId", EQ, appId)
                                                     .addFilter("workflowType", EQ, PIPELINE)
                                                     .addFilter("serviceIds", IN, serviceId)
                                                     .addOrder("createdAt", OrderType.DESC)
                                                     .build();
    PageResponse<WorkflowExecution> pageResponse = workflowExecutionService.listExecutions(pageRequest, false);

    List<PipelineExecutionHistory> pipelineExecutionHistoryList = new ArrayList<>();
    if (pageResponse == null || isEmpty(pageResponse.getResponse())) {
      return pipelineExecutionHistoryList;
    }

    for (WorkflowExecution workflowExecution : pageResponse.getResponse()) {
      try {
        List<EntitySummary> environmentList = Lists.newArrayList();
        Set<String> envEntitySummarySet = new HashSet<>();
        workflowExecution.getPipelineExecution()
            .getPipelineStageExecutions()
            .stream()
            .flatMap(pipelineStageExecution -> pipelineStageExecution.getWorkflowExecutions().stream())
            .forEach(workflowExecution1 -> {
              if (!envEntitySummarySet.contains(workflowExecution1.getEnvId())) {
                environmentList.add(getEntitySummary(
                    workflowExecution1.getEnvName(), workflowExecution1.getEnvId(), EntityType.ENVIRONMENT.name()));
                envEntitySummarySet.add(workflowExecution1.getEnvId());
              }
            });
        EntitySummary pipelineSummary =
            getEntitySummary(workflowExecution.getPipelineExecution().getPipeline().getName(),
                workflowExecution.getUuid(), EntityType.PIPELINE.name());

        ArtifactSummary artifactSummary = null;
        List<Artifact> artifacts = workflowExecution.getExecutionArgs().getArtifacts();
        if (artifacts != null) {
          Artifact artifact = workflowExecution.getExecutionArgs()
                                  .getArtifacts()
                                  .stream()
                                  .filter(artifact1 -> artifact1.getServiceIds().contains(serviceId))
                                  .findFirst()
                                  .orElse(null);
          if (artifact != null) {
            artifactSummary = getArtifactSummary(
                artifact.getDisplayName(), artifact.getUuid(), artifact.getBuildNo(), artifact.getArtifactSourceName());
          }
        }

        Builder builder = aPipelineExecutionHistory()
                              .withPipeline(pipelineSummary)
                              .withArtifact(artifactSummary)
                              .withEnvironmentList(environmentList)
                              .withStatus(workflowExecution.getStatus().name());
        if (workflowExecution.getStartTs() != null) {
          builder.withStartTime(new Date(workflowExecution.getStartTs()));
        }
        if (workflowExecution.getEndTs() != null) {
          builder.withEndTime(new Date(workflowExecution.getEndTs()));
        }
        PipelineExecutionHistory pipelineExecutionHistory = builder.build();
        pipelineExecutionHistoryList.add(pipelineExecutionHistory);
      } catch (Exception e) {
        logger.error(
            "Error in preparing PipelineExecutionHistory for workflowExecution : {}", workflowExecution.getUuid(), e);
      }
    }

    return pipelineExecutionHistoryList;
  }

  private List<CurrentActiveInstances> getCurrentActiveInstances(String appId, String serviceId) {
    Query<Instance> query;
    try {
      query = getQuery(null).filter("serviceId", serviceId);
    } catch (Exception exception) {
      handleException(exception);
      return Lists.newArrayList();
    }

    List<AggregationInfo> instanceInfoList = new ArrayList<>();
    wingsPersistence.getDatastore()
        .createAggregation(Instance.class)
        .match(query)
        .group(Group.id(grouping("envId"), grouping("infraMappingId"), grouping("lastArtifactId")),
            grouping("count", accumulator("$sum", 1)),
            grouping("appInfo", grouping("$first", projection("id", "appId"), projection("name", "appName"))),
            grouping("infraMappingInfo",
                grouping("$first", projection("id", "infraMappingId"), projection("name", "infraMappingType"))),
            grouping("envInfo",
                grouping(
                    "$first", projection("id", "envId"), projection("name", "envName"), projection("type", "envType"))),
            grouping("artifactInfo",
                grouping("$first", projection("id", "lastArtifactId"), projection("name", "lastArtifactName"),
                    projection("buildNo", "lastArtifactBuildNum"), projection("streamId", "lastArtifactStreamId"),
                    projection("deployedAt", "lastDeployedAt"), projection("sourceName", "lastArtifactSourceName"))))
        .sort(descending("count"))
        .aggregate(AggregationInfo.class)
        .forEachRemaining(instanceInfo -> {
          instanceInfoList.add(instanceInfo);
          logger.info(instanceInfo.toString());
        });
    return constructCurrentActiveInstances(instanceInfoList);
  }

  private List<CurrentActiveInstances> constructCurrentActiveInstances(List<AggregationInfo> aggregationInfoList) {
    List<CurrentActiveInstances> currentActiveInstancesList = Lists.newArrayList();
    for (AggregationInfo aggregationInfo : aggregationInfoList) {
      long count = aggregationInfo.getCount();

      EntitySummary infraMappingInfo = aggregationInfo.getInfraMappingInfo();
      Validator.notNullCheck("InfraMappingInfo", infraMappingInfo);
      EntitySummary serviceInfraSummary = getEntitySummary(
          infraMappingInfo.getName(), infraMappingInfo.getId(), EntityType.INFRASTRUCTURE_MAPPING.name());

      EnvInfo envInfo = aggregationInfo.getEnvInfo();
      Validator.notNullCheck("EnvInfo", envInfo);
      EntitySummary environmentSummary =
          getEntitySummary(envInfo.getName(), envInfo.getId(), EntityType.ENVIRONMENT.name());

      ArtifactInfo artifactInfo = aggregationInfo.getArtifactInfo();
      Validator.notNullCheck("ArtifactInfo", artifactInfo);
      ArtifactSummary artifactSummary = getArtifactSummary(
          artifactInfo.getName(), artifactInfo.getId(), artifactInfo.getBuildNo(), artifactInfo.getSourceName());

      long deployedAt = aggregationInfo.getArtifactInfo().getDeployedAt();

      CurrentActiveInstances currentActiveInstances = CurrentActiveInstances.Builder.aCurrentActiveInstances()
                                                          .withArtifact(artifactSummary)
                                                          .withDeployedAt(new Date(deployedAt))
                                                          .withEnvironment(environmentSummary)
                                                          .withInstanceCount(count)
                                                          .withServiceInfra(serviceInfraSummary)
                                                          .build();
      currentActiveInstancesList.add(currentActiveInstances);
    }

    return currentActiveInstancesList;
  }

  @Override
  public Instance getInstanceDetails(String instanceId) {
    return instanceService.get(instanceId);
  }

  private List<DeploymentHistory> getDeploymentHistory(String appId, String serviceId) {
    List<DeploymentHistory> deploymentExecutionHistoryList = new ArrayList<>();
    List<Environment> environments = environmentService.getEnvByApp(appId);
    if (isEmpty(environments)) {
      return deploymentExecutionHistoryList;
    }
    List<String> envIds = environments.stream()
                              .filter(environment -> environment.getEnvironmentType() == EnvironmentType.PROD)
                              .map(Environment::getUuid)
                              .collect(toList());

    PageRequest<WorkflowExecution> pageRequest = aPageRequest()
                                                     .addFilter("appId", EQ, appId)
                                                     .addFilter("workflowType", EQ, ORCHESTRATION)
                                                     .addFilter("serviceIds", IN, serviceId)
                                                     .addFilter("envIds", IN, envIds)
                                                     .addOrder("createdAt", OrderType.DESC)
                                                     .withLimit("10")
                                                     .build();

    List<WorkflowExecution> workflowExecutionList =
        workflowExecutionService.listExecutions(pageRequest, false).getResponse();

    if (isEmpty(workflowExecutionList)) {
      return deploymentExecutionHistoryList;
    }

    for (WorkflowExecution workflowExecution : workflowExecutionList) {
      PipelineSummary pipelineSummary = workflowExecution.getPipelineSummary();
      EntitySummary pipelineEntitySummary = null;
      if (pipelineSummary == null) {
        // This is temporary fix to work around ui bug. UI code assumed the pipeline is always present
        pipelineEntitySummary = getEntitySummary("", "dummy", EntityType.PIPELINE.name());
      } else {
        pipelineEntitySummary = getEntitySummary(
            pipelineSummary.getPipelineName(), pipelineSummary.getPipelineId(), EntityType.PIPELINE.name());
      }

      EntitySummary workflowExecutionSummary =
          getEntitySummary(workflowExecution.getName(), workflowExecution.getUuid(), EntityType.WORKFLOW.name());
      EmbeddedUser triggeredByUser = workflowExecution.getTriggeredBy();
      EntitySummary triggeredBySummary = null;
      if (triggeredByUser != null) {
        triggeredBySummary =
            getEntitySummary(triggeredByUser.getName(), triggeredByUser.getUuid(), EntityType.USER.name());
      }

      Integer instancesCount = null;
      EntitySummary infraMappingEntitySummary = null;
      List<ElementExecutionSummary> serviceExecutionSummaries = workflowExecution.getServiceExecutionSummaries();

      if (isNotEmpty(serviceExecutionSummaries)) {
        // we always have one execution summary per workflow
        ElementExecutionSummary elementExecutionSummary = serviceExecutionSummaries.get(0);
        instancesCount = elementExecutionSummary.getInstancesCount();
        List<InfraMappingSummary> infraMappingSummaries = elementExecutionSummary.getInfraMappingSummaries();
        if (isNotEmpty(infraMappingSummaries)) {
          InfraMappingSummary infraMappingSummary = infraMappingSummaries.get(0);
          infraMappingEntitySummary = getEntitySummary(infraMappingSummary.getDisplayName(),
              infraMappingSummary.getInfraMappingId(), EntityType.INFRASTRUCTURE_MAPPING.name());
        }
      }

      long instanceCount = 0L;
      if (instancesCount != null) {
        instanceCount = instancesCount.longValue();
      }

      ExecutionArgs executionArgs = workflowExecution.getExecutionArgs();
      if (executionArgs == null) {
        if (logger.isDebugEnabled()) {
          logger.debug("executionArgs is null for workflowExecution:" + workflowExecution.getName());
        }
        continue;
      }

      List<Artifact> artifacts = executionArgs.getArtifacts();
      if (artifacts == null) {
        if (logger.isDebugEnabled()) {
          logger.debug("artifacts is null for workflowExecution:" + workflowExecution.getName());
        }
        continue;
      }

      Long startTs = workflowExecution.getStartTs();
      Date startDate = null;
      if (startTs != null) {
        startDate = new Date(startTs.longValue());
      }

      ExecutionStatus status = workflowExecution.getStatus();
      String executionStatus = null;
      if (status != null) {
        executionStatus = status.name();
      }

      for (Artifact artifact : artifacts) {
        if (artifact == null) {
          continue;
        }

        List<String> serviceIds = artifact.getServiceIds();
        if (isEmpty(serviceIds)) {
          continue;
        }

        // The executionArgs contain all the artifacts involved in multiple stages of the pipeline.
        // We need to filter them down to only the ones that are mapped to the current service.
        if (!serviceIds.contains(serviceId)) {
          continue;
        }

        ArtifactSummary artifactSummary = getArtifactSummary(
            artifact.getDisplayName(), artifact.getUuid(), artifact.getBuildNo(), artifact.getArtifactSourceName());
        DeploymentHistory deploymentHistory = DeploymentHistory.Builder.aDeploymentHistory()
                                                  .withArtifact(artifactSummary)
                                                  .withDeployedAt(startDate)
                                                  .withInstanceCount(instanceCount)
                                                  .withPipeline(pipelineEntitySummary)
                                                  .withServiceInfra(infraMappingEntitySummary)
                                                  .withStatus(executionStatus)
                                                  .withTriggeredBy(triggeredBySummary)
                                                  .withWorkflow(workflowExecutionSummary)
                                                  .build();
        deploymentExecutionHistoryList.add(deploymentHistory);
      }
    }

    return deploymentExecutionHistoryList;
  }

  private EntitySummary getEntitySummary(String name, String id, String type) {
    return EntitySummary.Builder.anEntitySummary().type(type).id(id).name(name).build();
  }

  private ArtifactSummary getArtifactSummary(String name, String id, String buildNum, String artifactSourceName) {
    ArtifactSummary.Builder builder =
        ArtifactSummary.Builder.anArtifactSummary().buildNo(buildNum).artifactSourceName(artifactSourceName);
    return builder.type(ARTIFACT.name()).id(id).name(name).build();
  }

  private Query<Instance> getQuery(List<String> appIds) throws HarnessException {
    Query query = wingsPersistence.createAuthorizedQuery(Instance.class);
    if (isNotEmpty(appIds)) {
      query.field("appId").in(appIds);
    } else {
      User user = UserThreadLocal.get();
      if (user != null) {
        UserRequestContext userRequestContext = user.getUserRequestContext();
        if (userRequestContext.isAppIdFilterRequired()) {
          Set<String> allowedAppIds = userRequestContext.getAppIds();
          if (isNotEmpty(allowedAppIds)) {
            query.field("appId").in(allowedAppIds);
          } else {
            throw new HarnessException(NO_APPS_ASSIGNED);
          }
        }

      } else {
        throw new HarnessException(NO_APPS_ASSIGNED);
      }
    }

    return query;
  }

  @Data
  @NoArgsConstructor
  public static final class AggregationInfo {
    @Id private ID _id;
    private long count;
    private EntitySummary appInfo;
    private EntitySummary serviceInfo;
    private EntitySummary infraMappingInfo;
    private EnvInfo envInfo;
    private ArtifactInfo artifactInfo;
    private List<EntitySummary> instanceInfoList;

    @Data
    @NoArgsConstructor
    protected static final class EnvInfo {
      private String id;
      private String name;
      private String type;
    }

    @Data
    @NoArgsConstructor
    protected static final class ArtifactInfo {
      private String id;
      private String name;
      private String buildNo;
      private String streamId;
      private String streamName;
      private long deployedAt;
      private String sourceName;
    }

    @Data
    @NoArgsConstructor
    public static final class ID {
      private String serviceId;
      private String envId;
      private String lastArtifactId;
    }
  }

  @Data
  @NoArgsConstructor
  public static class FlatEntitySummaryStats {
    private String entityId;
    private String entityName;
    private String entityVersion;
    private int count;
  }

  @Data
  @NoArgsConstructor
  public static class EnvironmentSummaryStats {
    private String envType;
    private int count;
  }

  @Data
  @NoArgsConstructor
  public static class InstanceCount {
    private int count;
  }
}
