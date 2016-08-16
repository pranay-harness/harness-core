package software.wings.beans.stats;

import java.util.Map;

/**
 * Created by anubhaw on 8/15/16.
 */
public class DeploymentActivityStatistics extends WingsStatistics {
  Map<Long, Integer> activitiesCountByDay;

  public DeploymentActivityStatistics(Map<Long, Integer> activitiesCountByDay) {
    super(StatisticsType.DEPLOYMENT_ACTIVITIES);
    this.activitiesCountByDay = activitiesCountByDay;
  }

  public Map<Long, Integer> getActivitiesCountByDay() {
    return activitiesCountByDay;
  }

  public void setActivitiesCountByDay(Map<Long, Integer> activitiesCountByDay) {
    this.activitiesCountByDay = activitiesCountByDay;
  }
}
