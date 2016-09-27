package software.wings.service.intfc;

import software.wings.beans.stats.DeploymentActivityStatistics;
import software.wings.beans.stats.UserStatistics;
import software.wings.beans.stats.WingsStatistics;

import java.util.List;

/**
 * Created by anubhaw on 8/15/16.
 */
public interface StatisticsService {
  /**
   * Gets top consumers.
   *
   * @return the top consumers
   */
  WingsStatistics getTopConsumers();

  /**
   * Gets key stats.
   *
   * @return the key stats
   */
  List<WingsStatistics> getKeyStats();

  /**
   * Gets deployment activities.
   *
   * @param numOfDays the num of days
   * @param endDate   the end date
   * @return the deployment activities
   */
  DeploymentActivityStatistics getDeploymentActivities(Integer numOfDays, Long endDate);

  /**
   * Gets user stats.
   *
   * @return the user stats
   */
  UserStatistics getUserStats();
}
