package software.wings.service.intfc;

import software.wings.beans.stats.AppKeyStatistics;
import software.wings.beans.stats.DeploymentStatistics;
import software.wings.beans.stats.NotificationCount;
import software.wings.beans.stats.UserStatistics;
import software.wings.beans.stats.WingsStatistics;

import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;

/**
 * Created by anubhaw on 8/15/16.
 */
public interface StatisticsService {
  /**
   * Gets top consumers.
   *
   * @return the top consumers
   * @param accountId
   */
  WingsStatistics getTopConsumers(@NotNull String accountId);

  /**
   * Gets application key stats.
   *
   * @param appIds    the list of app ids
   * @param numOfDays the num of days
   * @return the application key stats
   */
  Map<String, AppKeyStatistics> getApplicationKeyStats(List<String> appIds, int numOfDays);

  /**
   * Gets application key stats for a single app.
   *
   * @param appId     the app id
   * @param numOfDays the num of days
   * @return the application key stats
   */
  AppKeyStatistics getSingleApplicationKeyStats(String appId, int numOfDays);

  /**
   * Gets user stats.
   *
   * @return the user stats
   * @param accountId
   */
  UserStatistics getUserStats(@NotNull String accountId);

  /**
   * Gets deployment statistics.
   *
   *
   * @param accountId
   * @param appIds    the app ids
   * @param numOfDays the num of days
   * @return the deployment statistics
   */
  DeploymentStatistics getDeploymentStatistics(@NotNull String accountId, List<String> appIds, int numOfDays);

  /**
   * Gets notification count.
   *
   *
   * @param accountId
   * @param appId          the app id
   * @param minutesFromNow the minutes from now
   * @return the notification count
   */
  NotificationCount getNotificationCount(@NotNull String accountId, String appId, int minutesFromNow);
}
