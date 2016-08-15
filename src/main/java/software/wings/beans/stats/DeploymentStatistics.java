package software.wings.beans.stats;

import com.google.common.base.MoreObjects;

import software.wings.beans.Environment.EnvironmentType;

import java.util.Objects;

/**
 * Created by anubhaw on 8/15/16.
 */
public class DeploymentStatistics extends WingsStatistics {
  private Integer count;
  private Integer countChange;
  private Integer avgTime;
  private Integer avgTimeChange;
  private EnvironmentType environmentType;

  /**
   * Instantiates a new Deployment statistics.
   */
  public DeploymentStatistics() {
    super(StatisticsType.DEPLOYMENT);
  }

  /**
   * Gets count.
   *
   * @return the count
   */
  public Integer getCount() {
    return count;
  }

  /**
   * Sets count.
   *
   * @param count the count
   */
  public void setCount(Integer count) {
    this.count = count;
  }

  /**
   * Gets count change.
   *
   * @return the count change
   */
  public Integer getCountChange() {
    return countChange;
  }

  /**
   * Sets count change.
   *
   * @param countChange the count change
   */
  public void setCountChange(Integer countChange) {
    this.countChange = countChange;
  }

  /**
   * Gets avg time.
   *
   * @return the avg time
   */
  public Integer getAvgTime() {
    return avgTime;
  }

  /**
   * Sets avg time.
   *
   * @param avgTime the avg time
   */
  public void setAvgTime(Integer avgTime) {
    this.avgTime = avgTime;
  }

  /**
   * Gets avg time change.
   *
   * @return the avg time change
   */
  public Integer getAvgTimeChange() {
    return avgTimeChange;
  }

  /**
   * Sets avg time change.
   *
   * @param avgTimeChange the avg time change
   */
  public void setAvgTimeChange(Integer avgTimeChange) {
    this.avgTimeChange = avgTimeChange;
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

  @Override
  public int hashCode() {
    return Objects.hash(count, countChange, avgTime, avgTimeChange, environmentType);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final DeploymentStatistics other = (DeploymentStatistics) obj;
    return Objects.equals(this.count, other.count) && Objects.equals(this.countChange, other.countChange)
        && Objects.equals(this.avgTime, other.avgTime) && Objects.equals(this.avgTimeChange, other.avgTimeChange)
        && Objects.equals(this.environmentType, other.environmentType);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("count", count)
        .add("countChange", countChange)
        .add("avgTime", avgTime)
        .add("avgTimeChange", avgTimeChange)
        .add("environmentType", environmentType)
        .toString();
  }

  /**
   * The type Builder.
   */
  public static final class Builder {
    private Integer count;
    private Integer countChange;
    private Integer avgTime;
    private Integer avgTimeChange;
    private EnvironmentType environmentType;

    private Builder() {}

    /**
     * A deployment statistics builder.
     *
     * @return the builder
     */
    public static Builder aDeploymentStatistics() {
      return new Builder();
    }

    /**
     * With count builder.
     *
     * @param count the count
     * @return the builder
     */
    public Builder withCount(Integer count) {
      this.count = count;
      return this;
    }

    /**
     * With count change builder.
     *
     * @param countChange the count change
     * @return the builder
     */
    public Builder withCountChange(Integer countChange) {
      this.countChange = countChange;
      return this;
    }

    /**
     * With avg time builder.
     *
     * @param avgTime the avg time
     * @return the builder
     */
    public Builder withAvgTime(Integer avgTime) {
      this.avgTime = avgTime;
      return this;
    }

    /**
     * With avg time change builder.
     *
     * @param avgTimeChange the avg time change
     * @return the builder
     */
    public Builder withAvgTimeChange(Integer avgTimeChange) {
      this.avgTimeChange = avgTimeChange;
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
      return aDeploymentStatistics()
          .withCount(count)
          .withCountChange(countChange)
          .withAvgTime(avgTime)
          .withAvgTimeChange(avgTimeChange)
          .withEnvironmentType(environmentType);
    }

    /**
     * Build deployment statistics.
     *
     * @return the deployment statistics
     */
    public DeploymentStatistics build() {
      DeploymentStatistics deploymentStatistics = new DeploymentStatistics();
      deploymentStatistics.setCount(count);
      deploymentStatistics.setCountChange(countChange);
      deploymentStatistics.setAvgTime(avgTime);
      deploymentStatistics.setAvgTimeChange(avgTimeChange);
      deploymentStatistics.setEnvironmentType(environmentType);
      return deploymentStatistics;
    }
  }
}
