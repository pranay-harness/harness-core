package software.wings.beans.stats;

/**
 * Created by anubhaw on 10/13/16.
 */
public class AppKeyStatistics extends WingsStatistics {
  private int deploymentCount;
  private int artifactCount;
  private int instanceCount;

  /**
   * Instantiates a new Wings statistics.
   */
  public AppKeyStatistics() {
    super(StatisticsType.APP_KEY_STATISTICS);
  }

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
   * Gets artifact count.
   *
   * @return the artifact count
   */
  public int getArtifactCount() {
    return artifactCount;
  }

  /**
   * Sets artifact count.
   *
   * @param artifactCount the artifact count
   */
  public void setArtifactCount(int artifactCount) {
    this.artifactCount = artifactCount;
  }

  /**
   * Gets instance count.
   *
   * @return the instance count
   */
  public int getInstanceCount() {
    return instanceCount;
  }

  /**
   * Sets instance count.
   *
   * @param instanceCount the instance count
   */
  public void setInstanceCount(int instanceCount) {
    this.instanceCount = instanceCount;
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private int deploymentCount;
    private int artifactCount;
    private int instanceCount;

    private Builder() {}

    /**
     * An app key statistics builder.
     *
     * @return the builder
     */
    public static Builder anAppKeyStatistics() {
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
     * With artifact count builder.
     *
     * @param artifactCount the artifact count
     * @return the builder
     */
    public Builder withArtifactCount(int artifactCount) {
      this.artifactCount = artifactCount;
      return this;
    }

    /**
     * With instance count builder.
     *
     * @param instanceCount the instance count
     * @return the builder
     */
    public Builder withInstanceCount(int instanceCount) {
      this.instanceCount = instanceCount;
      return this;
    }

    /**
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return anAppKeyStatistics()
          .withDeploymentCount(deploymentCount)
          .withArtifactCount(artifactCount)
          .withInstanceCount(instanceCount);
    }

    /**
     * Build app key statistics.
     *
     * @return the app key statistics
     */
    public AppKeyStatistics build() {
      AppKeyStatistics appKeyStatistics = new AppKeyStatistics();
      appKeyStatistics.setDeploymentCount(deploymentCount);
      appKeyStatistics.setArtifactCount(artifactCount);
      appKeyStatistics.setInstanceCount(instanceCount);
      return appKeyStatistics;
    }
  }
}
