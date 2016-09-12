package software.wings.service.impl;

import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.summingLong;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.mongodb.morphia.aggregation.Group.grouping;
import static software.wings.beans.Environment.EnvironmentType.NON_PROD;
import static software.wings.beans.Environment.EnvironmentType.PROD;
import static software.wings.beans.SearchFilter.Builder.aSearchFilter;
import static software.wings.beans.SearchFilter.Operator.EXISTS;
import static software.wings.beans.SortOrder.Builder.aSortOrder;
import static software.wings.beans.stats.DeploymentStatistics.Builder.aDeploymentStatistics;
import static software.wings.beans.stats.KeyStatistics.Builder.aKeyStatistics;
import static software.wings.beans.stats.TopConsumer.Builder.aTopConsumer;
import static software.wings.dl.PageRequest.Builder.aPageRequest;
import static software.wings.sm.ExecutionStatus.FAILED;
import static software.wings.sm.ExecutionStatus.RUNNING;
import static software.wings.sm.ExecutionStatus.SUCCESS;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import org.mongodb.morphia.aggregation.Accumulator;
import org.mongodb.morphia.aggregation.Group;
import org.mongodb.morphia.aggregation.Projection;
import org.mongodb.morphia.query.Query;
import software.wings.beans.Activity;
import software.wings.beans.Application;
import software.wings.beans.Environment.EnvironmentType;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.SortOrder.OrderType;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowType;
import software.wings.beans.stats.ActiveArtifactStatistics;
import software.wings.beans.stats.ActiveReleaseStatistics;
import software.wings.beans.stats.ActivityStatusAggregation;
import software.wings.beans.stats.DayActivityStatistics;
import software.wings.beans.stats.DeploymentActivityStatistics;
import software.wings.beans.stats.DeploymentStatistics;
import software.wings.beans.stats.KeyStatistics;
import software.wings.beans.stats.TopConsumer;
import software.wings.beans.stats.TopConsumersStatistics;
import software.wings.beans.stats.WingsStatistics;
import software.wings.dl.PageRequest;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.StatisticsService;
import software.wings.service.intfc.WorkflowService;
import software.wings.sm.ExecutionStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
  @Inject private ActivityService activityService;
  @Inject private WorkflowService workflowService;
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public List<WingsStatistics> getSummary() {
    List<WingsStatistics> summaryStats = new ArrayList<>();

    List<Application> applications =
        appService.list(PageRequest.Builder.aPageRequest().addFieldsIncluded("environments").build(), false, 0)
            .getResponse();

    summaryStats.add(new ActiveReleaseStatistics(getActiveReleases()));
    summaryStats.add(new ActiveArtifactStatistics(getActiveArtifacts()));
    addDeploymentStatistics(summaryStats, applications);

    return summaryStats;
  }

  private void addDeploymentStatistics(List<WingsStatistics> statisticsList, List<Application> applications) {
    List<DeploymentStatistics> currentDeploymentStats = getDeploymentStatisticsForRangeOfDays(applications, 30, 0);
    List<DeploymentStatistics> oldDeploymentStats = getDeploymentStatisticsForRangeOfDays(applications, 60, 30);

    if (oldDeploymentStats != null && oldDeploymentStats.size() == currentDeploymentStats.size()) {
      IntStream.of(0, 1).forEach(idx -> {
        int countChange = oldDeploymentStats.get(idx).getCount() == 0
            ? 0
            : currentDeploymentStats.get(idx).getCount() - oldDeploymentStats.get(idx).getCount();
        int avgTimeChange = oldDeploymentStats.get(idx).getAvgTime() == 0
            ? 0
            : currentDeploymentStats.get(idx).getAvgTime() - oldDeploymentStats.get(idx).getAvgTime();
        currentDeploymentStats.get(idx).setCountChange(countChange);
        currentDeploymentStats.get(idx).setAvgTimeChange(avgTimeChange);
      });
    }
    statisticsList.addAll(currentDeploymentStats);
  }

  private List<DeploymentStatistics> getDeploymentStatisticsForRangeOfDays(
      List<Application> applications, int daysFrom, int daysTo) {
    Map<EnvironmentType, Set<String>> envUuidListByType =
        applications.stream()
            .flatMap(application -> application.getEnvironments().stream())
            .collect(groupingBy(env
                -> env.getEnvironmentType().equals(PROD) ? PROD : NON_PROD,
                mapping(environment -> environment.getUuid(), toSet())));

    PageRequest pageRequest =
        aPageRequest()
            .addFilter(
                aSearchFilter()
                    .withField("createdAt", Operator.GT, getEpochMilliOfStartOfDayForXDaysInPastFromNow(daysFrom))
                    .build())
            .addFilter(aSearchFilter()
                           .withField("workflowType", Operator.IN, WorkflowType.ORCHESTRATION, WorkflowType.SIMPLE)
                           .build())
            .addFilter(aSearchFilter()
                           .withField("status", Operator.EQ, SUCCESS)
                           .withField("startTs", EXISTS)
                           .withField("endTs", EXISTS)
                           .build())
            .build();
    if (daysTo > 0) {
      pageRequest.addFilter(
          aSearchFilter()
              .withField("createdAt", Operator.LT, getEpochMilliOfStartOfDayForXDaysInPastFromNow(daysTo))
              .build());
    }

    List<WorkflowExecution> workflowExecutions = workflowService.listExecutions(pageRequest, false).getResponse();

    Map<EnvironmentType, Long> deploymentCountByType =
        workflowExecutions.stream()
            .filter(wfl
                -> envUuidListByType.get(PROD).contains(wfl.getEnvId())
                    || envUuidListByType.get(NON_PROD).contains(wfl.getEnvId()))
            .collect(
                groupingBy(wfl -> envUuidListByType.get(PROD).contains(wfl.getEnvId()) ? PROD : NON_PROD, counting()));

    deploymentCountByType.computeIfAbsent(PROD, wfl -> deploymentCountByType.put(PROD, 0L));
    deploymentCountByType.computeIfAbsent(NON_PROD, wfl -> deploymentCountByType.put(NON_PROD, 0L));

    Map<EnvironmentType, Long> deploymentTimeDurationSumByType =
        workflowExecutions.stream()
            .filter(wfl
                -> envUuidListByType.get(PROD).contains(wfl.getEnvId())
                    || envUuidListByType.get(NON_PROD).contains(wfl.getEnvId()))
            .collect(groupingBy(wfl
                -> envUuidListByType.get(PROD).contains(wfl.getEnvId()) ? PROD : NON_PROD,
                summingLong(wf -> (wf.getEndTs() - wf.getStartTs()))));

    deploymentTimeDurationSumByType.computeIfAbsent(PROD, wfl -> deploymentCountByType.put(PROD, 0L));
    deploymentTimeDurationSumByType.computeIfAbsent(NON_PROD, wfl -> deploymentCountByType.put(NON_PROD, 0L));

    int prodDeploymentAvgTime = deploymentCountByType.get(PROD) == 0
        ? 0
        : (int) (deploymentTimeDurationSumByType.get(PROD) / (60 * 1000 * deploymentCountByType.get(PROD)));

    int otherDeploymentAvgTime = deploymentCountByType.get(NON_PROD) == 0
        ? 0
        : (int) (deploymentTimeDurationSumByType.get(NON_PROD) / (60 * 1000 * deploymentCountByType.get(NON_PROD)));

    DeploymentStatistics prodDeploymentStatistics = aDeploymentStatistics()
                                                        .withEnvironmentType(PROD)
                                                        .withCount(deploymentCountByType.get(PROD).intValue())
                                                        .withCountChange(0)
                                                        .withAvgTime(prodDeploymentAvgTime)
                                                        .withAvgTimeChange(0)
                                                        .build();
    DeploymentStatistics otherDeploymentStatistics = aDeploymentStatistics()
                                                         .withEnvironmentType(NON_PROD)
                                                         .withCount(deploymentCountByType.get(NON_PROD).intValue())
                                                         .withCountChange(0)
                                                         .withAvgTime(otherDeploymentAvgTime)
                                                         .withAvgTimeChange(0)
                                                         .build();

    return Arrays.asList(prodDeploymentStatistics, otherDeploymentStatistics);
  }

  private int getActiveReleases() {
    PageRequest pageRequest =
        aPageRequest()
            .addFilter(aSearchFilter()
                           .withField("createdAt", Operator.GT, getEpochMilliOfStartOfDayForXDaysInPastFromNow(30))
                           .build())
            .addFilter(aSearchFilter().withField("status", Operator.EQ, ExecutionStatus.SUCCESS).build())
            .addFilter(aSearchFilter().withField("releaseId", EXISTS).build())
            .addFieldsIncluded("releaseId")
            .build();
    List<Activity> activityList = activityService.list(pageRequest).getResponse();
    return activityList.stream().map(Activity::getReleaseId).collect(toSet()).size();
  }

  private int getActiveArtifacts() {
    PageRequest pageRequest =
        aPageRequest()
            .addFilter(aSearchFilter()
                           .withField("createdAt", Operator.GT, getEpochMilliOfStartOfDayForXDaysInPastFromNow(30))
                           .build())
            .addFilter(aSearchFilter().withField("status", Operator.EQ, ExecutionStatus.SUCCESS).build())
            .addFilter(aSearchFilter().withField("artifactId", EXISTS).build())
            .addFieldsIncluded("artifactId")
            .build();
    List<Activity> activityList = activityService.list(pageRequest).getResponse();
    return activityList.stream().map(Activity::getArtifactId).collect(toSet()).size();
  }

  @Override
  public WingsStatistics getTopConsumers() {
    List<Application> applications = appService.list(new PageRequest<>(), false, 0).getResponse();
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

  private DeploymentActivityStatistics getDeploymentActivitiesForXDaysStartingFrom(
      Integer numOfDays, Long fromDateEpochMilli) {
    PageRequest pageRequest =
        aPageRequest()
            .withLimit("10000")
            .withOffset("0")
            .addFilter(aSearchFilter().withField("createdAt", Operator.GT, fromDateEpochMilli).build())
            .addFilter(aSearchFilter()
                           .withField("workflowType", Operator.IN, WorkflowType.ORCHESTRATION, WorkflowType.SIMPLE)
                           .build())
            .addOrder(aSortOrder().withField("createdAt", OrderType.DESC).build())
            .build();

    List<WorkflowExecution> workflowExecutions = workflowService.listExecutions(pageRequest, false).getResponse();
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
