package software.wings.service.intfc;

import software.wings.beans.stats.DeploymentActivityStatistics;
import software.wings.beans.stats.WingsStatistics;

import java.util.List;

/**
 * Created by anubhaw on 8/15/16.
 */
public interface StatisticsService {
  List<WingsStatistics> getSummary();

  DeploymentActivityStatistics getDeploymentActivities();

  WingsStatistics getTopConsumers();
}
