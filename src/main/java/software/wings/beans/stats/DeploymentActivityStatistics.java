package software.wings.beans.stats;

import java.util.Map;

/**
 * Created by anubhaw on 8/15/16.
 */
public class DeploymentActivityStatistics extends WingsStatistics {
  /**
   * The Activities count by day.
   */
  Map<Long, Long> activitiesCountByDay;

  /**
   * Instantiates a new Deployment activity statistics.
   *
   * @param activitiesCountByDay the activities count by day
   */
  public DeploymentActivityStatistics(Map<Long, Long> activitiesCountByDay) {
    super(StatisticsType.DEPLOYMENT_ACTIVITIES);
    this.activitiesCountByDay = activitiesCountByDay;
  }

  /**
   * Gets activities count by day.
   *
   * @return the activities count by day
   */
  public Map<Long, Long> getActivitiesCountByDay() {
    return activitiesCountByDay;
  }

  /**
   * Sets activities count by day.
   *
   * @param activitiesCountByDay the activities count by day
   */
  public void setActivitiesCountByDay(Map<Long, Long> activitiesCountByDay) {
    this.activitiesCountByDay = activitiesCountByDay;
  }
}
