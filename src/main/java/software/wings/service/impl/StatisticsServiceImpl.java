package software.wings.service.impl;

import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.summingLong;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static software.wings.beans.Activity.Status.COMPLETED;
import static software.wings.beans.Environment.EnvironmentType.OTHER;
import static software.wings.beans.Environment.EnvironmentType.PROD;
import static software.wings.beans.SearchFilter.Builder.aSearchFilter;
import static software.wings.beans.SortOrder.Builder.aSortOrder;
import static software.wings.beans.stats.DeploymentActivityStatistics.Builder.aDeploymentActivityStatistics;
import static software.wings.beans.stats.DeploymentStatistics.Builder.aDeploymentStatistics;
import static software.wings.dl.PageRequest.Builder.aPageRequest;
import static software.wings.sm.ExecutionStatus.SUCCESS;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import org.apache.commons.collections.IteratorUtils;
import org.mongodb.morphia.aggregation.Accumulator;
import org.mongodb.morphia.aggregation.Group;
import software.wings.beans.Activity;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.Environment.EnvironmentType;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.SortOrder.OrderType;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowType;
import software.wings.beans.stats.ActiveArtifactStatistics;
import software.wings.beans.stats.ActiveReleaseStatistics;
import software.wings.beans.stats.DeploymentActivityStatistics;
import software.wings.beans.stats.DeploymentStatistics;
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

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
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
            .collect(
                groupingBy(Environment::getEnvironmentType, mapping(environment -> environment.getUuid(), toSet())));

    PageRequest pageRequest =
        aPageRequest()
            .addFilter(
                aSearchFilter()
                    .withField("createdAt", Operator.GT, getEpochMilliOfStartOfDayForXDaysInPastFromNow(daysFrom))
                    .build())
            .addFilter(aSearchFilter()
                           .withField("workflowType", Operator.IN, WorkflowType.ORCHESTRATION, WorkflowType.SIMPLE)
                           .build())
            .addFilter(aSearchFilter().withField("status", Operator.EQ, SUCCESS).build())
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
                    || envUuidListByType.get(OTHER).contains(wfl.getEnvId()))
            .collect(
                groupingBy(wfl -> envUuidListByType.get(PROD).contains(wfl.getEnvId()) ? PROD : OTHER, counting()));

    deploymentCountByType.computeIfAbsent(PROD, wfl -> deploymentCountByType.put(PROD, 0L));
    deploymentCountByType.computeIfAbsent(OTHER, wfl -> deploymentCountByType.put(OTHER, 0L));

    Map<EnvironmentType, Long> deploymentTimeDurationSumByType =
        workflowExecutions.stream()
            .filter(wfl
                -> envUuidListByType.get(PROD).contains(wfl.getEnvId())
                    || envUuidListByType.get(OTHER).contains(wfl.getEnvId()))
            .collect(groupingBy(wfl
                -> envUuidListByType.get(PROD).contains(wfl.getEnvId()) ? PROD : OTHER,
                summingLong(wf -> (wf.getLastUpdatedAt() - wf.getCreatedAt()))));

    deploymentTimeDurationSumByType.computeIfAbsent(PROD, wfl -> deploymentCountByType.put(PROD, 0L));
    deploymentTimeDurationSumByType.computeIfAbsent(OTHER, wfl -> deploymentCountByType.put(OTHER, 0L));

    int prodDeploymentAvgTime = deploymentCountByType.get(PROD) == 0
        ? 0
        : (int) (deploymentTimeDurationSumByType.get(PROD) / (60 * 1000 * deploymentCountByType.get(PROD)));

    int otherDeploymentAvgTime = deploymentCountByType.get(OTHER) == 0
        ? 0
        : (int) (deploymentTimeDurationSumByType.get(OTHER) / (60 * 1000 * deploymentCountByType.get(OTHER)));

    DeploymentStatistics prodDeploymentStatistics = aDeploymentStatistics()
                                                        .withEnvironmentType(PROD)
                                                        .withCount(deploymentCountByType.get(PROD).intValue())
                                                        .withCountChange(0)
                                                        .withAvgTime(prodDeploymentAvgTime)
                                                        .withAvgTimeChange(0)
                                                        .build();
    DeploymentStatistics otherDeploymentStatistics = aDeploymentStatistics()
                                                         .withEnvironmentType(OTHER)
                                                         .withCount(deploymentCountByType.get(OTHER).intValue())
                                                         .withCountChange(0)
                                                         .withAvgTime(otherDeploymentAvgTime)
                                                         .withAvgTimeChange(0)
                                                         .build();

    return Arrays.asList(prodDeploymentStatistics, otherDeploymentStatistics);
  }

  @Override
  public DeploymentActivityStatistics getDeploymentActivities() {
    PageRequest pageRequest =
        aPageRequest()
            .addFilter(aSearchFilter()
                           .withField("createdAt", Operator.GT, getEpochMilliOfStartOfDayForXDaysInPastFromNow(30))
                           .build())
            .addFilter(aSearchFilter()
                           .withField("workflowType", Operator.IN, WorkflowType.ORCHESTRATION, WorkflowType.SIMPLE)
                           .build())
            .addOrder(aSortOrder().withField("createdAt", OrderType.DESC).build())
            .build();

    List<WorkflowExecution> workflowExecutions = workflowService.listExecutions(pageRequest, false).getResponse();

    Map<ExecutionStatus, List<WorkflowExecution>> executionStatusListMap =
        workflowExecutions.stream().collect(groupingBy(WorkflowExecution::getStatus, Collectors.toList()));

    Map<Long, Long> aggregatedSuccessMap =
        aggregateExecutionCountByDay(executionStatusListMap.get(ExecutionStatus.SUCCESS));
    Map<Long, Long> aggregatedFailedMap =
        aggregateExecutionCountByDay(executionStatusListMap.get(ExecutionStatus.FAILED));
    return aDeploymentActivityStatistics()
        .withSuccessfulActivitiesCountByDay(aggregatedSuccessMap)
        .withFailedActivitiesCountByDay(aggregatedFailedMap)
        .build();
  }

  private Map<Long, Long> aggregateExecutionCountByDay(List<WorkflowExecution> workflowExecutions) {
    if (workflowExecutions == null || workflowExecutions.size() == 0) {
      return emptyMap();
    }
    return workflowExecutions.stream().collect(
        groupingBy(wfl -> (wfl.getCreatedAt() - wfl.getCreatedAt() % 86400000), counting()));
  }

  private int getActiveReleases() {
    PageRequest pageRequest =
        aPageRequest()
            .addFilter(aSearchFilter()
                           .withField("createdAt", Operator.GT, getEpochMilliOfStartOfDayForXDaysInPastFromNow(30))
                           .build())
            .addFilter(aSearchFilter().withField("status", Operator.EQ, COMPLETED).build())
            .addFilter(aSearchFilter().withField("releaseId", Operator.EXISTS).build())
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
            .addFilter(aSearchFilter().withField("status", Operator.EQ, COMPLETED).build())
            .addFilter(aSearchFilter().withField("artifactId", Operator.EXISTS).build())
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

  private List<TopConsumer> getTopConsumerForPastXDays(int days) {
    long epochMilli = getEpochMilliOfStartOfDayForXDaysInPastFromNow(days);
    Iterator<TopConsumer> topConsumerIterator =
        wingsPersistence.getDatastore()
            .createAggregation(Activity.class)
            .match(wingsPersistence.createQuery(Activity.class).field("createdAt").greaterThanOrEq(epochMilli))
            .group("appId", Group.grouping("activityCount", new Accumulator("$sum", 1)))
            .aggregate(TopConsumer.class);

    return IteratorUtils.toList(topConsumerIterator);
  }

  private long getEpochMilliOfStartOfDayForXDaysInPastFromNow(int days) {
    return LocalDate.now(ZoneId.systemDefault())
        .minus(days, ChronoUnit.DAYS)
        .atStartOfDay(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli();
  }
}
