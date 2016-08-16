package software.wings.service.impl;

import static software.wings.beans.stats.DeploymentStatistics.Builder.aDeploymentStatistics;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import org.apache.commons.collections.IteratorUtils;
import org.mongodb.morphia.aggregation.Accumulator;
import org.mongodb.morphia.aggregation.Group;
import software.wings.beans.Activity;
import software.wings.beans.Application;
import software.wings.beans.Environment.EnvironmentType;
import software.wings.beans.Release;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.stats.ActiveReleaseStatistics;
import software.wings.beans.stats.ApplicationCountStatistics;
import software.wings.beans.stats.DeploymentActivityStatistics;
import software.wings.beans.stats.TopConsumer;
import software.wings.beans.stats.TopConsumersStatistics;
import software.wings.beans.stats.WingsStatistics;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
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
    ImmutableMap<String, Application> appIdMap = Maps.uniqueIndex(applications, Application::getUuid);

    PageResponse<WorkflowExecution> workflowExecutions = workflowService.listExecutions(new PageRequest<>(), false);

    wingsPersistence.getDatastore().createAggregation(Application.class);

    int numberOfApplications = appService.list(new PageRequest<>(), false, 0).getResponse().size();
    int numberOfActiveReleases =
        (int) releaseService.list(new PageRequest<>()).getResponse().stream().filter(Release::isActive).count();

    return Arrays.asList(aDeploymentStatistics()
                             .withEnvironmentType(EnvironmentType.PROD)
                             .withCount(30)
                             .withCountChange(2)
                             .withAvgTime(24)
                             .withAvgTimeChange(-1)
                             .build(),
        aDeploymentStatistics()
            .withEnvironmentType(EnvironmentType.OTHER)
            .withCount(65)
            .withCountChange(10)
            .withAvgTime(15)
            .withAvgTimeChange(2)
            .build(),
        new ActiveReleaseStatistics(numberOfActiveReleases), new ApplicationCountStatistics(numberOfApplications));
  }

  @Override
  public DeploymentActivityStatistics getDeploymentActivities() {
    return new DeploymentActivityStatistics(ImmutableMap.of(1470355200L, 70, 1470787200L, 25, 1471219200L, 100));
  }

  @Override
  public WingsStatistics getTopConsumers() {
    List<Application> applications = appService.list(new PageRequest<>(), false, 0).getResponse();
    ImmutableMap<String, Application> appIdMap = Maps.uniqueIndex(applications, Application::getUuid);
    List<TopConsumer> topConsumers = getTopConsumerForPastXDays(30);
    topConsumers.forEach(topConsumer -> topConsumer.setAppName(appIdMap.get(topConsumer.getAppId()).getName()));
    return new TopConsumersStatistics(topConsumers);
  }

  private List<TopConsumer> getTopConsumerForPastXDays(int days) {
    long epochMilli = LocalDate.now(ZoneId.systemDefault())
                          .minus(days, ChronoUnit.DAYS)
                          .atStartOfDay(ZoneId.systemDefault())
                          .toInstant()
                          .toEpochMilli();
    Iterator<TopConsumer> topConsumerIterator =
        wingsPersistence.getDatastore()
            .createAggregation(Activity.class)
            .match(wingsPersistence.createQuery(Activity.class).field("createdAt").greaterThanOrEq(epochMilli))
            .group("appId", Group.grouping("activityCount", new Accumulator("$sum", 1)))
            .aggregate(TopConsumer.class);

    return IteratorUtils.toList(topConsumerIterator);
  }
}
