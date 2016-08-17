package software.wings.beans.stats;

import java.util.Map;

/**
 * Created by anubhaw on 8/15/16.
 */
public class DeploymentActivityStatistics extends WingsStatistics {
  Map<Long, Long> activitiesCountByDay;

  public DeploymentActivityStatistics(Map<Long, Long> activitiesCountByDay) {
    super(StatisticsType.DEPLOYMENT_ACTIVITIES);
    this.activitiesCountByDay = activitiesCountByDay;
  }

  public Map<Long, Long> getActivitiesCountByDay() {
    return activitiesCountByDay;
  }

  public void setActivitiesCountByDay(Map<Long, Long> activitiesCountByDay) {
    this.activitiesCountByDay = activitiesCountByDay;
  }
}
