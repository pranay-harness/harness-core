package software.wings.beans.stats;

import static software.wings.beans.stats.WingsStatistics.StatisticsType.KEY_STATISTICS;

import software.wings.beans.Environment.EnvironmentType;

/**
 * Created by anubhaw on 8/24/16.
 */
public class KeyStatistics extends WingsStatistics {
  private int applicationCount;
  private int artifactCount;
  private int instanceCount;
  private EnvironmentType environmentType;

  /**
   * Instantiates a new Wings statistics.
   */
  public KeyStatistics() {
    super(KEY_STATISTICS);
  }

  /**
   * Gets application count.
   *
   * @return the application count
   */
  public int getApplicationCount() {
    return applicationCount;
  }

  /**
   * Sets application count.
   *
   * @param applicationCount the application count
   */
  public void setApplicationCount(int applicationCount) {
    this.applicationCount = applicationCount;
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
   * Gets environment type.
   *
   * @return the environment type
   */
  public EnvironmentType getEnvironmentType() {
    return environmentType;
  }

  /**
   * Sets environment type.
   *
   * @param environmentType the environment type
   */
  public void setEnvironmentType(EnvironmentType environmentType) {
    this.environmentType = environmentType;
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private int applicationCount;
    private int artifactCount;
    private int instanceCount;
    private EnvironmentType environmentType;

    private Builder() {}

    /**
     * A key statistics builder.
     *
     * @return the builder
     */
    public static Builder aKeyStatistics() {
      return new Builder();
    }

    /**
     * With application count builder.
     *
     * @param applicationCount the application count
     * @return the builder
     */
    public Builder withApplicationCount(int applicationCount) {
      this.applicationCount = applicationCount;
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
     * With environment type builder.
     *
     * @param environmentType the environment type
     * @return the builder
     */
    public Builder withEnvironmentType(EnvironmentType environmentType) {
      this.environmentType = environmentType;
      return this;
    }

    /**
     * But builder.
     *
     * @return the builder
     */
    public Builder but() {
      return aKeyStatistics()
          .withApplicationCount(applicationCount)
          .withArtifactCount(artifactCount)
          .withInstanceCount(instanceCount)
          .withEnvironmentType(environmentType);
    }

    /**
     * Build key statistics.
     *
     * @return the key statistics
     */
    public KeyStatistics build() {
      KeyStatistics keyStatistics = new KeyStatistics();
      keyStatistics.setApplicationCount(applicationCount);
      keyStatistics.setArtifactCount(artifactCount);
      keyStatistics.setInstanceCount(instanceCount);
      keyStatistics.setEnvironmentType(environmentType);
      return keyStatistics;
    }
  }
}
