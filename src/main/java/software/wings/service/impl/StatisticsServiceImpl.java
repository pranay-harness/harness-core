package software.wings.service.impl;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.mongodb.morphia.aggregation.Group.grouping;
import static software.wings.beans.Environment.EnvironmentType.ALL;
import static software.wings.beans.Environment.EnvironmentType.NON_PROD;
import static software.wings.beans.Environment.EnvironmentType.PROD;
import static software.wings.beans.SearchFilter.Builder.aSearchFilter;
import static software.wings.beans.SortOrder.Builder.aSortOrder;
import static software.wings.beans.WorkflowType.ORCHESTRATION;
import static software.wings.beans.WorkflowType.SIMPLE;
import static software.wings.beans.stats.AppKeyStatistics.AppKeyStatsBreakdown.Builder.anAppKeyStatistics;
import static software.wings.beans.stats.KeyStatistics.Builder.aKeyStatistics;
import static software.wings.beans.stats.NotificationCount.Builder.aNotificationCount;
import static software.wings.beans.stats.TopConsumer.Builder.aTopConsumer;
import static software.wings.beans.stats.UserStatistics.Builder.anUserStatistics;
import static software.wings.dl.PageRequest.Builder.aPageRequest;
import static software.wings.sm.ExecutionStatus.FAILED;
import static software.wings.sm.ExecutionStatus.RUNNING;
import static software.wings.sm.ExecutionStatus.SUCCESS;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import org.mongodb.morphia.aggregation.Accumulator;
import org.mongodb.morphia.aggregation.Group;
import org.mongodb.morphia.aggregation.Projection;
import org.mongodb.morphia.mapping.Mapper;
import org.mongodb.morphia.query.Query;
import software.wings.beans.Activity;
import software.wings.beans.Application;
import software.wings.beans.Environment.EnvironmentType;
import software.wings.beans.Release;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.SortOrder.OrderType;
import software.wings.beans.User;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowType;
import software.wings.beans.stats.ActivityStatusAggregation;
import software.wings.beans.stats.AppKeyStatistics;
import software.wings.beans.stats.AppKeyStatistics.AppKeyStatsBreakdown;
import software.wings.beans.stats.DayActivityStatistics;
import software.wings.beans.stats.DeploymentActivityStatistics;
import software.wings.beans.stats.DeploymentStatistics;
import software.wings.beans.stats.DeploymentStatistics.AggregatedDayStats;
import software.wings.beans.stats.DeploymentStatistics.AggregatedDayStats.DayStat;
import software.wings.beans.stats.KeyStatistics;
import software.wings.beans.stats.NotificationCount;
import software.wings.beans.stats.TopConsumer;
import software.wings.beans.stats.TopConsumersStatistics;
import software.wings.beans.stats.UserStatistics;
import software.wings.beans.stats.UserStatistics.AppDeployment;
import software.wings.beans.stats.UserStatistics.ReleaseDetails;
import software.wings.beans.stats.WingsStatistics;
import software.wings.dl.PageRequest;
import software.wings.dl.WingsPersistence;
import software.wings.security.UserThreadLocal;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.NotificationService;
import software.wings.service.intfc.ReleaseService;
import software.wings.service.intfc.StatisticsService;
import software.wings.service.intfc.UserService;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.sm.ExecutionStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.inject.Inject;

/**
 * Created by anubhaw on 8/15/16.
 */
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
  @Inject private ReleaseService releaseService;
  @Inject private NotificationService notificationService;
  @Inject private ActivityService activityService;

  @Override
  public WingsStatistics getTopConsumers() {
    List<Application> applications = appService.list(new PageRequest<>(), false, 0, 0).getResponse();
    ImmutableMap<String, Application> appIdMap = Maps.uniqueIndex(applications, Application::getUuid);
    List<TopConsumer> topConsumers =
        getTopConsumerForPastXDays(30).stream().filter(tc -> appIdMap.containsKey(tc.getAppId())).collect(toList());
    topConsumers.forEach(topConsumer -> topConsumer.setAppName(appIdMap.get(topConsumer.getAppId()).getName()));
    return new TopConsumersStatistics(topConsumers);
  }

  @Override
  public List<WingsStatistics> getKeyStats() {
    List<Activity> activities = wingsPersistence.createQuery(Activity.class)
                                    .field("createdAt")
                                    .greaterThanOrEq(getEpochMilliOfStartOfDayForXDaysInPastFromNow(30))
                                    .retrievedFields(true, "appId", "environmentType", "artifactId")
                                    .asList();

    Map<EnvironmentType, List<Activity>> activityByEnvType =
        activities.stream()
            .filter(activity -> activity.getEnvironmentType() != null)
            .collect(groupingBy(activity -> PROD.equals(activity.getEnvironmentType()) ? PROD : NON_PROD));
    List<WingsStatistics> keyStats = new ArrayList<>();
    keyStats.add(getKeyStatsForEnvType(PROD, activityByEnvType.get(PROD)));
    keyStats.add(getKeyStatsForEnvType(NON_PROD, activityByEnvType.get(NON_PROD)));
    return keyStats;
  }

  @Override
  public Map<String, AppKeyStatistics> getApplicationKeyStats(List<String> appIds, int numOfDays) {
    List<Activity> activities =
        wingsPersistence.createQuery(Activity.class)
            .field("createdAt")
            .greaterThanOrEq(getEpochMilliOfStartOfDayForXDaysInPastFromNow(numOfDays - 1))
            .field("appId")
            .in(appIds)
            .field("workflowType")
            .in(asList(ORCHESTRATION, SIMPLE))
            .retrievedFields(true, "appId", "environmentType", "serviceInstanceId", "artifactId", "workflowExecutionId")
            .asList();

    Map<String, List<Activity>> activitiesByApp = activities.stream().collect(groupingBy(Activity::getAppId));

    appIds.forEach(appId -> activitiesByApp.computeIfAbsent(appId, id -> asList()));

    Map<String, AppKeyStatistics> appKeyStatisticsMap = new HashMap<>();
    activitiesByApp.forEach((appId, activityList) -> {
      AppKeyStatistics keyStatistics = getAppKeyStatistics(activityList);
      appKeyStatisticsMap.put(appId, keyStatistics);
    });
    appIds.forEach(appId -> appKeyStatisticsMap.computeIfAbsent(appId, v -> new AppKeyStatistics()));
    return appKeyStatisticsMap;
  }

  public AppKeyStatistics getAppKeyStatistics(List<Activity> applicationActivities) {
    AppKeyStatistics appKeyStatistics = new AppKeyStatistics();

    Map<EnvironmentType, List<Activity>> activitiesByEnvType = applicationActivities.stream().collect(
        groupingBy(activity -> PROD.equals(activity.getEnvironmentType()) ? PROD : NON_PROD));
    activitiesByEnvType.computeIfAbsent(PROD, v -> asList());
    activitiesByEnvType.computeIfAbsent(NON_PROD, v -> asList());

    activitiesByEnvType.forEach((environmentType, activities) -> {
      int deployments = activities.stream().map(Activity::getWorkflowExecutionId).collect(toSet()).size();
      int instances = activities.size();
      int artifacts = activities.stream().map(Activity::getArtifactId).collect(toSet()).size();
      appKeyStatistics.getStatsMap().put(environmentType,
          anAppKeyStatistics()
              .withInstanceCount(instances)
              .withDeploymentCount(deployments)
              .withArtifactCount(artifacts)
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
  public DeploymentActivityStatistics getDeploymentActivities(Integer numOfDays, Long endDate) {
    if (numOfDays == null || numOfDays <= 0) {
      numOfDays = 30;
    }
    if (endDate == null || endDate <= 0) {
      endDate = LocalDate.now(ZoneId.systemDefault()).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    long fromDateEpochMilli = endDate - MILLIS_IN_A_DAY * (numOfDays - 1);
    return getDeploymentActivitiesForXDaysStartingFrom(numOfDays, fromDateEpochMilli);
  }

  @Override
  public UserStatistics getUserStats() {
    User user = UserThreadLocal.get();
    if (user == null) {
      return new UserStatistics();
    }
    long statsFetchedOn = user.getStatsFetchedOn();

    PageRequest pageRequest =
        aPageRequest()
            .withLimit(PageRequest.UNLIMITED)
            .addFilter(aSearchFilter().withField("createdAt", Operator.GT, statsFetchedOn).build())
            .addFilter(
                aSearchFilter().withField("workflowType", Operator.IN, ORCHESTRATION, WorkflowType.SIMPLE).build())
            .build();
    List<WorkflowExecution> workflowExecutions =
        workflowExecutionService.listExecutions(pageRequest, false, false, false).getResponse();

    Map<String, List<WorkflowExecution>> wflExecutionsByApp = new HashMap<>();
    workflowExecutions.forEach(wflExecution
        -> wflExecutionsByApp.computeIfAbsent(wflExecution.getAppId(), s -> new ArrayList<>()).add(wflExecution));

    List<AppDeployment> appDeployments = new ArrayList<>();
    List<ReleaseDetails> releaseDetailsList = new ArrayList<>();

    wflExecutionsByApp.forEach((appId, wflExecutions) -> {
      if (wflExecutions.size() != 0) {
        String appName = wflExecutions.get(0).getAppName();

        appDeployments.add(new AppDeployment(appId, appName, wflExecutions));

        List<Release> releases = getAssociatedReleasesFromExecutions(appId, wflExecutions);
        if (releases.size() > 0) {
          releaseDetailsList.add(new ReleaseDetails(appId, appName, releases));
        }
      }
    });

    int releaseCount =
        (int) releaseDetailsList.stream().map(releaseDetails -> releaseDetails.getReleases().size()).count();
    int deploymentCount =
        (int) appDeployments.stream().map(appDeployment -> appDeployment.getDeployments().size()).count();

    executorService.submit(() -> userService.updateStatsFetchedOnForUser(user));
    return anUserStatistics()
        .withAppDeployments(appDeployments)
        .withReleaseDetails(releaseDetailsList)
        .withReleaseCount(releaseCount)
        .withDeploymentCount(deploymentCount)
        .withLastFetchedOn(statsFetchedOn)
        .build();
  }

  @Override
  public DeploymentStatistics getDeploymentStatistics(String appId, int numOfDays) {
    long fromDateEpochMilli = getEpochMilliOfStartOfDayForXDaysInPastFromNow(numOfDays);

    PageRequest pageRequest =
        aPageRequest()
            .withLimit(PageRequest.UNLIMITED)
            .addFilter(aSearchFilter().withField("createdAt", Operator.GT, fromDateEpochMilli).build())
            .addFilter(
                aSearchFilter().withField("workflowType", Operator.IN, ORCHESTRATION, WorkflowType.SIMPLE).build())
            .addOrder(aSortOrder().withField("createdAt", OrderType.DESC).build())
            .build();
    if (!isNullOrEmpty(appId)) {
      pageRequest.addFilter(aSearchFilter().withField("appId", Operator.EQ, appId).build());
    }

    List<WorkflowExecution> workflowExecutions =
        workflowExecutionService.listExecutions(pageRequest, false, false, false).getResponse();

    Map<EnvironmentType, List<WorkflowExecution>> wflExecutionByEnvType =
        workflowExecutions.parallelStream().collect(groupingBy(wex -> PROD.equals(wex.getEnvType()) ? PROD : NON_PROD));

    DeploymentStatistics deploymentStats = new DeploymentStatistics();
    deploymentStats.getStatsMap().put(
        PROD, getDeploymentStatisticsByEnvType(numOfDays, wflExecutionByEnvType.get(EnvironmentType.PROD)));
    deploymentStats.getStatsMap().put(
        NON_PROD, getDeploymentStatisticsByEnvType(numOfDays, wflExecutionByEnvType.get(EnvironmentType.NON_PROD)));
    deploymentStats.getStatsMap().put(
        ALL, merge(deploymentStats.getStatsMap().get(PROD), deploymentStats.getStatsMap().get(NON_PROD)));
    return deploymentStats;
  }

  @Override
  public NotificationCount getNotificationCount(String appId, int minutesFromNow) {
    long queryStartEpoch = System.currentTimeMillis() - (minutesFromNow * 60 * 1000);

    PageRequest actionableNotificationRequest =
        aPageRequest().addFilter("actionable", Operator.EQ, true).addFilter("complete", Operator.EQ, false).build();

    PageRequest nonActionableNotificationRequest = aPageRequest()
                                                       .addFilter("createdAt", Operator.GT, queryStartEpoch)
                                                       .addFilter("actionable", Operator.EQ, false)
                                                       .build();

    PageRequest failureRequest = aPageRequest()
                                     .addFilter("createdAt", Operator.GT, queryStartEpoch)
                                     .addFilter("status", Operator.EQ, FAILED)
                                     .addFieldsIncluded("appId")
                                     .build();

    if (appId != null) {
      actionableNotificationRequest.addFilter("appId", appId, Operator.EQ);
      nonActionableNotificationRequest.addFilter("appId", appId, Operator.EQ);
      failureRequest.addFilter("appId", appId, Operator.EQ);
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

  /**
   * Gets associated releases from executions.
   *
   * @param appId         the app id
   * @param wflExecutions the wfl executions
   * @return the associated releases from executions
   */
  public List<Release> getAssociatedReleasesFromExecutions(String appId, List<WorkflowExecution> wflExecutions) {
    Set<String> releaseIds =
        wflExecutions.stream()
            .filter(wfle -> wfle.getExecutionArgs() != null && wfle.getExecutionArgs().getReleaseId() != null)
            .map(e -> e.getExecutionArgs().getReleaseId())
            .collect(toSet());
    if (releaseIds.size() == 0) {
      return new ArrayList<>();
    }

    return releaseService
        .list(
            aPageRequest()
                .addFilter(aSearchFilter().withField("appId", Operator.EQ, appId).build())
                .addFilter(aSearchFilter()
                               .withField(Mapper.ID_KEY, Operator.IN, releaseIds.toArray(new String[releaseIds.size()]))
                               .build())
                .build())
        .getResponse();
  }

  private DeploymentActivityStatistics getDeploymentActivitiesForXDaysStartingFrom(
      Integer numOfDays, Long fromDateEpochMilli) {
    PageRequest pageRequest =
        aPageRequest()
            .withLimit(PageRequest.UNLIMITED)
            .addFilter(aSearchFilter().withField("createdAt", Operator.GT, fromDateEpochMilli).build())
            .addFilter(
                aSearchFilter().withField("workflowType", Operator.IN, ORCHESTRATION, WorkflowType.SIMPLE).build())
            .addOrder(aSortOrder().withField("createdAt", OrderType.DESC).build())
            .build();

    List<WorkflowExecution> workflowExecutions =
        workflowExecutionService.listExecutions(pageRequest, false, false, false).getResponse();
    List<DayActivityStatistics> dayActivityStatisticsList = new ArrayList<>(numOfDays);

    Map<Long, Map<ExecutionStatus, Long>> executionStatusCountByDay =
        workflowExecutions.parallelStream().collect(groupingBy(
            wfl -> (getStartOfTheDayEpoch(wfl.getCreatedAt())), groupingBy(WorkflowExecution::getStatus, counting())));

    IntStream.range(0, numOfDays).forEach(idx -> {
      int successCount = 0;
      int failureCount = 0;
      int runningCount = 0;
      Long timeOffset = fromDateEpochMilli + idx * MILLIS_IN_A_DAY;
      Map<ExecutionStatus, Long> executionStatusCountMap = executionStatusCountByDay.get(timeOffset);
      if (executionStatusCountMap != null) {
        successCount = executionStatusCountMap.getOrDefault(SUCCESS, 0L).intValue();
        failureCount = executionStatusCountMap.getOrDefault(FAILED, 0L).intValue();
        runningCount = executionStatusCountMap.getOrDefault(RUNNING, 0L).intValue();
      }
      int total = successCount + failureCount + runningCount;
      dayActivityStatisticsList.add(DayActivityStatistics.Builder.aDayActivityStatistics()
                                        .withDate(timeOffset)
                                        .withSuccessCount(successCount)
                                        .withFailureCount(failureCount)
                                        .withRunningCount(runningCount)
                                        .withTotalCount(total)
                                        .build());
    });
    return new DeploymentActivityStatistics(dayActivityStatisticsList);
  }

  private KeyStatistics getKeyStatsForEnvType(EnvironmentType environmentType, List<Activity> activities) {
    int appCount = 0;
    int artifactCount = 0;
    int instanceCount = 0;

    if (activities != null && activities.size() > 0) {
      appCount = activities.stream().map(Activity::getAppId).collect(Collectors.toSet()).size();
      artifactCount = activities.stream().map(Activity::getArtifactId).collect(Collectors.toSet()).size();
      instanceCount = activities.size();
    }
    return aKeyStatistics()
        .withApplicationCount(appCount)
        .withArtifactCount(artifactCount)
        .withInstanceCount(instanceCount)
        .withEnvironmentType(environmentType)
        .build();
  }

  private List<TopConsumer> getTopConsumerForPastXDays(int days) {
    long epochMilli = getEpochMilliOfStartOfDayForXDaysInPastFromNow(days);

    List<TopConsumer> topConsumers = new ArrayList<>();
    Query<Activity> query = wingsPersistence.createQuery(Activity.class)
                                .field("createdAt")
                                .greaterThanOrEq(epochMilli)
                                .field("status")
                                .hasAnyOf(asList(ExecutionStatus.FAILED, ExecutionStatus.SUCCESS));

    wingsPersistence.getDatastore()
        .createAggregation(Activity.class)
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

  private TopConsumer getTopConsumerFromActivityStatusAggregation(ActivityStatusAggregation activityStatusAggregation) {
    TopConsumer topConsumer = aTopConsumer().withAppId(activityStatusAggregation.getAppId()).build();
    activityStatusAggregation.getStatus().stream().forEach(statusCount -> {
      if (statusCount.getStatus().equals(ExecutionStatus.SUCCESS)) {
        topConsumer.setSuccessfulActivityCount(statusCount.getCount());
      } else {
        topConsumer.setFailedActivityCount(statusCount.getCount());
      }
      topConsumer.setTotalCount(topConsumer.getTotalCount() + statusCount.getCount());
    });
    return topConsumer;
  }

  private long getEpochMilliOfStartOfDayForXDaysInPastFromNow(int days) {
    return LocalDate.now(ZoneId.systemDefault())
        .minus(days, ChronoUnit.DAYS)
        .atStartOfDay(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli();
  }

  private long getStartOfTheDayEpoch(long epoch) {
    return Instant.ofEpochMilli(epoch)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .atStartOfDay(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli();
  }
}
