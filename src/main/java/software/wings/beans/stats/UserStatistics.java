package software.wings.beans.stats;

import software.wings.beans.WorkflowExecution;

import java.util.List;

/**
 * Created by anubhaw on 9/19/16.
 */
public class UserStatistics {
  private int deploymentCount;
  private long lastFetchedOn;
  private List<AppDeployment> appDeployments;

  /**
   * Gets deployment count.
   *
   * @return the deployment count
   */
  public int getDeploymentCount() {
    return deploymentCount;
  }

  /**
   * Sets deployment count.
   *
   * @param deploymentCount the deployment count
   */
  public void setDeploymentCount(int deploymentCount) {
    this.deploymentCount = deploymentCount;
  }

  /**
   * Gets last fetched on.
   *
   * @return the last fetched on
   */
  public long getLastFetchedOn() {
    return lastFetchedOn;
  }

  /**
   * Sets last fetched on.
   *
   * @param lastFetchedOn the last fetched on
   */
  public void setLastFetchedOn(long lastFetchedOn) {
    this.lastFetchedOn = lastFetchedOn;
  }

  /**
   * Gets app deployments.
   *
   * @return the app deployments
   */
  public List<AppDeployment> getAppDeployments() {
    return appDeployments;
  }

  /**
   * Sets app deployments.
   *
   * @param appDeployments the app deployments
   */
  public void setAppDeployments(List<AppDeployment> appDeployments) {
    this.appDeployments = appDeployments;
  }

  /**
   * The type App deployment.
   */
  public static class AppDeployment {
    /**
     * The Deployments.
     */
    List<WorkflowExecution> deployments;
    private String appId;
    private String appName;

    /**
     * Instantiates a new App deployment.
     *
     * @param appId       the app id
     * @param appName     the app name
     * @param deployments the deployments
     */
    public AppDeployment(String appId, String appName, List<WorkflowExecution> deployments) {
      this.appId = appId;
      this.appName = appName;
      this.deployments = deployments;
    }

    /**
     * Gets app id.
     *
     * @return the app id
     */
    public String getAppId() {
      return appId;
    }

    /**
     * Sets app id.
     *
     * @param appId the app id
     */
    public void setAppId(String appId) {
      this.appId = appId;
    }

    /**
     * Gets app name.
     *
     * @return the app name
     */
    public String getAppName() {
      return appName;
    }

    /**
     * Sets app name.
     *
     * @param appName the app name
     */
    public void setAppName(String appName) {
      this.appName = appName;
    }

    /**
     * Gets deployments.
     *
     * @return the deployments
     */
    public List<WorkflowExecution> getDeployments() {
      return deployments;
    }

    /**
     * Sets deployments.
     *
     * @param deployments the deployments
     */
    public void setDeployments(List<WorkflowExecution> deployments) {
      this.deployments = deployments;
    }
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private int deploymentCount;
    private int releaseCount;
    private long lastFetchedOn;
    private List<AppDeployment> appDeployments;

    private Builder() {}

    /**
     * An user statistics builder.
     *
     * @return the builder
     */
    public static Builder anUserStatistics() {
      return new Builder();
    }

    /**
     * With deployment count builder.
     *
     * @param deploymentCount the deployment count
     * @return the builder
     */
    public Builder withDeploymentCount(int deploymentCount) {
      this.deploymentCount = deploymentCount;
      return this;
    }

    /**
     * With release count builder.
     *
     * @param releaseCount the release count
     * @return the builder
     */
    public Builder withReleaseCount(int releaseCount) {
      this.releaseCount = releaseCount;
      return this;
    }

    /**
     * With last fetched on builder.
     *
     * @param lastFetchedOn the last fetched on
     * @return the builder
     */
    public Builder withLastFetchedOn(long lastFetchedOn) {
      this.lastFetchedOn = lastFetchedOn;
      return this;
    }

    /**
     * With app deployments builder.
     *
     * @param appDeployments the app deployments
     * @return the builder
     */
    public Builder withAppDeployments(List<AppDeployment> appDeployments) {
      this.appDeployments = appDeployments;
      return this;
    }

    /**
     * Build user statistics.
     *
     * @return the user statistics
     */
    public UserStatistics build() {
      UserStatistics userStatistics = new UserStatistics();
      userStatistics.setDeploymentCount(deploymentCount);
      userStatistics.setLastFetchedOn(lastFetchedOn);
      userStatistics.setAppDeployments(appDeployments);
      return userStatistics;
    }
  }
}
