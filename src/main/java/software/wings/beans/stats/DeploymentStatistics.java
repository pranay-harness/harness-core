package software.wings.beans.stats;

import software.wings.beans.Environment.EnvironmentType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by anubhaw on 8/15/16.
 */
public class DeploymentStatistics extends WingsStatistics {
  private Map<EnvironmentType, List<DayStats>> statsMap = new HashMap<>();

  /**
   * Instantiates a new Deployment statistics.
   */
  public DeploymentStatistics() {
    super(StatisticsType.DEPLOYMENT);
  }

  /**
   * Gets stats map.
   *
   * @return the stats map
   */
  public Map<EnvironmentType, List<DayStats>> getStatsMap() {
    return statsMap;
  }

  /**
   * Sets stats map.
   *
   * @param statsMap the stats map
   */
  public void setStatsMap(Map<EnvironmentType, List<DayStats>> statsMap) {
    this.statsMap = statsMap;
  }

  /**
   * The type Day stats.
   */
  public static class DayStats {
    private int totalCount;
    private int failedCount;
    private int instancesCount;
    private Long date;

    /**
     * Gets total count.
     *
     * @return the total count
     */
    public int getTotalCount() {
      return totalCount;
    }

    /**
     * Sets total count.
     *
     * @param totalCount the total count
     */
    public void setTotalCount(int totalCount) {
      this.totalCount = totalCount;
    }

    /**
     * Gets failed count.
     *
     * @return the failed count
     */
    public int getFailedCount() {
      return failedCount;
    }

    /**
     * Sets failed count.
     *
     * @param failedCount the failed count
     */
    public void setFailedCount(int failedCount) {
      this.failedCount = failedCount;
    }

    /**
     * Gets instances count.
     *
     * @return the instances count
     */
    public int getInstancesCount() {
      return instancesCount;
    }

    /**
     * Sets instances count.
     *
     * @param instancesCount the instances count
     */
    public void setInstancesCount(int instancesCount) {
      this.instancesCount = instancesCount;
    }

    /**
     * Gets date.
     *
     * @return the date
     */
    public Long getDate() {
      return date;
    }

    /**
     * Sets date.
     *
     * @param date the date
     */
    public void setDate(Long date) {
      this.date = date;
    }

    /**
     * The type Builder.
     */
    public static final class Builder {
      private int totalCount;
      private int failedCount;
      private int instancesCount;
      private Long date;

      private Builder() {}

      /**
       * A day stats builder.
       *
       * @return the builder
       */
      public static Builder aDayStats() {
        return new Builder();
      }

      /**
       * With total count builder.
       *
       * @param totalCount the total count
       * @return the builder
       */
      public Builder withTotalCount(int totalCount) {
        this.totalCount = totalCount;
        return this;
      }

      /**
       * With failed count builder.
       *
       * @param failedCount the failed count
       * @return the builder
       */
      public Builder withFailedCount(int failedCount) {
        this.failedCount = failedCount;
        return this;
      }

      /**
       * With instances count builder.
       *
       * @param instancesCount the instances count
       * @return the builder
       */
      public Builder withInstancesCount(int instancesCount) {
        this.instancesCount = instancesCount;
        return this;
      }

      /**
       * With date builder.
       *
       * @param date the date
       * @return the builder
       */
      public Builder withDate(long date) {
        this.date = date;
        return this;
      }

      /**
       * But builder.
       *
       * @return the builder
       */
      public Builder but() {
        return aDayStats()
            .withTotalCount(totalCount)
            .withFailedCount(failedCount)
            .withInstancesCount(instancesCount)
            .withDate(date);
      }

      /**
       * Build day stats.
       *
       * @return the day stats
       */
      public DayStats build() {
        DayStats dayStats = new DayStats();
        dayStats.setTotalCount(totalCount);
        dayStats.setFailedCount(failedCount);
        dayStats.setInstancesCount(instancesCount);
        dayStats.setDate(date);
        return dayStats;
      }
    }
  }
}
