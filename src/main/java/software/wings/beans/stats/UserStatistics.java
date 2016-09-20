package software.wings.beans.stats;

/**
 * Created by anubhaw on 9/19/16.
 */
public class UserStatistics {
  private int deploymentCount;
  private int releaseCount;

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
   * Gets release count.
   *
   * @return the release count
   */
  public int getReleaseCount() {
    return releaseCount;
  }

  /**
   * Sets release count.
   *
   * @param releaseCount the release count
   */
  public void setReleaseCount(int releaseCount) {
    this.releaseCount = releaseCount;
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private int deploymentCount;
    private int releaseCount;

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
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return anUserStatistics().withDeploymentCount(deploymentCount).withReleaseCount(releaseCount);
    }

    /**
     * Build user statistics.
     *
     * @return the user statistics
     */
    public UserStatistics build() {
      UserStatistics userStatistics = new UserStatistics();
      userStatistics.setDeploymentCount(deploymentCount);
      userStatistics.setReleaseCount(releaseCount);
      return userStatistics;
    }
  }
}
