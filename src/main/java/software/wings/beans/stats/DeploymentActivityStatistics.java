package software.wings.beans.stats;

import java.util.List;

/**
 * Created by anubhaw on 8/15/16.
 */
public class DeploymentActivityStatistics extends WingsStatistics {
  /**
   * The Activity statistics.
   */
  List<DayActivityStatistics> activityStatistics;

  /**
   * Instantiates a new Deployment activity statistics.
   */
  public DeploymentActivityStatistics(List<DayActivityStatistics> activityStatistics) {
    super(StatisticsType.DEPLOYMENT_ACTIVITIES);
    this.activityStatistics = activityStatistics;
  }

  /**
   * Gets activity statistics.
   *
   * @return the activity statistics
   */
  public List<DayActivityStatistics> getActivityStatistics() {
    return activityStatistics;
  }

  /**
   * Sets activity statistics.
   *
   * @param activityStatistics the activity statistics
   */
  public void setActivityStatistics(List<DayActivityStatistics> activityStatistics) {
    this.activityStatistics = activityStatistics;
  }
}
