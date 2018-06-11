package software.wings.service.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.time.EpochUtil.PST_ZONE_ID;
import static java.util.Arrays.asList;
import static java.util.Comparator.comparing;
import static java.util.Comparator.reverseOrder;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static org.mongodb.morphia.aggregation.Group.grouping;
import static org.mongodb.morphia.aggregation.Projection.projection;
import static software.wings.beans.Environment.EnvironmentType.ALL;
import static software.wings.beans.Environment.EnvironmentType.NON_PROD;
import static software.wings.beans.Environment.EnvironmentType.PROD;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.beans.SearchFilter.Operator.GE;
import static software.wings.beans.SearchFilter.Operator.IN;
import static software.wings.beans.WorkflowType.ORCHESTRATION;
import static software.wings.beans.WorkflowType.PIPELINE;
import static software.wings.beans.WorkflowType.SIMPLE;
import static software.wings.beans.stats.AppKeyStatistics.AppKeyStatsBreakdown.Builder.anAppKeyStatistics;
import static software.wings.beans.stats.NotificationCount.Builder.aNotificationCount;
import static software.wings.beans.stats.UserStatistics.Builder.anUserStatistics;
import static software.wings.dl.PageRequest.PageRequestBuilder.aPageRequest;
import static software.wings.sm.ExecutionStatus.FAILED;
import static software.wings.sm.ExecutionStatus.SUCCESS;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Streams;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.harness.time.EpochUtil;
import org.mongodb.morphia.aggregation.Accumulator;
import org.mongodb.morphia.aggregation.Group;
import org.mongodb.morphia.query.Query;
import software.wings.api.ServiceElement;
import software.wings.beans.Application;
import software.wings.beans.ElementExecutionSummary;
import software.wings.beans.Environment.EnvironmentType;
import software.wings.beans.PipelineStageExecution;
import software.wings.beans.User;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.stats.ActivityStatusAggregation;
import software.wings.beans.stats.AppKeyStatistics;
import software.wings.beans.stats.AppKeyStatistics.AppKeyStatsBreakdown;
import software.wings.beans.stats.DeploymentStatistics;
import software.wings.beans.stats.DeploymentStatistics.AggregatedDayStats;
import software.wings.beans.stats.DeploymentStatistics.AggregatedDayStats.DayStat;
import software.wings.beans.stats.NotificationCount;
import software.wings.beans.stats.ServiceInstanceStatistics;
import software.wings.beans.stats.TopConsumer;
import software.wings.beans.stats.TopConsumersStatistics;
import software.wings.beans.stats.UserStatistics;
import software.wings.beans.stats.UserStatistics.AppDeployment;
import software.wings.beans.stats.WingsStatistics;
import software.wings.dl.HIterator;
import software.wings.dl.PageRequest;
import software.wings.dl.WingsPersistence;
import software.wings.security.UserThreadLocal;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.NotificationService;
import software.wings.service.intfc.StatisticsService;
import software.wings.service.intfc.UserService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.InstanceStatusSummary;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Created by anubhaw on 8/15/16.
 */
@Singleton
public class StatisticsServiceImpl implements StatisticsService {
  @Inject private AppService appService;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private UserService userService;
  @Inject private ExecutorService executorService;
  @Inject private NotificationService notificationService;
  @Inject private ActivityService activityService;
  @Inject private WorkflowExecutionService workflowExecutionService;

  @Override
  public WingsStatistics getTopConsumerServices(String accountId, List<String> appIds) {
    if (isEmpty(appIds)) {
      appIds = appService.getAppIdsByAccountId(accountId);
    }
    if (isEmpty(appIds)) {
      return null;
    }
    return new TopConsumersStatistics(getTopConsumerServicesForPastXDays(30, appIds));
  }

  @Override
  public WingsStatistics getTopConsumers(String accountId, List<String> appIds) {
    ImmutableMap<String, Application> appIdMap;
    List<TopConsumer> topConsumers;
    List<Application> applications;
    if (isEmpty(appIds)) {
      applications = appService.getAppsByAccountId(accountId);
    } else {
      applications = appService.list(aPageRequest().addFilter("appId", IN, appIds.toArray()).build()).getResponse();
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
    long fromDateEpochMilli =
        EpochUtil.calculateEpochMilliOfStartOfDayForXDaysInPastFromNow(numOfDays, EpochUtil.PST_ZONE_ID);

    try (HIterator<WorkflowExecution> workflowExecutionIterator =
             workflowExecutionService.obtainWorkflowExecutionIterator(appIds, fromDateEpochMilli)) {
      final Stream<WorkflowExecution> stream = Streams.stream(workflowExecutionIterator);
      return calculateStringAppKeyStatisticsMap(appIds, stream);
    }
  }

  @Override
  public Map<String, AppKeyStatistics> calculateStringAppKeyStatisticsMap(
      List<String> appIds, Stream<WorkflowExecution> stream) {
    Map<String, List<WorkflowExecution>> workflowExecutionsByApp =
        stream.collect(groupingBy(WorkflowExecution::getAppId));

    appIds.forEach(appId -> workflowExecutionsByApp.computeIfAbsent(appId, id -> asList()));

    Map<String, AppKeyStatistics> appKeyStatisticsMap = new HashMap<>();
    workflowExecutionsByApp.forEach((appId, wexList) -> {
      AppKeyStatistics keyStatistics = getAppKeyStatistics(wexList);
      appKeyStatisticsMap.put(appId, keyStatistics);
    });
    appIds.forEach(appId -> appKeyStatisticsMap.computeIfAbsent(appId, v -> new AppKeyStatistics()));
    return appKeyStatisticsMap;
  }

  @Override
  public AppKeyStatistics getSingleApplicationKeyStats(String appId, int numOfDays) {
    Map<String, AppKeyStatistics> appKeyStatisticsMap = getApplicationKeyStats(asList(appId), numOfDays);
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
                              .map(ElementExecutionSummary::getInstancesCount)
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
    if (isEmpty(appIds)) {
      authorizedAppIds = appService.getAppIdsByAccountId(accountId);
      if (isEmpty(authorizedAppIds)) {
        return userStatistics;
      }

    } else {
      authorizedAppIds = appIds;
    }
    List<WorkflowExecution> workflowExecutions =
        workflowExecutionService.obtainWorkflowExecutions(authorizedAppIds, statsFetchedOn);

    Map<String, List<WorkflowExecution>> wflExecutionsByApp = new HashMap<>();
    workflowExecutions.forEach(wflExecution
        -> wflExecutionsByApp.computeIfAbsent(wflExecution.getAppId(), s -> new ArrayList<>()).add(wflExecution));

    List<AppDeployment> appDeployments = new ArrayList<>();

    wflExecutionsByApp.forEach((appId, wflExecutions) -> {
      if (!wflExecutions.isEmpty()) {
        String appName = wflExecutions.get(0).getAppName();

        appDeployments.add(new AppDeployment(appId, appName, wflExecutions));
      }
    });

    int deploymentCount =
        appDeployments.stream().mapToInt(appDeployment -> appDeployment.getDeployments().size()).sum();

    executorService.submit(() -> userService.updateStatsFetchedOnForUser(user));
    userStatistics.setAppDeployments(appDeployments);
    userStatistics.setDeploymentCount(deploymentCount);
    return userStatistics;
  }

  @Override
  public DeploymentStatistics getDeploymentStatistics(String accountId, List<String> appIds, int numOfDays) {
    long fromDateEpochMilli = getEpochMilliPSTZone(numOfDays);
    DeploymentStatistics deploymentStats = new DeploymentStatistics();
    if (isEmpty(appIds)) {
      appIds = appService.getAppIdsByAccountId(accountId);
    }
    if (isEmpty(appIds)) {
      return deploymentStats;
    }
    List<WorkflowExecution> workflowExecutions =
        workflowExecutionService.obtainWorkflowExecutions(appIds, fromDateEpochMilli);
    Map<EnvironmentType, List<WorkflowExecution>> wflExecutionByEnvType =
        workflowExecutions.parallelStream().collect(groupingBy(wex -> PROD.equals(wex.getEnvType()) ? PROD : NON_PROD));

    deploymentStats.getStatsMap().put(
        PROD, getDeploymentStatisticsByEnvType(numOfDays, wflExecutionByEnvType.get(EnvironmentType.PROD)));
    deploymentStats.getStatsMap().put(
        NON_PROD, getDeploymentStatisticsByEnvType(numOfDays, wflExecutionByEnvType.get(EnvironmentType.NON_PROD)));
    deploymentStats.getStatsMap().put(
        ALL, merge(deploymentStats.getStatsMap().get(PROD), deploymentStats.getStatsMap().get(NON_PROD)));
    return deploymentStats;
  }

  @Override
  public ServiceInstanceStatistics getServiceInstanceStatistics(String accountId, List<String> appIds, int numOfDays) {
    long fromDateEpochMilli = getEpochMilliPSTZone(numOfDays);

    ServiceInstanceStatistics instanceStats = new ServiceInstanceStatistics();
    if (isEmpty(appIds)) {
      appIds = appService.getAppIdsByAccountId(accountId);
      if (isEmpty(appIds)) {
        return instanceStats;
      }
    }
    List<WorkflowExecution> workflowExecutions =
        workflowExecutionService.obtainWorkflowExecutions(appIds, fromDateEpochMilli);
    Comparator<TopConsumer> byCount = comparing(TopConsumer::getTotalCount, reverseOrder());

    List<TopConsumer> allTopConsumers = new ArrayList<>();
    getTopServicesDeployed(allTopConsumers, workflowExecutions);

    allTopConsumers = allTopConsumers.stream().sorted(byCount).collect(toList());

    Map<EnvironmentType, List<WorkflowExecution>> wflExecutionByEnvType =
        workflowExecutions.parallelStream().collect(groupingBy(wex -> PROD.equals(wex.getEnvType()) ? PROD : NON_PROD));

    List<TopConsumer> prodTopConsumers = new ArrayList<>();
    getTopServicesDeployed(prodTopConsumers, wflExecutionByEnvType.get(PROD));
    prodTopConsumers = prodTopConsumers.stream().sorted(byCount).collect(toList());

    List<TopConsumer> nonProdTopConsumers = new ArrayList<>();
    getTopServicesDeployed(nonProdTopConsumers, wflExecutionByEnvType.get(NON_PROD));

    nonProdTopConsumers = nonProdTopConsumers.stream().sorted(byCount).collect(toList());

    instanceStats.getStatsMap().put(ALL, allTopConsumers);
    instanceStats.getStatsMap().put(PROD, prodTopConsumers);
    instanceStats.getStatsMap().put(NON_PROD, nonProdTopConsumers);
    return instanceStats;
  }

  @SuppressFBWarnings("ICAST_INTEGER_MULTIPLY_CAST_TO_LONG")
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
                                                       .addFilter("createdAt", GE, queryStartEpoch)
                                                       .addFilter("actionable", EQ, false)
                                                       .build();

    PageRequest failureRequest = aPageRequest()
                                     .addFilter("createdAt", GE, queryStartEpoch)
                                     .addFilter("status", EQ, FAILED)
                                     .addFieldsIncluded("appId")
                                     .build();
    List<String> authorizedAppIds;
    if (isEmpty(appIds)) {
      authorizedAppIds = appService.getAppIdsByAccountId(accountId);
      if (isNotEmpty(authorizedAppIds)) {
        failureRequest.addFilter("appId", IN, authorizedAppIds.toArray());
      }
    } else {
      authorizedAppIds = appIds;
      actionableNotificationRequest.addFilter("appId", IN, authorizedAppIds.toArray());
      nonActionableNotificationRequest.addFilter("appId", IN, authorizedAppIds.toArray());
      failureRequest.addFilter("appId", IN, authorizedAppIds.toArray());
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
    List<DayStat> dayStats = new ArrayList<>(numOfDays);

    Map<Long, List<WorkflowExecution>> wflExecutionByDate = new HashMap<>();
    if (workflowExecutions != null) {
      wflExecutionByDate = workflowExecutions.parallelStream().collect(
          groupingBy(wfl -> EpochUtil.obtainStartOfTheDayEpoch(wfl.getCreatedAt(), PST_ZONE_ID)));
    }

    int aggTotalCount = 0;
    int aggFailureCount = 0;
    int aggInstanceCount = 0;

    for (int idx = 0; idx < numOfDays; idx++) {
      int totalCount = 0;
      int failureCount = 0;
      int instanceCount = 0;

      Long timeOffset = getEpochMilliPSTZone(numOfDays - idx);
      List<WorkflowExecution> wflExecutions = wflExecutionByDate.get(timeOffset);
      if (wflExecutions != null) {
        totalCount = wflExecutions.size();
        failureCount = (int) wflExecutions.stream()
                           .map(WorkflowExecution::getStatus)
                           .filter(ExecutionStatus.negativeStatuses()::contains)
                           .count();
        for (WorkflowExecution workflowExecution : wflExecutions) {
          if ((workflowExecution.getWorkflowType() == ORCHESTRATION || workflowExecution.getWorkflowType() == SIMPLE)
              && workflowExecution.getServiceExecutionSummaries() != null) {
            instanceCount += workflowExecution.getServiceExecutionSummaries()
                                 .stream()
                                 .map(ElementExecutionSummary::getInstancesCount)
                                 .mapToInt(i -> i)
                                 .sum();
          } else if (workflowExecution.getWorkflowType() == PIPELINE && workflowExecution.getPipelineExecution() != null
              && workflowExecution.getPipelineExecution().getPipelineStageExecutions() != null) {
            for (PipelineStageExecution pipelineStageExecution :
                workflowExecution.getPipelineExecution().getPipelineStageExecutions()) {
              if (pipelineStageExecution == null || pipelineStageExecution.getWorkflowExecutions() == null) {
                continue;
              }
              instanceCount +=
                  pipelineStageExecution.getWorkflowExecutions()
                      .stream()
                      .filter(workflowExecution1 -> workflowExecution1.getServiceExecutionSummaries() != null)
                      .flatMap(workflowExecution1 -> workflowExecution1.getServiceExecutionSummaries().stream())
                      .map(ElementExecutionSummary::getInstancesCount)
                      .mapToInt(i -> i)
                      .sum();
            }
          }
        }
      }

      dayStats.add(new DayStat(totalCount, failureCount, instanceCount, timeOffset));
      aggTotalCount += totalCount;
      aggFailureCount += failureCount;
      aggInstanceCount += instanceCount;
    }

    return new AggregatedDayStats(aggTotalCount, aggFailureCount, aggInstanceCount, dayStats);
  }

  private List<TopConsumer> getTopConsumerForPastXDays(int days, Set<String> appIds) {
    long epochMilli = getEpochMilliPSTZone(days);

    List<TopConsumer> topConsumers = new ArrayList<>();
    Query<WorkflowExecution> query = wingsPersistence.createQuery(WorkflowExecution.class)
                                         .field("appId")
                                         .in(appIds)
                                         .field("createdAt")
                                         .greaterThanOrEq(epochMilli)
                                         .field("status")
                                         .hasAnyOf(asList(FAILED, SUCCESS))
                                         .field("workflowType")
                                         .hasAnyOf(asList(ORCHESTRATION, SIMPLE, PIPELINE))
                                         .field("pipelineExecutionId")
                                         .doesNotExist();

    wingsPersistence.getDatastore()
        .createAggregation(WorkflowExecution.class)
        .match(query)
        .group(Group.id(grouping("appId"), grouping("status")), grouping("count", new Accumulator("$sum", 1)))
        .group("_id.appId",
            grouping("status", grouping("$addToSet", projection("status", "_id.status"), projection("count", "count"))))
        .aggregate(ActivityStatusAggregation.class)
        .forEachRemaining(activityStatusAggregation
            -> topConsumers.add(getTopConsumerFromActivityStatusAggregation(activityStatusAggregation)));
    return topConsumers;
  }

  private List<TopConsumer> getTopConsumerServicesForPastXDays(int days, List<String> appIds) {
    List<TopConsumer> topConsumers = new ArrayList<>();
    getTopInstancesDeployed(topConsumers, appIds, days);
    return topConsumers.stream().sorted(comparing(TopConsumer::getTotalCount, reverseOrder())).collect(toList());
  }

  private void getTopServicesDeployed(List<TopConsumer> topConsumers, List<WorkflowExecution> wflExecutions) {
    Map<String, TopConsumer> topConsumerMap = new HashMap<>();
    if (isEmpty(wflExecutions)) {
      return;
    }
    for (WorkflowExecution execution : wflExecutions) {
      if (!ExecutionStatus.isFinalStatus(execution.getStatus())) {
        continue;
      }
      final List<ElementExecutionSummary> serviceExecutionSummaries = new ArrayList<>();
      if (execution.getWorkflowType() == PIPELINE && execution.getPipelineExecution() != null
          && execution.getPipelineExecution().getPipelineStageExecutions() != null) {
        execution.getPipelineExecution()
            .getPipelineStageExecutions()
            .stream()
            .filter(pipelineStageExecution -> pipelineStageExecution.getWorkflowExecutions() != null)
            .flatMap(pipelineStageExecution -> pipelineStageExecution.getWorkflowExecutions().stream())
            .filter(workflowExecution -> workflowExecution.getServiceExecutionSummaries() != null)
            .forEach(workflowExecution -> {
              serviceExecutionSummaries.addAll(workflowExecution.getServiceExecutionSummaries());
            });
      } else if (execution.getServiceExecutionSummaries() != null) {
        serviceExecutionSummaries.addAll(execution.getServiceExecutionSummaries());
      }
      Map<String, ElementExecutionSummary> serviceExecutionStatusMap = new HashMap<>();
      for (ElementExecutionSummary serviceExecutionSummary : serviceExecutionSummaries) {
        if (serviceExecutionSummary.getContextElement() == null) {
          continue;
        }
        String serviceId = serviceExecutionSummary.getContextElement().getUuid();
        serviceExecutionStatusMap.put(serviceId, serviceExecutionSummary);
      }
      for (ElementExecutionSummary serviceExecutionSummary : serviceExecutionStatusMap.values()) {
        String serviceId = serviceExecutionSummary.getContextElement().getUuid();
        ExecutionStatus serviceExecutionStatus = serviceExecutionSummary.getStatus();
        if (serviceExecutionStatus == null) {
          serviceExecutionStatus = execution.getStatus();
        }
        TopConsumer topConsumer;
        if (!topConsumerMap.containsKey(serviceId)) {
          TopConsumer tempConsumer = TopConsumer.builder()
                                         .appId(execution.getAppId())
                                         .appName(execution.getAppName())
                                         .serviceId(serviceId)
                                         .serviceName(serviceExecutionSummary.getContextElement().getName())
                                         .build();
          topConsumerMap.put(serviceId, tempConsumer);
          topConsumers.add(tempConsumer);
        }
        topConsumer = topConsumerMap.get(serviceId);
        if (serviceExecutionStatus.equals(SUCCESS)) {
          topConsumer.setSuccessfulActivityCount(topConsumer.getSuccessfulActivityCount() + 1);
          topConsumer.setTotalCount(topConsumer.getTotalCount() + 1);
        } else {
          topConsumer.setFailedActivityCount(topConsumer.getFailedActivityCount() + 1);
          topConsumer.setTotalCount(topConsumer.getTotalCount() + 1);
        }
      }
    }
  }

  @SuppressFBWarnings({"UC_USELESS_OBJECT", "WMI_WRONG_MAP_ITERATOR"})
  private void getTopInstancesDeployed(List<TopConsumer> topConsumers, List<String> appIds, int days) {
    Map<String, String> serviceIdNames = new HashMap<>();
    Map<String, String> serviceAppIdMap = new HashMap<>();
    Map<String, TopConsumer> topConsumerMap = new HashMap<>();
    long epochMilli = getEpochMilliPSTZone(days);
    try (HIterator<WorkflowExecution> executionIterator =
             workflowExecutionService.obtainWorkflowExecutionIterator(appIds, epochMilli)) {
      while (executionIterator.hasNext()) {
        WorkflowExecution workflowExecution = executionIterator.next();
        if (!ExecutionStatus.isFinalStatus(workflowExecution.getStatus())
            || workflowExecution.getServiceExecutionSummaries() == null) {
          continue;
        }
        for (ElementExecutionSummary elementExecutionSummary : workflowExecution.getServiceExecutionSummaries()) {
          if (elementExecutionSummary.getInstanceStatusSummaries() == null) {
            continue;
          }
          Map<String, Map<String, ExecutionStatus>> serviceInstanceStatusMap = new HashMap<>();

          for (InstanceStatusSummary instanceStatusSummary : elementExecutionSummary.getInstanceStatusSummaries()) {
            ServiceElement serviceElement =
                instanceStatusSummary.getInstanceElement().getServiceTemplateElement() != null
                ? instanceStatusSummary.getInstanceElement().getServiceTemplateElement().getServiceElement()
                : null;
            if (serviceElement != null) {
              String serviceId = serviceElement.getUuid();
              serviceAppIdMap.put(serviceId, workflowExecution.getAppId());
              serviceIdNames.put(serviceId, serviceElement.getName());
              Map<String, ExecutionStatus> instancestatusMap =
                  serviceInstanceStatusMap.computeIfAbsent(serviceId, k -> new HashMap<>());
              instancestatusMap.put(
                  instanceStatusSummary.getInstanceElement().getUuid(), instanceStatusSummary.getStatus());
            }
          }
          TopConsumer topConsumer;
          for (String serviceId : serviceInstanceStatusMap.keySet()) {
            if (!topConsumerMap.containsKey(serviceId)) {
              TopConsumer tempConsumer = TopConsumer.builder()
                                             .appId(workflowExecution.getAppId())
                                             .appName(workflowExecution.getAppName())
                                             .serviceId(serviceId)
                                             .serviceName(serviceIdNames.get(serviceId))
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
  }

  private long getEpochMilliPSTZone(int days) {
    return EpochUtil.calculateEpochMilliOfStartOfDayForXDaysInPastFromNow(days, PST_ZONE_ID);
  }

  private TopConsumer getTopConsumerFromActivityStatusAggregation(ActivityStatusAggregation activityStatusAggregation) {
    TopConsumer topConsumer = TopConsumer.builder().appId(activityStatusAggregation.getAppId()).build();
    activityStatusAggregation.getStatus().forEach(statusCount -> {
      if (statusCount.getStatus().equals(SUCCESS)) {
        topConsumer.setSuccessfulActivityCount(statusCount.getCount());
      } else {
        topConsumer.setFailedActivityCount(statusCount.getCount());
      }
      topConsumer.setTotalCount(topConsumer.getTotalCount() + statusCount.getCount());
    });
    return topConsumer;
  }
}
