package software.wings.service.impl.instance;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.PageResponse.PageResponseBuilder.aPageResponse;
import static io.harness.beans.SearchFilter.Operator.EQ;
import static io.harness.beans.SearchFilter.Operator.HAS;
import static io.harness.beans.SearchFilter.Operator.IN;
import static io.harness.beans.WorkflowType.ORCHESTRATION;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.eraro.ErrorCode.NO_APPS_ASSIGNED;
import static io.harness.exception.WingsException.ExecutionContext.MANAGER;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.mongodb.morphia.aggregation.Accumulator.accumulator;
import static org.mongodb.morphia.aggregation.Group.grouping;
import static org.mongodb.morphia.aggregation.Projection.projection;
import static org.mongodb.morphia.query.Sort.ascending;
import static org.mongodb.morphia.query.Sort.descending;
import static software.wings.beans.Base.CREATED_AT_KEY;
import static software.wings.beans.EntityType.APPLICATION;
import static software.wings.beans.EntityType.ARTIFACT;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.EmbeddedUser;
import io.harness.beans.ExecutionStatus;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.beans.SortOrder.OrderType;
import io.harness.eraro.ErrorCode;
import io.harness.eraro.ResponseMessage;
import io.harness.exception.HarnessException;
import io.harness.exception.WingsException;
import io.harness.logging.ExceptionLogger;
import io.harness.persistence.HIterator;
import io.harness.persistence.ReadPref;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.mongodb.morphia.aggregation.AggregationPipeline;
import org.mongodb.morphia.aggregation.Group;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.Sort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Application;
import software.wings.beans.ElementExecutionSummary;
import software.wings.beans.EntityType;
import software.wings.beans.Environment;
import software.wings.beans.Environment.EnvironmentType;
import software.wings.beans.ExecutionArgs;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute.Category;
import software.wings.beans.User;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.infrastructure.instance.Instance;
import software.wings.beans.infrastructure.instance.SyncStatus;
import software.wings.beans.instance.dashboard.ArtifactSummary;
import software.wings.beans.instance.dashboard.ArtifactSummary.ArtifactSummaryBuilder;
import software.wings.beans.instance.dashboard.EntitySummary;
import software.wings.beans.instance.dashboard.EntitySummaryStats;
import software.wings.beans.instance.dashboard.EnvironmentSummary;
import software.wings.beans.instance.dashboard.EnvironmentSummary.EnvironmentSummaryBuilder;
import software.wings.beans.instance.dashboard.InstanceStats;
import software.wings.beans.instance.dashboard.InstanceStatsByArtifact;
import software.wings.beans.instance.dashboard.InstanceStatsByEnvironment;
import software.wings.beans.instance.dashboard.InstanceStatsByEnvironment.InstanceStatsByEnvironmentBuilder;
import software.wings.beans.instance.dashboard.InstanceStatsByService;
import software.wings.beans.instance.dashboard.InstanceSummaryStats;
import software.wings.beans.instance.dashboard.InstanceSummaryStatsByService;
import software.wings.beans.instance.dashboard.ServiceSummary;
import software.wings.beans.instance.dashboard.ServiceSummary.ServiceSummaryBuilder;
import software.wings.beans.instance.dashboard.service.CurrentActiveInstances;
import software.wings.beans.instance.dashboard.service.DeploymentHistory;
import software.wings.beans.instance.dashboard.service.ServiceInstanceDashboard;
import software.wings.dl.WingsPersistence;
import software.wings.security.UserRequestContext;
import software.wings.security.UserThreadLocal;
import software.wings.service.impl.instance.DashboardStatisticsServiceImpl.ServiceInstanceCount.EnvType;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.UsageRestrictionsService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.service.intfc.instance.DashboardStatisticsService;
import software.wings.service.intfc.instance.InstanceService;
import software.wings.sm.PipelineSummary;
import software.wings.utils.Validator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;

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
  @Inject private InfrastructureMappingService infraMappingService;
  @Inject private UsageRestrictionsService usageRestrictionsService;

  @Override
  public InstanceSummaryStats getAppInstanceSummaryStats(
      String accountId, List<String> appIds, List<String> groupByEntityTypes, long timestamp) {
    Query<Instance> query;
    try {
      query = getInstanceQueryAtTime(accountId, appIds, timestamp);
    } catch (HarnessException e) {
      return InstanceSummaryStats.Builder.anInstanceSummaryStats().countMap(null).totalCount(0).build();
    } catch (Exception e) {
      logger.error("Error while compiling query for getting app instance summary stats");
      return InstanceSummaryStats.Builder.anInstanceSummaryStats().countMap(null).totalCount(0).build();
    }

    long instanceCount = getInstanceCount(query);

    Map<String, List<EntitySummaryStats>> instanceSummaryMap = new HashMap<>();
    for (String groupByEntityType : groupByEntityTypes) {
      String entityIdColumn;
      String entityNameColumn;
      List<EntitySummaryStats> entitySummaryStatsList;
      if (EntityType.SERVICE.name().equals(groupByEntityType)) {
        entityIdColumn = "serviceId";
        entityNameColumn = "serviceName";
        entitySummaryStatsList = getEntitySummaryStats(entityIdColumn, entityNameColumn, groupByEntityType, query);
      } else if (EntityType.ENVIRONMENT.name().equals(groupByEntityType)) {
        // TODO: Make UI pass ENVIRONMENT_TYPE instead of ENVIRONMENT since that's what are we are really displaying
        entitySummaryStatsList = getEnvironmentTypeSummaryStats(query);
      } else if (Category.CLOUD_PROVIDER.name().equals(groupByEntityType)) {
        entityIdColumn = "computeProviderId";
        entityNameColumn = "computeProviderName";
        entitySummaryStatsList = getEntitySummaryStats(entityIdColumn, entityNameColumn, groupByEntityType, query);
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
      String entityIdColumn, String entityNameColumn, String groupByEntityType, Query<Instance> query) {
    List<EntitySummaryStats> entitySummaryStatsList = new ArrayList<>();
    wingsPersistence.getDatastore(query.getEntityClass(), ReadPref.NORMAL)
        .createAggregation(Instance.class)
        .match(query)
        .group(Group.id(grouping(entityIdColumn)), grouping("count", accumulator("$sum", 1)),
            grouping(entityNameColumn, grouping("$first", entityNameColumn)))
        .project(projection("_id").suppress(), projection("entityId", "_id." + entityIdColumn),
            projection("entityName", entityNameColumn), projection("count"))
        .sort(descending("count"))
        .aggregate(FlatEntitySummaryStats.class)
        .forEachRemaining(flatEntitySummaryStats -> {
          EntitySummaryStats entitySummaryStats = getEntitySummaryStats(flatEntitySummaryStats, groupByEntityType);
          entitySummaryStatsList.add(entitySummaryStats);
        });
    return entitySummaryStatsList;
  }

  private long getInstanceCount(Query<Instance> query) {
    AtomicLong totalCount = new AtomicLong();
    wingsPersistence.getDatastore(query.getEntityClass(), ReadPref.NORMAL)
        .createAggregation(Instance.class)
        .match(query)
        .group("_id", grouping("count", accumulator("$sum", 1)))
        .aggregate(InstanceCount.class)
        .forEachRemaining(instanceCount -> totalCount.addAndGet(instanceCount.getCount()));
    return totalCount.get();
  }

  private List<EntitySummaryStats> getEnvironmentTypeSummaryStats(Query<Instance> query) {
    List<EntitySummaryStats> entitySummaryStatsList = Lists.newArrayList();
    wingsPersistence.getDatastore(query.getEntityClass(), ReadPref.NORMAL)
        .createAggregation(Instance.class)
        .match(query)
        .group(Group.id(grouping("envType")), grouping("count", accumulator("$sum", 1)))
        .project(projection("_id").suppress(), projection("envType", "_id.envType"), projection("count"))
        .sort(ascending("_id.envType"))
        .aggregate(EnvironmentSummaryStats.class)
        .forEachRemaining(environmentSummaryStats -> {
          String envType = environmentSummaryStats.getEnvType();
          EntitySummary entitySummary =
              EntitySummary.builder().name(envType).type(EntityType.ENVIRONMENT.name()).id(envType).build();
          EntitySummaryStats entitySummaryStats = EntitySummaryStats.Builder.anEntitySummaryStats()
                                                      .entitySummary(entitySummary)
                                                      .count(environmentSummaryStats.getCount())
                                                      .build();
          entitySummaryStatsList.add(entitySummaryStats);
        });

    return entitySummaryStatsList;
  }

  private EntitySummaryStats getEntitySummaryStats(FlatEntitySummaryStats flatEntitySummaryStats, String entityType) {
    EntitySummary entitySummary = EntitySummary.builder()
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
  public InstanceSummaryStats getServiceInstanceSummaryStats(
      String accountId, String serviceId, List<String> groupByEntityTypes, long timestamp) {
    Query<Instance> query;
    List<String> appIds = null;
    try {
      query = getInstanceQueryAtTime(accountId, appIds, timestamp);
    } catch (HarnessException e) {
      return InstanceSummaryStats.Builder.anInstanceSummaryStats().countMap(null).totalCount(0).build();
    } catch (Exception e) {
      logger.error("Error while compiling query for getting app instance summary stats");
      return InstanceSummaryStats.Builder.anInstanceSummaryStats().countMap(null).totalCount(0).build();
    }

    query.filter("serviceId", serviceId);

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

      entitySummaryStatsList = getEntitySummaryStats(entityIdColumn, entityNameColumn, groupByEntityType, query);
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
      ExceptionLogger.logProcessedMessages((WingsException) exception, MANAGER, logger);
    } else {
      logger.error("Unable to get instance stats", exception);
    }
  }

  @Override
  public @Nonnull List<Instance> getAppInstancesForAccount(String accountId, long timestamp) {
    Query<Instance> query = wingsPersistence.createQuery(Instance.class);
    return getInstancesForAccount(accountId, timestamp, query);
  }

  private List<Instance> getInstancesForAccount(String accountId, long timestamp, Query<Instance> query) {
    List<Instance> instanceList = new ArrayList<>();
    query.field("accountId").equal(accountId);
    if (timestamp > 0) {
      query.field(Instance.CREATED_AT_KEY).lessThanOrEq(timestamp);
      query.and(
          query.or(query.criteria("isDeleted").equal(false), query.criteria("deletedAt").greaterThanOrEq(timestamp)));
    } else {
      query.filter("isDeleted", false);
    }

    int counter = 0;
    try (HIterator<Instance> iterator = new HIterator<>(query.fetch())) {
      while (iterator.hasNext()) {
        Instance instance = iterator.next();
        counter++;
        instanceList.add(instance);
      }
    }

    if (isNotEmpty(instanceList)) {
      HashSet<Instance> instanceSet = new HashSet<>(instanceList);
      logger.info("Instances reported {}, set count {}", counter, instanceSet.size());
    } else {
      logger.info("Instances reported {}", counter);
    }
    return instanceList;
  }

  private long getCreatedTimeOfInstanceAtTimestamp(
      String accountId, long timestamp, Query<Instance> query, boolean oldest) {
    query.field("accountId").equal(accountId);
    query.field(Instance.CREATED_AT_KEY).lessThanOrEq(timestamp);
    query.and(
        query.or(query.criteria("isDeleted").equal(false), query.criteria("deletedAt").greaterThanOrEq(timestamp)));
    if (oldest) {
      query.order(Sort.ascending(CREATED_AT_KEY));
    } else {
      query.order(Sort.descending(CREATED_AT_KEY));
    }

    Instance instance = query.get();
    if (instance == null) {
      return timestamp;
    }

    return instance.getCreatedAt();
  }

  private PageResponse getEmptyPageResponse() {
    return aPageResponse().withResponse(Collections.emptyList()).build();
  }

  @Override
  public List<InstanceStatsByService> getAppInstanceStatsByService(
      String accountId, List<String> appIds, long timestamp) {
    Query<Instance> query;
    try {
      query = getInstanceQueryAtTime(accountId, appIds, timestamp);
    } catch (HarnessException e) {
      return Collections.emptyList();
    } catch (Exception e) {
      logger.error("Error while compiling query for instance stats by service");
      return Collections.emptyList();
    }

    List<AggregationInfo> instanceInfoList = new ArrayList<>();
    wingsPersistence.getDatastore(query.getEntityClass(), ReadPref.NORMAL)
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

  @Override
  public List<InstanceStatsByEnvironment> getServiceInstances(String accountId, String serviceId, long timestamp) {
    Query<Instance> query;
    try {
      query = getInstanceQueryAtTime(accountId, serviceId, timestamp);
    } catch (Exception e) {
      logger.error("Error while compiling query for instance stats by service");
      return Collections.emptyList();
    }

    List<ServiceAggregationInfo> serviceAggregationInfoList = new ArrayList<>();
    wingsPersistence.getDatastore(query.getEntityClass(), ReadPref.NORMAL)
        .createAggregation(Instance.class)
        .match(query)
        .group(Group.id(grouping("envId"), grouping("lastArtifactId")), grouping("count", accumulator("$sum", 1)),
            grouping("appInfo", grouping("$first", projection("id", "appId"), projection("name", "appName"))),
            grouping("envInfo",
                grouping(
                    "$first", projection("id", "envId"), projection("name", "envName"), projection("type", "envType"))),
            grouping("artifactInfo",
                grouping("$first", projection("id", "lastArtifactId"), projection("name", "lastArtifactName"),
                    projection("buildNo", "lastArtifactBuildNum"), projection("streamId", "lastArtifactStreamId"),
                    projection("deployedAt", "lastDeployedAt"), projection("sourceName", "lastArtifactSourceName"))),
            grouping(
                "instanceInfoList", grouping("$addToSet", projection("id", "_id"), projection("name", "hostName"))))
        .sort(ascending("_id.envId"), descending("count"))
        .aggregate(ServiceAggregationInfo.class)
        .forEachRemaining(serviceAggregationInfo -> { serviceAggregationInfoList.add(serviceAggregationInfo); });

    return constructInstanceStatsForService(serviceId, serviceAggregationInfoList);
  }

  @Override
  public PageResponse<InstanceSummaryStatsByService> getAppInstanceSummaryStatsByService(
      String accountId, List<String> appIds, long timestamp, int offset, int limit) {
    Query<Instance> query;
    try {
      query = getInstanceQueryAtTime(accountId, appIds, timestamp);
    } catch (HarnessException e) {
      return getEmptyPageResponse();
    } catch (Exception e) {
      logger.error("Error while compiling query for instance stats by service");
      return getEmptyPageResponse();
    }

    List<ServiceInstanceCount> instanceInfoList = new ArrayList<>();
    AggregationPipeline aggregationPipeline =
        wingsPersistence.getDatastore(query.getEntityClass(), ReadPref.NORMAL)
            .createAggregation(Instance.class)
            .match(query)
            .group(Group.id(grouping("serviceId")), grouping("count", accumulator("$sum", 1)),
                grouping("appInfo", grouping("$first", projection("id", "appId"), projection("name", "appName"))),
                grouping("serviceInfo",
                    grouping("$first", projection("id", "serviceId"), projection("name", "serviceName"))),
                grouping("envTypeList", grouping("$push", projection("type", "envType"))));
    aggregationPipeline.skip(offset);
    aggregationPipeline.limit(limit);

    aggregationPipeline.aggregate(ServiceInstanceCount.class)
        .forEachRemaining(serviceInstanceCount -> instanceInfoList.add(serviceInstanceCount));
    return constructInstanceSummaryStatsByService(instanceInfoList, offset, limit);
  }

  private PageResponse<InstanceSummaryStatsByService> constructInstanceSummaryStatsByService(
      List<ServiceInstanceCount> serviceInstanceCountList, int offset, int limit) {
    List<InstanceSummaryStatsByService> instanceSummaryStatsByServiceList =
        serviceInstanceCountList.stream().map(s -> getInstanceSummaryStatsByService(s)).collect(toList());
    return aPageResponse()
        .withResponse(instanceSummaryStatsByServiceList)
        .withOffset(Integer.toString(offset))
        .withLimit(Integer.toString(limit))
        .build();
  }

  private Query<Instance> getInstanceQueryAtTime(String accountId, String serviceId, long timestamp) {
    Query<Instance> query;
    if (timestamp > 0) {
      query = getInstanceQuery(accountId, serviceId, true);
      query.field(Instance.CREATED_AT_KEY).lessThanOrEq(timestamp);
      query.and(
          query.or(query.criteria("isDeleted").equal(false), query.criteria("deletedAt").greaterThanOrEq(timestamp)));
    } else {
      query = getInstanceQuery(accountId, serviceId, false);
    }
    return query;
  }

  private Query<Instance> getInstanceQueryAtTime(String accountId, List<String> appIds, long timestamp)
      throws HarnessException {
    Query<Instance> query;
    if (timestamp > 0) {
      query = getInstanceQuery(accountId, appIds, true, timestamp);
      query.field(Instance.CREATED_AT_KEY).lessThanOrEq(timestamp);
      query.and(
          query.or(query.criteria("isDeleted").equal(false), query.criteria("deletedAt").greaterThanOrEq(timestamp)));
    } else {
      query = getInstanceQuery(accountId, appIds, false, timestamp);
    }
    return query;
  }

  private List<InstanceStatsByEnvironment> constructInstanceStatsForService(
      String serviceId, List<ServiceAggregationInfo> serviceAggregationInfoList) {
    if (isEmpty(serviceAggregationInfoList)) {
      return Lists.newArrayList();
    }

    String appId = serviceAggregationInfoList.get(0).getAppInfo().getId();

    InstanceStatsByEnvironment currentEnv = null;
    InstanceStatsByArtifact currentArtifact;

    List<InstanceStatsByEnvironment> currentEnvList = Lists.newArrayList();
    List<InstanceStatsByArtifact> currentArtifactList = Lists.newArrayList();

    for (ServiceAggregationInfo serviceAggregationInfo : serviceAggregationInfoList) {
      int size = serviceAggregationInfo.getInstanceInfoList().size();
      List<EntitySummary> instanceList = Lists.newArrayListWithExpectedSize(size);
      for (EntitySummary instanceSummary : serviceAggregationInfo.getInstanceInfoList()) {
        // We have to clone the entity summary because type is not present in database.
        EntitySummary newInstanceSummary = EntitySummary.builder()
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

      if (currentEnv == null || !compareEnvironment(currentEnv, serviceAggregationInfo.getEnvInfo())) {
        currentArtifactList = Lists.newArrayList();
        currentEnv =
            getInstanceStatsByEnvironment(appId, serviceId, serviceAggregationInfo.getEnvInfo(), currentArtifactList);
        currentEnvList.add(currentEnv);
      }

      currentArtifact = getInstanceStatsByArtifact(serviceAggregationInfo.getArtifactInfo(), instanceStats);
      currentArtifactList.add(currentArtifact);
    }

    return currentEnvList;
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
        EntitySummary newInstanceSummary = EntitySummary.builder()
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
            currentService.getServiceSummary().getId(), aggregationInfo.getEnvInfo(), currentArtifactList);
        currentEnvList.add(currentEnv);
      }

      currentArtifact = getInstanceStatsByArtifact(aggregationInfo.getArtifactInfo(), instanceStats);
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

  private InstanceStatsByArtifact getInstanceStatsByArtifact(ArtifactInfo artifactInfo, InstanceStats instanceStats) {
    ArtifactSummaryBuilder builder = ArtifactSummary.builder();
    builder.buildNo(artifactInfo.buildNo)
        .artifactSourceName(artifactInfo.sourceName)
        .name(artifactInfo.getName())
        .type(ARTIFACT.name())
        .id(artifactInfo.getId());
    ArtifactSummary artifactSummary = builder.build();

    InstanceStatsByArtifact.Builder artifactBuilder = InstanceStatsByArtifact.Builder.anInstanceStatsByArtifact();
    artifactBuilder.withEntitySummary(artifactSummary);
    artifactBuilder.withInstanceStats(instanceStats);
    return artifactBuilder.build();
  }

  private InstanceStatsByEnvironment getInstanceStatsByEnvironment(
      String appId, String serviceId, EnvInfo envInfo, List<InstanceStatsByArtifact> currentArtifactList) {
    EnvironmentSummaryBuilder builder = EnvironmentSummary.builder();
    builder.prod("PROD".equals(envInfo.getType()))
        .id(envInfo.getId())
        .type(EntityType.ENVIRONMENT.name())
        .name(envInfo.getName());
    List<SyncStatus> syncStatusList = instanceService.getSyncStatus(appId, serviceId, envInfo.getId());
    InstanceStatsByEnvironmentBuilder instanceStatsByEnvironmentBuilder =
        InstanceStatsByEnvironment.builder()
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
    ServiceSummaryBuilder serviceBuilder = ServiceSummary.builder();
    EntitySummary serviceInfo = instanceInfo.getServiceInfo();
    EntitySummary appInfo = instanceInfo.getAppInfo();
    EntitySummary appSummary =
        EntitySummary.builder().name(appInfo.getName()).id(appInfo.getId()).type(APPLICATION.name()).build();

    serviceBuilder.appSummary(appSummary)
        .id(serviceInfo.getId())
        .type(EntityType.SERVICE.name())
        .name(serviceInfo.getName());

    return InstanceStatsByService.builder()
        .serviceSummary(serviceBuilder.build())
        .totalCount(totalInstanceCountForService.get())
        .instanceStatsByEnvList(currentEnvList)
        .build();
  }

  private InstanceSummaryStatsByService getInstanceSummaryStatsByService(ServiceInstanceCount serviceInstanceCount) {
    ServiceSummaryBuilder serviceBuilder = ServiceSummary.builder();
    EntitySummary serviceInfo = serviceInstanceCount.getServiceInfo();
    EntitySummary appInfo = serviceInstanceCount.getAppInfo();
    EntitySummary appSummary =
        EntitySummary.builder().name(appInfo.getName()).id(appInfo.getId()).type(APPLICATION.name()).build();

    serviceBuilder.appSummary(appSummary)
        .id(serviceInfo.getId())
        .type(EntityType.SERVICE.name())
        .name(serviceInfo.getName());

    long prodCount = 0;
    long nonprodCount = 0;

    List<EnvType> envTypeList = serviceInstanceCount.envTypeList;
    for (EnvType envType : envTypeList) {
      if (EnvironmentType.PROD.name().equals(envType.getType())) {
        ++prodCount;
      } else {
        ++nonprodCount;
      }
    }

    return InstanceSummaryStatsByService.builder()
        .serviceSummary(serviceBuilder.build())
        .totalCount(serviceInstanceCount.getCount())
        .prodCount(prodCount)
        .nonprodCount(nonprodCount)
        .build();
  }

  private boolean compareEnvironment(InstanceStatsByEnvironment currentEnv, EnvInfo envInfo) {
    return currentEnv != null && envInfo != null && envInfo.getId().equals(currentEnv.getEnvironmentSummary().getId());
  }

  private boolean compareService(InstanceStatsByService currentService, EntitySummary serviceInfo) {
    return currentService != null && serviceInfo != null
        && serviceInfo.getId().equals(currentService.getServiceSummary().getId());
  }

  @Override
  public ServiceInstanceDashboard getServiceInstanceDashboard(String accountId, String appId, String serviceId) {
    List<CurrentActiveInstances> currentActiveInstances = getCurrentActiveInstances(accountId, appId, serviceId);
    List<DeploymentHistory> deploymentHistoryList = getDeploymentHistory(appId, serviceId);
    Service service = serviceResourceService.get(appId, serviceId);
    Validator.notNullCheck("Service not found", service);
    EntitySummary serviceSummary = getEntitySummary(service.getName(), serviceId, EntityType.SERVICE.name());
    return ServiceInstanceDashboard.builder()
        .serviceSummary(serviceSummary)
        .currentActiveInstancesList(currentActiveInstances)
        .deploymentHistoryList(deploymentHistoryList)
        .build();
  }

  private List<CurrentActiveInstances> getCurrentActiveInstances(String accountId, String appId, String serviceId) {
    Query<Instance> query;
    try {
      query = getInstanceQuery(accountId, asList(appId), false, 0L);
      query.filter("serviceId", serviceId);
    } catch (Exception exception) {
      handleException(exception);
      return Lists.newArrayList();
    }

    List<AggregationInfo> instanceInfoList = new ArrayList<>();
    wingsPersistence.getDatastore(query.getEntityClass(), ReadPref.NORMAL)
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
        .forEachRemaining(instanceInfo -> instanceInfoList.add(instanceInfo));
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

      CurrentActiveInstances currentActiveInstances = CurrentActiveInstances.builder()
                                                          .artifact(artifactSummary)
                                                          .deployedAt(new Date(deployedAt))
                                                          .environment(environmentSummary)
                                                          .instanceCount(count)
                                                          .serviceInfra(serviceInfraSummary)
                                                          .build();
      currentActiveInstancesList.add(currentActiveInstances);
    }

    return currentActiveInstancesList;
  }

  @Override
  public Instance getInstanceDetails(String instanceId) {
    return instanceService.get(instanceId);
  }

  @Override
  public Set<String> getDeletedAppIds(String accountId, long timestamp) {
    Query<Instance> query = wingsPersistence.createQuery(Instance.class);
    query.project("appId", true);
    List<Instance> instancesForAccount = getInstancesForAccount(accountId, timestamp, query);
    Set<String> appIdsFromInstances = instancesForAccount.stream().map(Instance::getAppId).collect(Collectors.toSet());

    List<Application> appsByAccountId = appService.getAppsByAccountId(accountId);
    Set<String> existingApps = appsByAccountId.stream().map(Application::getUuid).collect(Collectors.toSet());
    appIdsFromInstances.removeAll(existingApps);
    return appIdsFromInstances;
  }

  @Override
  public Set<String> getDeletedAppIds(String accountId, long fromTimestamp, long toTimestamp) {
    Query<Instance> query = wingsPersistence.createQuery(Instance.class);
    query.project("appId", true);
    query.project(Instance.CREATED_AT_KEY, true);

    // Find the timestamp of oldest instance alive at fromTimestamp
    long lhsCreatedAt = getCreatedTimeOfInstanceAtTimestamp(accountId, fromTimestamp, query, true);
    // Find the timestamp of latest instance alive at toTimestamp
    long rhsCreatedAt = getCreatedTimeOfInstanceAtTimestamp(accountId, toTimestamp, query, false);

    query = wingsPersistence.createQuery(Instance.class);
    query.field("accountId").equal(accountId);
    query.field(Instance.CREATED_AT_KEY).greaterThanOrEq(lhsCreatedAt);
    query.field(Instance.CREATED_AT_KEY).lessThanOrEq(rhsCreatedAt);
    query.project("appId", true);

    List<Instance> instanceList = new ArrayList<>();
    try (HIterator<Instance> iterator = new HIterator<>(query.fetch())) {
      while (iterator.hasNext()) {
        Instance instance = iterator.next();
        instanceList.add(instance);
      }
    }

    Set<String> appIdsFromInstances = instanceList.stream().map(Instance::getAppId).collect(Collectors.toSet());

    List<Application> appsByAccountId = appService.getAppsByAccountId(accountId);
    Set<String> existingApps = appsByAccountId.stream().map(Application::getUuid).collect(Collectors.toSet());
    appIdsFromInstances.removeAll(existingApps);
    return appIdsFromInstances;
  }

  private List<DeploymentHistory> getDeploymentHistory(String appId, String serviceId) {
    List<DeploymentHistory> deploymentExecutionHistoryList = new ArrayList<>();

    PageRequest<WorkflowExecution> pageRequest = aPageRequest()
                                                     .addFilter("appId", EQ, appId)
                                                     .addFilter("workflowType", EQ, ORCHESTRATION)
                                                     .addFilter("serviceIds", HAS, serviceId)
                                                     .addOrder(WorkflowExecution.CREATED_AT_KEY, OrderType.DESC)
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
      if (pipelineSummary != null) {
        pipelineEntitySummary = getEntitySummary(
            pipelineSummary.getPipelineName(), workflowExecution.getPipelineExecutionId(), EntityType.PIPELINE.name());
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
      List<ElementExecutionSummary> serviceExecutionSummaries = workflowExecution.getServiceExecutionSummaries();

      if (isNotEmpty(serviceExecutionSummaries)) {
        // we always have one execution summary per workflow
        ElementExecutionSummary elementExecutionSummary = serviceExecutionSummaries.get(0);
        instancesCount = elementExecutionSummary.getInstancesCount();
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

        List<String> envIdList = workflowExecution.getEnvIds();
        List<EntitySummary> envList = null;
        if (isNotEmpty(envIdList)) {
          PageRequest<Environment> envPageRequest = aPageRequest()
                                                        .addFilter("_id", IN, envIdList.toArray())
                                                        .addFilter("appId", EQ, appId)
                                                        .addFieldsIncluded("_id", "name")
                                                        .build();

          PageResponse<Environment> pageResponse = environmentService.list(envPageRequest, false);

          List<Environment> environmentList = pageResponse.getResponse();
          if (isNotEmpty(environmentList)) {
            envList = environmentList.stream()
                          .map(env
                              -> EntitySummary.builder()
                                     .id(env.getUuid())
                                     .name(env.getName())
                                     .type(EntityType.ENVIRONMENT.name())
                                     .build())
                          .collect(toList());
          }
        }

        List<EntitySummary> serviceInfraList = null;
        List<String> infraMappingIdList = workflowExecution.getInfraMappingIds();
        if (isNotEmpty(infraMappingIdList)) {
          PageRequest<InfrastructureMapping> envPageRequest = aPageRequest()
                                                                  .addFilter("_id", IN, infraMappingIdList.toArray())
                                                                  .addFilter("serviceId", EQ, serviceId)
                                                                  .addFilter("appId", EQ, appId)
                                                                  .addFieldsIncluded("_id", "name")
                                                                  .build();

          PageResponse<InfrastructureMapping> pageResponse = infraMappingService.list(envPageRequest);

          List<InfrastructureMapping> infraList = pageResponse.getResponse();
          if (isNotEmpty(infraList)) {
            serviceInfraList = infraList.stream()
                                   .map(infraMapping
                                       -> EntitySummary.builder()
                                              .id(infraMapping.getUuid())
                                              .name(infraMapping.getName())
                                              .type(EntityType.INFRASTRUCTURE_MAPPING.name())
                                              .build())
                                   .collect(toList());
          }
        }

        DeploymentHistory deploymentHistory = DeploymentHistory.builder()
                                                  .artifact(artifactSummary)
                                                  .envs(envList)
                                                  .inframappings(serviceInfraList)
                                                  .deployedAt(startDate)
                                                  .instanceCount(instanceCount)
                                                  .pipeline(pipelineEntitySummary)
                                                  .status(executionStatus)
                                                  .triggeredBy(triggeredBySummary)
                                                  .workflow(workflowExecutionSummary)
                                                  .build();
        deploymentExecutionHistoryList.add(deploymentHistory);
      }
    }

    return deploymentExecutionHistoryList;
  }

  private EntitySummary getEntitySummary(String name, String id, String type) {
    return EntitySummary.builder().type(type).id(id).name(name).build();
  }

  private ArtifactSummary getArtifactSummary(String name, String id, String buildNum, String artifactSourceName) {
    ArtifactSummaryBuilder builder = ArtifactSummary.builder().buildNo(buildNum).artifactSourceName(artifactSourceName);
    return builder.type(ARTIFACT.name()).id(id).name(name).build();
  }

  private Query<Instance> getInstanceQuery(
      String accountId, List<String> appIds, boolean includeDeleted, long timestamp) throws HarnessException {
    Query query = wingsPersistence.createQuery(Instance.class);
    if (isNotEmpty(appIds)) {
      query.field("appId").in(appIds);
    } else {
      User user = UserThreadLocal.get();
      if (user != null) {
        UserRequestContext userRequestContext = user.getUserRequestContext();
        if (userRequestContext.isAppIdFilterRequired()) {
          Set<String> allowedAppIds = userRequestContext.getAppIds();

          if (includeDeleted && usageRestrictionsService.isAccountAdmin(accountId)) {
            Set<String> deletedAppIds = getDeletedAppIds(accountId, timestamp);
            if (isNotEmpty(deletedAppIds)) {
              allowedAppIds = Sets.newHashSet(allowedAppIds);
              allowedAppIds.addAll(deletedAppIds);
            }
          }

          if (isNotEmpty(allowedAppIds)) {
            // This is an optimization. Instead of a large IN() Query, if the user has access to all apps,
            // we could just pull it using accountId. For example, QA has 212 apps in our account.
            if (allowedAppIds.size() > 10) {
              List<String> allApps = appService.getAppIdsByAccountId(accountId);
              if (allowedAppIds.size() == allApps.size()) {
                query.filter("accountId", accountId);
              } else {
                query.field("appId").in(allowedAppIds);
              }
            } else {
              query.field("appId").in(allowedAppIds);
            }
          } else {
            throw new HarnessException(NO_APPS_ASSIGNED);
          }
        }
      } else {
        throw new HarnessException(NO_APPS_ASSIGNED);
      }
    }

    if (!includeDeleted) {
      query.filter("isDeleted", false);
    }

    return query;
  }

  private Query<Instance> getInstanceQuery(String accountId, String serviceId, boolean includeDeleted) {
    Query query = wingsPersistence.createQuery(Instance.class);
    if (!includeDeleted) {
      query.filter("isDeleted", false);
    }
    query.filter("accountId", accountId);
    query.filter("serviceId", serviceId);
    return query;
  }

  @Data
  @NoArgsConstructor
  public static final class ServiceInstanceCount {
    @Id private String serviceId;
    private long count;
    private List<EnvType> envTypeList;
    private EntitySummary appInfo;
    private EntitySummary serviceInfo;

    @Data
    @NoArgsConstructor
    public static final class EnvType {
      private String type;
    }
  }

  @Data
  @NoArgsConstructor
  public static final class ServiceAggregationInfo {
    @Id private ID _id;
    private EntitySummary appInfo;
    private EntitySummary infraMappingInfo;
    private EnvInfo envInfo;
    private ArtifactInfo artifactInfo;
    private List<EntitySummary> instanceInfoList;

    @Data
    @NoArgsConstructor
    public static final class ID {
      private String envId;
      private String lastArtifactId;
    }
  }

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
