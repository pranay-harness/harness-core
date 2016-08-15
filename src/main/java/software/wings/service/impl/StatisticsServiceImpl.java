package software.wings.service.impl;

import static software.wings.beans.stats.DeploymentStatistics.Builder.aDeploymentStatistics;

import com.google.common.collect.ImmutableMap;

import software.wings.beans.Environment.EnvironmentType;
import software.wings.beans.stats.ActiveReleaseStatistics;
import software.wings.beans.stats.ApplicationCountStatistics;
import software.wings.beans.stats.DeploymentActivityStatistics;
import software.wings.beans.stats.TopConsumersStatistics;
import software.wings.beans.stats.WingsStatistics;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ReleaseService;
import software.wings.service.intfc.StatisticsService;
import software.wings.service.intfc.WorkflowService;

import java.util.Arrays;
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

  @Override
  public List<WingsStatistics> getSummary() {
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
        new ActiveReleaseStatistics(55), new ApplicationCountStatistics(12));
  }

  @Override
  public DeploymentActivityStatistics getDeploymentActivities() {
    return new DeploymentActivityStatistics(ImmutableMap.of(1470355200L, 70, 1470787200L, 25, 1471219200L, 100));
  }

  @Override
  public WingsStatistics getTopConsumers() {
    return new TopConsumersStatistics(
        ImmutableMap.of("appA", 1234, "appB", 1134, "appC", 1034, "appD", 934, "appE", 834));
  }
}
