package software.wings.service.intfc;

import software.wings.beans.stats.DeploymentActivityStatistics;
import software.wings.beans.stats.WingsStatistics;

import java.util.List;

/**
 * Created by anubhaw on 8/15/16.
 */
public interface StatisticsService {
  /**
   * Gets summary.
   *
   * @return the summary
   */
  List<WingsStatistics> getSummary();

  /**
   * Gets deployment activities.
   *
   * @return the deployment activities
   */
  DeploymentActivityStatistics getDeploymentActivities();

  /**
   * Gets top consumers.
   *
   * @return the top consumers
   */
  WingsStatistics getTopConsumers();

  List<WingsStatistics> getKeyStats();
}
