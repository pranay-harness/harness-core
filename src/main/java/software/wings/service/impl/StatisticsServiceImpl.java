package software.wings.service.impl;

import static software.wings.beans.Environment.EnvironmentType.OTHER;
import static software.wings.beans.Environment.EnvironmentType.PROD;
import static software.wings.beans.SearchFilter.Builder.aSearchFilter;
import static software.wings.beans.SortOrder.Builder.aSortOrder;
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
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.SortOrder.OrderType;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowType;
import software.wings.beans.stats.ActiveReleaseStatistics;
import software.wings.beans.stats.ApplicationCountStatistics;
import software.wings.beans.stats.DeploymentActivityStatistics;
import software.wings.beans.stats.DeploymentStatistics;
import software.wings.beans.stats.TopConsumer;
import software.wings.beans.stats.TopConsumersStatistics;
import software.wings.beans.stats.WingsStatistics;
import software.wings.dl.PageRequest;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ReleaseService;
import software.wings.service.intfc.StatisticsService;
import software.wings.service.intfc.WorkflowService;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;

/**
 * Created by anubhaw on 8/15/16.
 */
public class StatisticsServiceImpl implements StatisticsService {
  @Inject private AppService appService;
  @Inject private ActivityService activityService;
  @Inject private WorkflowService workflowService;
  @Inject private ReleaseService releaseService;
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public List<WingsStatistics> getSummary() {
    List<Application> applications = appService.list(new PageRequest<>(), false, 0).getResponse();

    Set<String> prodEnvironments = applications.stream()
                                       .flatMap(application -> application.getEnvironments().stream())
                                       .filter(environment -> environment.getEnvironmentType().equals(PROD))
                                       .map(Environment::getUuid)
                                       .collect(Collectors.toSet());

    Set<String> otherEnvironments = applications.stream()
                                        .flatMap(application -> application.getEnvironments().stream())
                                        .filter(environment -> !environment.getEnvironmentType().equals(PROD))
                                        .map(Environment::getUuid)
                                        .collect(Collectors.toSet());

    PageRequest pageRequest =
        aPageRequest()
            .addFilter(aSearchFilter()
                           .withField("createdAt", Operator.GT, getEpochMilliOfStartOfDayForXDaysInPastFromNow(30))
                           .build())
            .addFilter(aSearchFilter()
                           .withField("workflowType", Operator.IN, WorkflowType.ORCHESTRATION, WorkflowType.SIMPLE)
                           .build())
            .addFilter(aSearchFilter().withField("status", Operator.EQ, SUCCESS).build())
            .build();

    List<WorkflowExecution> workflowExecutionsForFirst30Days =
        workflowService.listExecutions(pageRequest, false).getResponse();

    int prodDeploymentInLast30Days =
        (int) workflowExecutionsForFirst30Days.stream()
            .filter(workflowExecution -> prodEnvironments.contains(workflowExecution.getEnvId()))
            .count();
    int otherDeploymentInLast30Days =
        (int) workflowExecutionsForFirst30Days.stream()
            .filter(workflowExecution -> otherEnvironments.contains(workflowExecution.getEnvId()))
            .count();

    int prodDeploymentAvgTime = prodDeploymentInLast30Days == 0
        ? 0
        : (int) (workflowExecutionsForFirst30Days.stream()
                     .filter(workflowExecution -> prodEnvironments.contains(workflowExecution.getEnvId()))
                     .mapToLong(wf -> (wf.getLastUpdatedAt() - wf.getCreatedAt()))
                     .sum()
              / (60 * 1000 * prodDeploymentInLast30Days));
    int otherDeploymentAvgTime = otherDeploymentInLast30Days == 0
        ? 0
        : (int) (workflowExecutionsForFirst30Days.stream()
                     .filter(workflowExecution -> otherEnvironments.contains(workflowExecution.getEnvId()))
                     .mapToLong(wf -> (wf.getLastUpdatedAt() - wf.getCreatedAt()))
                     .sum()
              / (60 * 1000 * otherDeploymentInLast30Days));

    DeploymentStatistics prodDeploymentStatistics = aDeploymentStatistics()
                                                        .withEnvironmentType(PROD)
                                                        .withCount(prodDeploymentInLast30Days)
                                                        .withCountChange(0)
                                                        .withAvgTime(prodDeploymentAvgTime)
                                                        .withAvgTimeChange(0)
                                                        .build();
    DeploymentStatistics otherDeploymentStatistics = aDeploymentStatistics()
                                                         .withEnvironmentType(OTHER)
                                                         .withCount(otherDeploymentInLast30Days)
                                                         .withCountChange(0)
                                                         .withAvgTime(otherDeploymentAvgTime)
                                                         .withAvgTimeChange(0)
                                                         .build();

    int numberOfApplications = appService.list(new PageRequest<>(), false, 0).getResponse().size();
    int numberOfActiveReleases =
        5; //(int) releaseService.list(new PageRequest<>()).getResponse().stream().filter(Release::isActive).count();

    return Arrays.asList(prodDeploymentStatistics, otherDeploymentStatistics,
        new ActiveReleaseStatistics(numberOfActiveReleases), new ApplicationCountStatistics(numberOfApplications));
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

    Map<Long, Long> longLongMap = workflowExecutions.stream().collect(
        Collectors.groupingBy(wfl -> (wfl.getCreatedAt() - wfl.getCreatedAt() % 86400000), Collectors.counting()));

    return new DeploymentActivityStatistics(longLongMap);
  }

  /**
   * Gets active releases.
   *
   * @return the active releases
   */
  public WingsStatistics getActiveReleases() {
    long epochMilli = getEpochMilliOfStartOfDayForXDaysInPastFromNow(30);
    List<Activity> activities = wingsPersistence.createQuery(Activity.class)
                                    .field("createdAt")
                                    .greaterThanOrEq(epochMilli)
                                    .field("releaseId")
                                    .exists()
                                    .retrievedFields(true, "releaseId")
                                    .asList();
    return new ActiveReleaseStatistics(
        activities.stream().map(Activity::getReleaseId).collect(Collectors.toSet()).size());
  }

  @Override
  public WingsStatistics getTopConsumers() {
    List<Application> applications = appService.list(new PageRequest<>(), false, 0).getResponse();
    ImmutableMap<String, Application> appIdMap = Maps.uniqueIndex(applications, Application::getUuid);
    List<TopConsumer> topConsumers = getTopConsumerForPastXDays(30)
                                         .stream()
                                         .filter(tc -> appIdMap.containsKey(tc.getAppId()))
                                         .collect(Collectors.toList());
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
