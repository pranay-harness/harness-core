package software.wings.beans.stats;

import com.google.common.base.MoreObjects;

import software.wings.beans.Environment.EnvironmentType;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Created by anubhaw on 10/13/16.
 */
public class AppKeyStatistics extends WingsStatistics {
  private Map<EnvironmentType, AppKeyStatsBreakdown> statsMap = new HashMap<>();

  /**
   * Instantiates a new Wings statistics.
   */
  public AppKeyStatistics() {
    super(StatisticsType.APP_KEY_STATISTICS);
  }

  public Map<EnvironmentType, AppKeyStatsBreakdown> getStatsMap() {
    return statsMap;
  }

  public void setStatsMap(Map<EnvironmentType, AppKeyStatsBreakdown> statsMap) {
    this.statsMap = statsMap;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("statsMap", statsMap).toString();
  }

  @Override
  public int hashCode() {
    return Objects.hash(statsMap);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final AppKeyStatistics other = (AppKeyStatistics) obj;
    return Objects.equals(this.statsMap, other.statsMap);
  }

  public static class AppKeyStatsBreakdown {
    private int deploymentCount;
    private int artifactCount;
    private int instanceCount;

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

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
          .add("deploymentCount", deploymentCount)
          .add("artifactCount", artifactCount)
          .add("instanceCount", instanceCount)
          .toString();
    }

    @Override
    public int hashCode() {
      return Objects.hash(deploymentCount, artifactCount, instanceCount);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null || getClass() != obj.getClass()) {
        return false;
      }
      final AppKeyStatsBreakdown other = (AppKeyStatsBreakdown) obj;
      return Objects.equals(this.deploymentCount, other.deploymentCount)
          && Objects.equals(this.artifactCount, other.artifactCount)
          && Objects.equals(this.instanceCount, other.instanceCount);
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
      public AppKeyStatsBreakdown build() {
        AppKeyStatsBreakdown appKeyStatsBreakdown = new AppKeyStatsBreakdown();
        appKeyStatsBreakdown.setDeploymentCount(deploymentCount);
        appKeyStatsBreakdown.setArtifactCount(artifactCount);
        appKeyStatsBreakdown.setInstanceCount(instanceCount);
        return appKeyStatsBreakdown;
      }
    }
  }
}
