package software.wings.service.intfc;

import software.wings.beans.stats.AppKeyStatistics;
import software.wings.beans.stats.DeploymentActivityStatistics;
import software.wings.beans.stats.DeploymentStatistics;
import software.wings.beans.stats.NotificationCount;
import software.wings.beans.stats.UserStatistics;
import software.wings.beans.stats.WingsStatistics;

import java.util.List;
import java.util.Map;

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
   * Gets application key stats.
   *
   * @param appIds    the app id
   * @param numOfDays the num of days
   * @return the application key stats
   */
  Map<String, AppKeyStatistics> getApplicationKeyStats(List<String> appIds, int numOfDays);

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

  /**
   * Gets deployment statistics.
   *
   * @param appId     the app id
   * @param numOfDays the num of days
   * @return the deployment statistics
   */
  DeploymentStatistics getDeploymentStatistics(String appId, int numOfDays);

  /**
   * Gets notification count.
   *
   * @param appId          the app id
   * @param minutesFromNow the minutes from now
   * @return the notification count
   */
  NotificationCount getNotificationCount(String appId, int minutesFromNow);
}
