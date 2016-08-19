package software.wings.beans.stats;

import java.util.Map;

/**
 * Created by anubhaw on 8/15/16.
 */
public class DeploymentActivityStatistics extends WingsStatistics {
  /**
   * The Activities count by day.
   */
  Map<Long, Long> successfulActivitiesCountByDay;

  /**
   * The Failed activities count by day.
   */
  Map<Long, Long> failedActivitiesCountByDay;

  /**
   * Instantiates a new Deployment activity statistics.
   */
  public DeploymentActivityStatistics() {
    super(StatisticsType.DEPLOYMENT_ACTIVITIES);
  }

  /**
   * Gets activities count by day.
   *
   * @return the activities count by day
   */
  public Map<Long, Long> getSuccessfulActivitiesCountByDay() {
    return successfulActivitiesCountByDay;
  }

  /**
   * Sets activities count by day.
   *
   * @param successfulActivitiesCountByDay the activities count by day
   */
  public void setSuccessfulActivitiesCountByDay(Map<Long, Long> successfulActivitiesCountByDay) {
    this.successfulActivitiesCountByDay = successfulActivitiesCountByDay;
  }

  /**
   * Gets failed activities count by day.
   *
   * @return the failed activities count by day
   */
  public Map<Long, Long> getFailedActivitiesCountByDay() {
    return failedActivitiesCountByDay;
  }

  /**
   * Sets failed activities count by day.
   *
   * @param failedActivitiesCountByDay the failed activities count by day
   */
  public void setFailedActivitiesCountByDay(Map<Long, Long> failedActivitiesCountByDay) {
    this.failedActivitiesCountByDay = failedActivitiesCountByDay;
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    /**
     * The Successful activities count by day.
     */
    Map<Long, Long> successfulActivitiesCountByDay;
    /**
     * The Failed activities count by day.
     */
    Map<Long, Long> failedActivitiesCountByDay;

    private Builder() {}

    /**
     * A deployment activity statistics builder.
     *
     * @return the builder
     */
    public static Builder aDeploymentActivityStatistics() {
      return new Builder();
    }

    /**
     * With successful activities count by day builder.
     *
     * @param successfulActivitiesCountByDay the successful activities count by day
     * @return the builder
     */
    public Builder withSuccessfulActivitiesCountByDay(Map<Long, Long> successfulActivitiesCountByDay) {
      this.successfulActivitiesCountByDay = successfulActivitiesCountByDay;
      return this;
    }

    /**
     * With failed activities count by day builder.
     *
     * @param failedActivitiesCountByDay the failed activities count by day
     * @return the builder
     */
    public Builder withFailedActivitiesCountByDay(Map<Long, Long> failedActivitiesCountByDay) {
      this.failedActivitiesCountByDay = failedActivitiesCountByDay;
      return this;
    }

    /**
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return aDeploymentActivityStatistics()
          .withSuccessfulActivitiesCountByDay(successfulActivitiesCountByDay)
          .withFailedActivitiesCountByDay(failedActivitiesCountByDay);
    }

    /**
     * Build deployment activity statistics.
     *
     * @return the deployment activity statistics
     */
    public DeploymentActivityStatistics build() {
      DeploymentActivityStatistics deploymentActivityStatistics = new DeploymentActivityStatistics();
      deploymentActivityStatistics.setSuccessfulActivitiesCountByDay(successfulActivitiesCountByDay);
      deploymentActivityStatistics.setFailedActivitiesCountByDay(failedActivitiesCountByDay);
      return deploymentActivityStatistics;
    }
  }
}
