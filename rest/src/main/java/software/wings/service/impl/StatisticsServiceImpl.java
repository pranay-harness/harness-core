package software.wings.service.impl;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static org.mongodb.morphia.aggregation.Group.grouping;
import static software.wings.beans.Environment.EnvironmentType.ALL;
import static software.wings.beans.Environment.EnvironmentType.NON_PROD;
import static software.wings.beans.Environment.EnvironmentType.PROD;
import static software.wings.beans.SearchFilter.Builder.aSearchFilter;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.beans.SearchFilter.Operator.IN;
import static software.wings.beans.SortOrder.Builder.aSortOrder;
import static software.wings.beans.WorkflowType.ORCHESTRATION;
import static software.wings.beans.WorkflowType.SIMPLE;
import static software.wings.beans.stats.AppKeyStatistics.AppKeyStatsBreakdown.Builder.anAppKeyStatistics;
import static software.wings.beans.stats.NotificationCount.Builder.aNotificationCount;
import static software.wings.beans.stats.TopConsumer.Builder.aTopConsumer;
import static software.wings.beans.stats.UserStatistics.Builder.anUserStatistics;
import static software.wings.dl.PageRequest.Builder.aPageRequest;
import static software.wings.sm.ExecutionStatus.FAILED;
import static software.wings.sm.ExecutionStatus.SUCCESS;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.inject.Singleton;

import org.apache.commons.collections.CollectionUtils;
import org.mongodb.morphia.aggregation.Accumulator;
import org.mongodb.morphia.aggregation.Group;
import org.mongodb.morphia.aggregation.Projection;
import org.mongodb.morphia.query.Query;
import software.wings.api.ServiceElement;
import software.wings.beans.Application;
import software.wings.beans.ElementExecutionSummary;
import software.wings.beans.Environment.EnvironmentType;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.SortOrder.OrderType;
import software.wings.beans.User;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.stats.ActivityStatusAggregation;
import software.wings.beans.stats.AppKeyStatistics;
import software.wings.beans.stats.AppKeyStatistics.AppKeyStatsBreakdown;
import software.wings.beans.stats.DeploymentStatistics;
import software.wings.beans.stats.DeploymentStatistics.AggregatedDayStats;
import software.wings.beans.stats.DeploymentStatistics.AggregatedDayStats.DayStat;
import software.wings.beans.stats.ServiceInstanceStatistics;
import software.wings.beans.stats.NotificationCount;
import software.wings.beans.stats.TopConsumer;
import software.wings.beans.stats.TopConsumersStatistics;
import software.wings.beans.stats.UserStatistics;
import software.wings.beans.stats.UserStatistics.AppDeployment;
import software.wings.beans.stats.WingsStatistics;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.security.UserThreadLocal;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.NotificationService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.StatisticsService;
import software.wings.service.intfc.UserService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.InstanceStatusSummary;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.stream.IntStream;
import javax.inject.Inject;

/**
 * Created by anubhaw on 8/15/16.
 */
@Singleton
public class StatisticsServiceImpl implements StatisticsService {
  /**
   * The constant MILLIS_IN_A_DAY.
   */
  public static final long MILLIS_IN_A_DAY = 86400000;
  @Inject private AppService appService;
  @Inject private WorkflowExecutionService workflowExecutionService;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private UserService userService;
  @Inject private ExecutorService executorService;
  @Inject private NotificationService notificationService;
  @Inject private ActivityService activityService;
  @Inject private ServiceResourceService serviceResourceService;

  @Override
  public WingsStatistics getTopConsumerServices(String accountId, List<String> appIds) {
    ImmutableMap<String, Application> appIdMap;
    List<Application> applications;
    if (CollectionUtils.isEmpty(appIds)) {
      applications =
          appService.list(aPageRequest().addFilter("accountId", EQ, accountId).build(), false, 0, 0).getResponse();
    } else {
      applications =
          appService.list(aPageRequest().addFilter("appId", IN, appIds.toArray()).build(), false, 0, 0).getResponse();
    }
    appIdMap = Maps.uniqueIndex(applications, Application::getUuid);
    return new TopConsumersStatistics(getTopConsumerServicesForPastXDays(30, appIdMap));
  }
  @Override
  public WingsStatistics getTopConsumers(String accountId, List<String> appIds) {
    ImmutableMap<String, Application> appIdMap;
    List<TopConsumer> topConsumers;
    List<Application> applications;
    if (CollectionUtils.isEmpty(appIds)) {
      applications =
          appService.list(aPageRequest().addFilter("accountId", EQ, accountId).build(), false, 0, 0).getResponse();
    } else {
      applications =
          appService.list(aPageRequest().addFilter("appId", IN, appIds.toArray()).build(), false, 0, 0).getResponse();
    }
    appIdMap = Maps.uniqueIndex(applications, Application::getUuid);
    topConsumers = getTopConsumerForPastXDays(30, appIdMap.keySet())
                       .stream()
                       .filter(tc -> appIdMap.containsKey(tc.getAppId()))
                       .collect(toList());
    topConsumers.forEach(topConsumer -> topConsumer.setAppName(appIdMap.get(topConsumer.getAppId()).getName()));
    return new TopConsumersStatistics(topConsumers);
  }
  @Override
  public Map<String, AppKeyStatistics> getApplicationKeyStats(List<String> appIds, int numOfDays) {
    long fromDateEpochMilli = getEpochMilliOfStartOfDayForXDaysInPastFromNow(numOfDays);

    Map<String, AppKeyStatistics> appKeyStatisticsMap = new HashMap<>();

    PageRequest pageRequest =
        aPageRequest()
            .withLimit(PageRequest.UNLIMITED)
            .addFilter(aSearchFilter().withField("createdAt", Operator.GT, fromDateEpochMilli).build())
            .addFilter(aSearchFilter().withField("workflowType", IN, ORCHESTRATION, SIMPLE).build())
            .addFilter("appId", IN, appIds.toArray())
            .addOrder(aSortOrder().withField("createdAt", OrderType.DESC).build())
            .build();

    PageResponse<WorkflowExecution> pageResponse =
        workflowExecutionService.listExecutions(pageRequest, false, false, false, false);
    if (pageResponse != null) {
      List<WorkflowExecution> workflowExecutions = pageResponse.getResponse();

      Map<String, List<WorkflowExecution>> workflowExecutionsByApp =
          workflowExecutions.stream().collect(groupingBy(WorkflowExecution::getAppId));

      appIds.forEach(appId -> workflowExecutionsByApp.computeIfAbsent(appId, id -> asList()));

      workflowExecutionsByApp.forEach((appId, wexList) -> {
        AppKeyStatistics keyStatistics = getAppKeyStatistics(wexList);
        appKeyStatisticsMap.put(appId, keyStatistics);
      });
      appIds.forEach(appId -> appKeyStatisticsMap.computeIfAbsent(appId, v -> new AppKeyStatistics()));
    }
    return appKeyStatisticsMap;
  }

  @Override
  public AppKeyStatistics getSingleApplicationKeyStats(String appId, int numOfDays) {
    Map<String, AppKeyStatistics> appKeyStatisticsMap = getApplicationKeyStats(Arrays.asList(appId), numOfDays);
    return appKeyStatisticsMap.get(appId);
  }

  public AppKeyStatistics getAppKeyStatistics(List<WorkflowExecution> workflowExecutions) {
    AppKeyStatistics appKeyStatistics = new AppKeyStatistics();

    Map<EnvironmentType, List<WorkflowExecution>> wflExecutionByEnvType =
        workflowExecutions.parallelStream().collect(groupingBy(wex -> PROD.equals(wex.getEnvType()) ? PROD : NON_PROD));

    wflExecutionByEnvType.computeIfAbsent(PROD, v -> asList());
    wflExecutionByEnvType.computeIfAbsent(NON_PROD, v -> asList());
    wflExecutionByEnvType.forEach((environmentType, wflExecutions) -> {
      int deploymentCount = wflExecutions.size();
      int instanceCount = wflExecutions.stream()
                              .filter(wex -> wex.getServiceExecutionSummaries() != null)
                              .flatMap(wex -> wex.getServiceExecutionSummaries().stream())
                              .map(elementExecutionSummary -> elementExecutionSummary.getInstancesCount())
                              .mapToInt(i -> i)
                              .sum();
      int artifactCount =
          (int) wflExecutions.stream()
              .map(WorkflowExecution::getExecutionArgs)
              .filter(executionArgs -> executionArgs != null && executionArgs.getArtifactIdNames() != null)
              .flatMap(executionArgs -> executionArgs.getArtifactIdNames().keySet().stream())
              .distinct()
              .count();
      appKeyStatistics.getStatsMap().put(environmentType,
          anAppKeyStatistics()
              .withInstanceCount(instanceCount)
              .withDeploymentCount(deploymentCount)
              .withArtifactCount(artifactCount)
              .build());
    });
    appKeyStatistics.getStatsMap().put(
        ALL, mergeAppKeyStats(appKeyStatistics.getStatsMap().get(PROD), appKeyStatistics.getStatsMap().get(NON_PROD)));
    return appKeyStatistics;
  }
  private AppKeyStatsBreakdown mergeAppKeyStats(AppKeyStatsBreakdown prod, AppKeyStatsBreakdown nonProd) {
    return anAppKeyStatistics()
        .withInstanceCount(prod.getInstanceCount() + nonProd.getInstanceCount())
        .withDeploymentCount(prod.getDeploymentCount() + nonProd.getDeploymentCount())
        .withArtifactCount(prod.getArtifactCount() + nonProd.getArtifactCount())
        .build();
  }

  @Override
  public UserStatistics getUserStats(String accountId, List<String> appIds) {
    // TODO: Needs to see what's purpose for this.
    User user = UserThreadLocal.get();
    if (user == null) {
      return new UserStatistics();
    }
    long statsFetchedOn = user.getStatsFetchedOn();
    UserStatistics userStatistics = anUserStatistics().withLastFetchedOn(statsFetchedOn).build();

    List<String> authorizedAppIds;
    if (CollectionUtils.isEmpty(appIds)) {
      authorizedAppIds = getAppIdsForAccount(accountId);
      if (CollectionUtils.isEmpty(authorizedAppIds)) {
        return userStatistics;
      }

    } else {
      authorizedAppIds = appIds;
    }

    PageRequest pageRequest =
        aPageRequest()
            .withLimit(PageRequest.UNLIMITED)
            .addFilter(aSearchFilter().withField("createdAt", Operator.GT, statsFetchedOn).build())
            .addFilter(aSearchFilter().withField("workflowType", IN, ORCHESTRATION, SIMPLE).build())
            .addFilter("appId", IN, authorizedAppIds.toArray())
            .build();
    List<WorkflowExecution> workflowExecutions =
        workflowExecutionService.listExecutions(pageRequest, false, false, false, false).getResponse();

    Map<String, List<WorkflowExecution>> wflExecutionsByApp = new HashMap<>();
    workflowExecutions.forEach(wflExecution
        -> wflExecutionsByApp.computeIfAbsent(wflExecution.getAppId(), s -> new ArrayList<>()).add(wflExecution));

    List<AppDeployment> appDeployments = new ArrayList<>();

    wflExecutionsByApp.forEach((appId, wflExecutions) -> {
      if (wflExecutions.size() != 0) {
        String appName = wflExecutions.get(0).getAppName();

        appDeployments.add(new AppDeployment(appId, appName, wflExecutions));
      }
    });

    int deploymentCount =
        (int) appDeployments.stream().mapToInt(appDeployment -> appDeployment.getDeployments().size()).sum();

    executorService.submit(() -> userService.updateStatsFetchedOnForUser(user));
    userStatistics.setAppDeployments(appDeployments);
    userStatistics.setDeploymentCount(deploymentCount);
    return userStatistics;
  }

  private List<String> getAppIdsForAccount(String accountId) {
    List<Application> applications =
        appService.list(PageRequest.Builder.aPageRequest().addFilter("accountId", EQ, accountId).build(), false, 0, 0);
    if (applications == null) {
      return new ArrayList<>();
    } else {
      return applications.stream().map(Application::getUuid).collect(toList());
    }
  }
  @Override
  public DeploymentStatistics getDeploymentStatistics(String accountId, List<String> appIds, int numOfDays) {
    long fromDateEpochMilli = getEpochMilliOfStartOfDayForXDaysInPastFromNow(numOfDays);

    PageRequest pageRequest =
        aPageRequest()
            .withLimit(PageRequest.UNLIMITED)
            .addFilter(aSearchFilter().withField("createdAt", Operator.GT, fromDateEpochMilli).build())
            .addFilter(aSearchFilter().withField("workflowType", IN, ORCHESTRATION, SIMPLE).build())
            .addOrder(aSortOrder().withField("createdAt", OrderType.DESC).build())
            .build();
    if (CollectionUtils.isEmpty(appIds)) {
      appIds = getAppIdsForAccount(accountId);
      if (CollectionUtils.isEmpty(appIds)) {
        return null;
      }
      pageRequest.addFilter(aSearchFilter().withField("appId", IN, appIds.toArray()).build());

    } else {
      pageRequest.addFilter(aSearchFilter().withField("appId", IN, appIds.toArray()).build());
    }

    DeploymentStatistics deploymentStats = new DeploymentStatistics();
    PageResponse<WorkflowExecution> pageResponse =
        workflowExecutionService.listExecutions(pageRequest, false, false, false, false);

    if (pageResponse != null) {
      List<WorkflowExecution> workflowExecutions = pageResponse.getResponse();

      Map<EnvironmentType, List<WorkflowExecution>> wflExecutionByEnvType = workflowExecutions.parallelStream().collect(
          groupingBy(wex -> PROD.equals(wex.getEnvType()) ? PROD : NON_PROD));

      deploymentStats.getStatsMap().put(
          PROD, getDeploymentStatisticsByEnvType(numOfDays, wflExecutionByEnvType.get(EnvironmentType.PROD)));
      deploymentStats.getStatsMap().put(
          NON_PROD, getDeploymentStatisticsByEnvType(numOfDays, wflExecutionByEnvType.get(EnvironmentType.NON_PROD)));
      deploymentStats.getStatsMap().put(
          ALL, merge(deploymentStats.getStatsMap().get(PROD), deploymentStats.getStatsMap().get(NON_PROD)));
    }
    return deploymentStats;
  }

  @Override
  public ServiceInstanceStatistics getServiceInstanceStatistics(String accountId, List<String> appIds, int numOfDays) {
    long fromDateEpochMilli = getEpochMilliOfStartOfDayForXDaysInPastFromNow(numOfDays);

    PageRequest pageRequest =
        aPageRequest()
            .withLimit(PageRequest.UNLIMITED)
            .addFilter(aSearchFilter().withField("createdAt", Operator.GT, fromDateEpochMilli).build())
            .addFilter(aSearchFilter().withField("workflowType", IN, ORCHESTRATION, SIMPLE).build())
            .addOrder(aSortOrder().withField("createdAt", OrderType.DESC).build())
            .build();
    if (CollectionUtils.isEmpty(appIds)) {
      appIds = getAppIdsForAccount(accountId);
      if (CollectionUtils.isEmpty(appIds)) {
        return null;
      }
      pageRequest.addFilter(aSearchFilter().withField("appId", IN, appIds.toArray()).build());

    } else {
      pageRequest.addFilter(aSearchFilter().withField("appId", IN, appIds.toArray()).build());
    }

    ServiceInstanceStatistics instanceStats = new ServiceInstanceStatistics();
    PageResponse<WorkflowExecution> pageResponse =
        workflowExecutionService.listExecutions(pageRequest, false, false, false, false);

    if (pageResponse != null) {
      List<WorkflowExecution> workflowExecutions = pageResponse.getResponse();

      if (workflowExecutions != null) {
        Comparator<TopConsumer> byCount = Comparator.comparing(tc -> tc.getTotalCount(), Comparator.reverseOrder());

        List<TopConsumer> allTopConsumers = new ArrayList<>();
        getTopInstancesDeployed(allTopConsumers, workflowExecutions);

        allTopConsumers = allTopConsumers.stream().sorted(byCount).collect(toList());

        Map<EnvironmentType, List<WorkflowExecution>> wflExecutionByEnvType =
            workflowExecutions.parallelStream().collect(
                groupingBy(wex -> PROD.equals(wex.getEnvType()) ? PROD : NON_PROD));

        List<TopConsumer> prodTopConsumers = new ArrayList<>();
        getTopInstancesDeployed(prodTopConsumers, wflExecutionByEnvType.get(PROD));
        prodTopConsumers = prodTopConsumers.stream().sorted(byCount).collect(toList());

        List<TopConsumer> nonProdTopConsumers = new ArrayList<>();
        nonProdTopConsumers = nonProdTopConsumers.stream().sorted(byCount).collect(toList());
        getTopInstancesDeployed(nonProdTopConsumers, wflExecutionByEnvType.get(NON_PROD));
        instanceStats.getStatsMap().put(ALL, allTopConsumers);
        instanceStats.getStatsMap().put(PROD, prodTopConsumers);
        instanceStats.getStatsMap().put(NON_PROD, nonProdTopConsumers);
      }
    }
    return instanceStats;
  }
  @Override
  public NotificationCount getNotificationCount(String accountId, List<String> appIds, int minutesFromNow) {
    long queryStartEpoch = System.currentTimeMillis() - (minutesFromNow * 60 * 1000);

    PageRequest actionableNotificationRequest = aPageRequest()
                                                    .addFilter("accountId", EQ, accountId)
                                                    .addFilter("actionable", EQ, true)
                                                    .addFilter("complete", EQ, false)
                                                    .build();

    PageRequest nonActionableNotificationRequest = aPageRequest()
                                                       .addFilter("accountId", EQ, accountId)
                                                       .addFilter("createdAt", Operator.GT, queryStartEpoch)
                                                       .addFilter("actionable", EQ, false)
                                                       .build();

    PageRequest failureRequest = aPageRequest()
                                     .addFilter("createdAt", Operator.GT, queryStartEpoch)
                                     .addFilter("status", EQ, FAILED)
                                     .addFieldsIncluded("appId")
                                     .build();
    List<String> authorizedAppIds;
    if (CollectionUtils.isEmpty(appIds)) {
      authorizedAppIds = getAppIdsForAccount(accountId);
      if (!CollectionUtils.isEmpty(authorizedAppIds)) {
        failureRequest.addFilter(aSearchFilter().withField("appId", IN, authorizedAppIds.toArray()).build());
      }
    } else {
      authorizedAppIds = appIds;
      actionableNotificationRequest.addFilter("appId", authorizedAppIds.toArray(), IN);
      nonActionableNotificationRequest.addFilter("appId", authorizedAppIds.toArray(), IN);
      failureRequest.addFilter("appId", authorizedAppIds.toArray(), IN);
    }

    int actionableNotificationCount = notificationService.list(actionableNotificationRequest).getResponse().size();
    int nonActionableNotification = notificationService.list(nonActionableNotificationRequest).getResponse().size();
    int failureCount = activityService.list(failureRequest).getResponse().size();

    return aNotificationCount()
        .withCompletedNotificationsCount(nonActionableNotification)
        .withPendingNotificationsCount(actionableNotificationCount)
        .withFailureCount(failureCount)
        .build();
  }

  private AggregatedDayStats merge(AggregatedDayStats prodAggStats, AggregatedDayStats nonProdAggStats) {
    List<DayStat> dayStats = new ArrayList<>(prodAggStats.getDaysStats().size());

    IntStream.range(0, prodAggStats.getDaysStats().size()).forEach(idx -> {
      DayStat prod = prodAggStats.getDaysStats().get(idx);
      DayStat nonProd = nonProdAggStats.getDaysStats().get(idx);
      dayStats.add(
          new DayStat(prod.getTotalCount() + nonProd.getTotalCount(), prod.getFailedCount() + nonProd.getFailedCount(),
              prod.getInstancesCount() + nonProd.getInstancesCount(), prod.getDate()));
    });
    return new AggregatedDayStats(prodAggStats.getTotalCount() + nonProdAggStats.getTotalCount(),
        prodAggStats.getFailedCount() + nonProdAggStats.getFailedCount(),
        prodAggStats.getInstancesCount() + nonProdAggStats.getInstancesCount(), dayStats);
  }

  private AggregatedDayStats getDeploymentStatisticsByEnvType(
      int numOfDays, List<WorkflowExecution> workflowExecutions) {
    long fromDateEpochMilli = getEpochMilliOfStartOfDayForXDaysInPastFromNow(numOfDays);

    List<DayStat> dayStats = new ArrayList<>(numOfDays);

    Map<Long, List<WorkflowExecution>> wflExecutionByDate = new HashMap<>();
    if (workflowExecutions != null) {
      wflExecutionByDate =
          workflowExecutions.parallelStream().collect(groupingBy(wfl -> (getStartOfTheDayEpoch(wfl.getCreatedAt()))));
    }

    int aggTotalCount = 0;
    int aggFailureCount = 0;
    int aggInstanceCount = 0;

    for (int idx = 0; idx < numOfDays; idx++) {
      int totalCount = 0;
      int failureCount = 0;
      int instanceCount = 0;

      Long timeOffset = fromDateEpochMilli + idx * MILLIS_IN_A_DAY;
      List<WorkflowExecution> wflExecutions = wflExecutionByDate.get(timeOffset);
      if (wflExecutions != null) {
        totalCount = wflExecutions.size();
        failureCount = (int) wflExecutions.stream()
                           .filter(workflowExecution -> workflowExecution.getStatus().equals(FAILED))
                           .count();
        instanceCount = wflExecutions.stream()
                            .filter(wex -> wex.getServiceExecutionSummaries() != null)
                            .flatMap(wex -> wex.getServiceExecutionSummaries().stream())
                            .map(elementExecutionSummary -> elementExecutionSummary.getInstancesCount())
                            .mapToInt(i -> i)
                            .sum();
      }

      dayStats.add(new DayStat(totalCount, failureCount, instanceCount, timeOffset));
      aggTotalCount += totalCount;
      aggFailureCount += failureCount;
      aggInstanceCount += instanceCount;
    }

    return new AggregatedDayStats(aggTotalCount, aggFailureCount, aggInstanceCount, dayStats);
  }

  private List<TopConsumer> getTopConsumerForPastXDays(int days, Set<String> appIds) {
    long epochMilli = getEpochMilliOfStartOfDayForXDaysInPastFromNow(days);

    List<TopConsumer> topConsumers = new ArrayList<>();
    Query<WorkflowExecution> query = wingsPersistence.createQuery(WorkflowExecution.class)
                                         .field("appId")
                                         .in(appIds)
                                         .field("createdAt")
                                         .greaterThanOrEq(epochMilli)
                                         .field("status")
                                         .hasAnyOf(asList(ExecutionStatus.FAILED, SUCCESS))
                                         .field("workflowType")
                                         .hasAnyOf(asList(ORCHESTRATION, SIMPLE));

    wingsPersistence.getDatastore()
        .createAggregation(WorkflowExecution.class)
        .match(query)
        .group(Group.id(grouping("appId"), grouping("status")), grouping("count", new Accumulator("$sum", 1)))
        .group("_id.appId",
            grouping("status",
                grouping("$addToSet", Projection.projection("status", "_id.status"),
                    Projection.projection("count", "count"))))
        .aggregate(ActivityStatusAggregation.class)
        .forEachRemaining(activityStatusAggregation
            -> topConsumers.add(getTopConsumerFromActivityStatusAggregation(activityStatusAggregation)));
    return topConsumers;
  }

  private List<TopConsumer> getTopConsumerServicesForPastXDays(int days, Map<String, Application> appIdMap) {
    long epochMilli = getEpochMilliOfStartOfDayForXDaysInPastFromNow(days);
    List<TopConsumer> topConsumers = new ArrayList<>();
    PageRequest pageRequest =
        aPageRequest()
            .withLimit(PageRequest.UNLIMITED)
            .addFilter(aSearchFilter().withField("createdAt", Operator.GT, epochMilli).build())
            .addFilter(aSearchFilter().withField("workflowType", IN, ORCHESTRATION, SIMPLE).build())
            .addFilter(aSearchFilter().withField("status", IN, FAILED, SUCCESS).build())
            .addFilter(aSearchFilter().withField("appId", IN, appIdMap.keySet().toArray()).build())
            .build();

    PageResponse<WorkflowExecution> pageResponse =
        workflowExecutionService.listExecutions(pageRequest, false, false, false, false);
    if (pageResponse != null) {
      List<WorkflowExecution> wflExecutions = pageResponse.getResponse();
      getTopInstancesDeployed(topConsumers, wflExecutions);
    }
    Comparator<TopConsumer> byCount = Comparator.comparing(tc -> tc.getTotalCount(), Comparator.reverseOrder());
    return topConsumers.stream().sorted(byCount).collect(toList());
  }

  private void getTopInstancesDeployed(List<TopConsumer> topConsumers, List<WorkflowExecution> wflExecutions) {
    Map<String, String> serviceIdNames = new HashMap<>();
    Map<String, String> serviceAppIdMap = new HashMap<>();
    Map<String, TopConsumer> topConsumerMap = new HashMap<>();
    if (wflExecutions == null || wflExecutions.size() == 0) {
      return;
    }
    for (WorkflowExecution execution : wflExecutions) {
      if ((execution.getStatus() != ExecutionStatus.SUCCESS && execution.getStatus() != ExecutionStatus.FAILED
              && execution.getStatus() != ExecutionStatus.ABORTED && execution.getStatus() != ExecutionStatus.ERROR)
          || execution.getServiceExecutionSummaries() == null) {
        continue;
      }
      for (ElementExecutionSummary elementExecutionSummary : execution.getServiceExecutionSummaries()) {
        if (elementExecutionSummary.getInstanceStatusSummaries() == null) {
          continue;
        }
        Map<String, Map<String, ExecutionStatus>> serviceInstanceStatusMap = new HashMap<>();

        for (InstanceStatusSummary instanceStatusSummary : elementExecutionSummary.getInstanceStatusSummaries()) {
          ServiceElement serviceElement = instanceStatusSummary.getInstanceElement().getServiceTemplateElement() != null
              ? instanceStatusSummary.getInstanceElement().getServiceTemplateElement().getServiceElement()
              : null;
          if (serviceElement != null) {
            String serviceId = serviceElement.getUuid();
            serviceAppIdMap.put(serviceId, execution.getAppId());
            serviceIdNames.put(serviceId, serviceElement.getName());
            Map<String, ExecutionStatus> instancestatusMap = serviceInstanceStatusMap.get(serviceId);
            if (instancestatusMap == null) {
              instancestatusMap = new HashMap<>();
              serviceInstanceStatusMap.put(serviceId, instancestatusMap);
            }
            instancestatusMap.put(
                instanceStatusSummary.getInstanceElement().getUuid(), instanceStatusSummary.getStatus());
          }
        }
        TopConsumer topConsumer;
        for (String serviceId : serviceInstanceStatusMap.keySet()) {
          if (!topConsumerMap.containsKey(serviceId)) {
            TopConsumer tempConsumer = aTopConsumer()
                                           .withAppId(execution.getAppId())
                                           .withAppName(execution.getAppName())
                                           .withServiceId(serviceId)
                                           .withServiceName(serviceIdNames.get(serviceId))
                                           .build();
            topConsumerMap.put(serviceId, tempConsumer);
            topConsumers.add(tempConsumer);
          }
          topConsumer = topConsumerMap.get(serviceId);
          Map<String, ExecutionStatus> instancestatusMap = serviceInstanceStatusMap.get(serviceId);
          for (String instanceId : instancestatusMap.keySet()) {
            if (instancestatusMap.get(instanceId).equals(SUCCESS)) {
              topConsumer.setSuccessfulActivityCount(topConsumer.getSuccessfulActivityCount() + 1);
              topConsumer.setTotalCount(topConsumer.getTotalCount() + 1);
            } else {
              topConsumer.setFailedActivityCount(topConsumer.getFailedActivityCount() + 1);
              topConsumer.setTotalCount(topConsumer.getTotalCount() + 1);
            }
          }
        }
      }
    }
  }
  private TopConsumer getTopConsumerFromActivityStatusAggregation(ActivityStatusAggregation activityStatusAggregation) {
    TopConsumer topConsumer = aTopConsumer().withAppId(activityStatusAggregation.getAppId()).build();
    activityStatusAggregation.getStatus().stream().forEach(statusCount -> {
      if (statusCount.getStatus().equals(SUCCESS)) {
        topConsumer.setSuccessfulActivityCount(statusCount.getCount());
      } else {
        topConsumer.setFailedActivityCount(statusCount.getCount());
      }
      topConsumer.setTotalCount(topConsumer.getTotalCount() + statusCount.getCount());
    });
    return topConsumer;
  }

  private long getEpochMilliOfStartOfDayForXDaysInPastFromNow(int days) {
    return LocalDate.now(ZoneId.of("America/Los_Angeles"))
        .minus(days - 1, ChronoUnit.DAYS)
        .atStartOfDay(ZoneId.of("America/Los_Angeles"))
        .toInstant()
        .toEpochMilli();
  }

  private long getStartOfTheDayEpoch(long epoch) {
    return Instant.ofEpochMilli(epoch)
        .atZone(ZoneId.of("America/Los_Angeles"))
        .toLocalDate()
        .atStartOfDay(ZoneId.of("America/Los_Angeles"))
        .toInstant()
        .toEpochMilli();
  }
}
