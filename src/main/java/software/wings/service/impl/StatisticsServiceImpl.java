package software.wings.service.impl;

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
import static software.wings.beans.stats.AppKeyStatistics.Builder.anAppKeyStatistics;
import static software.wings.beans.stats.DeploymentStatistics.DayStats.Builder.aDayStats;
import static software.wings.beans.stats.KeyStatistics.Builder.aKeyStatistics;
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
import software.wings.beans.stats.DayActivityStatistics;
import software.wings.beans.stats.DeploymentActivityStatistics;
import software.wings.beans.stats.DeploymentStatistics;
import software.wings.beans.stats.DeploymentStatistics.DayStats;
import software.wings.beans.stats.KeyStatistics;
import software.wings.beans.stats.TopConsumer;
import software.wings.beans.stats.TopConsumersStatistics;
import software.wings.beans.stats.UserStatistics;
import software.wings.beans.stats.UserStatistics.AppDeployment;
import software.wings.beans.stats.UserStatistics.ReleaseDetails;
import software.wings.beans.stats.WingsStatistics;
import software.wings.dl.PageRequest;
import software.wings.dl.WingsPersistence;
import software.wings.security.UserThreadLocal;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.EnvironmentService;
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
import java.util.Arrays;
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
  @Inject private EnvironmentService environmentService;

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
            .greaterThanOrEq(getEpochMilliOfStartOfDayForXDaysInPastFromNow(numOfDays))
            .field("appId")
            .in(appIds)
            .field("workflowType")
            .in(Arrays.asList(ORCHESTRATION, SIMPLE))
            .retrievedFields(true, "appId", "serviceInstanceId", "artifactId", "workflowExecutionId")
            .asList();

    Map<String, List<Activity>> activitiesByApp = activities.stream().collect(groupingBy(Activity::getAppId));
    Map<String, AppKeyStatistics> appKeyStatisticsMap = new HashMap<>();
    activitiesByApp.forEach((appId, activityList) -> {
      int deployments = activityList.stream().map(Activity::getWorkflowExecutionId).collect(toSet()).size();
      int instances = activityList.size();
      int artifacts = activities.stream().map(Activity::getArtifactId).collect(toSet()).size();
      appKeyStatisticsMap.put(appId,
          anAppKeyStatistics()
              .withInstanceCount(instances)
              .withDeploymentCount(deployments)
              .withArtifactCount(artifacts)
              .build());
    });
    return appKeyStatisticsMap;
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
  public DeploymentStatistics getDeploymentStatistics(int numOfDays) {
    long fromDateEpochMilli = getEpochMilliOfStartOfDayForXDaysInPastFromNow(numOfDays);

    long start = System.currentTimeMillis();
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

  private List<DayStats> merge(List<DayStats> prodStats, List<DayStats> nonProdStats) {
    List<DayStats> dayStats = new ArrayList<>(prodStats.size());
    IntStream.range(0, prodStats.size()).forEach(idx -> {
      DayStats prod = prodStats.get(idx);
      DayStats nonProd = nonProdStats.get(idx);
      dayStats.add(aDayStats()
                       .withDate(prod.getDate())
                       .withTotalCount(prod.getTotalCount() + nonProd.getTotalCount())
                       .withFailedCount(prod.getFailedCount() + nonProd.getFailedCount())
                       .withInstancesCount(prod.getInstancesCount() + nonProd.getInstancesCount())
                       .build());
    });
    return dayStats;
  }

  private List<DayStats> getDeploymentStatisticsByEnvType(int numOfDays, List<WorkflowExecution> workflowExecutions) {
    long fromDateEpochMilli = getEpochMilliOfStartOfDayForXDaysInPastFromNow(numOfDays);

    List<DayStats> dayStats = new ArrayList<>(numOfDays);

    Map<Long, List<WorkflowExecution>> wflExecutionByDate = new HashMap<>();
    if (workflowExecutions != null) {
      wflExecutionByDate =
          workflowExecutions.parallelStream().collect(groupingBy(wfl -> (getStartOfTheDayEpoch(wfl.getCreatedAt()))));
    }

    final Map<Long, List<WorkflowExecution>> finalWflExecutionByDate = wflExecutionByDate;
    IntStream.range(0, numOfDays).forEach(idx -> {
      int totalCount = 0;
      int failureCount = 0;
      int instanceCount = 0;

      Long timeOffset = fromDateEpochMilli + idx * MILLIS_IN_A_DAY;
      List<WorkflowExecution> wflExecutions = finalWflExecutionByDate.get(timeOffset);
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
      dayStats.add(aDayStats()
                       .withDate(timeOffset)
                       .withTotalCount(totalCount)
                       .withFailedCount(failureCount)
                       .withInstancesCount(instanceCount)
                       .build());
    });
    return dayStats;
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
                                .hasAnyOf(Arrays.asList(ExecutionStatus.FAILED, ExecutionStatus.SUCCESS));

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
