package software.wings.beans.stats;

import com.google.common.base.MoreObjects;

import software.wings.beans.Environment.EnvironmentType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Created by anubhaw on 8/15/16.
 */
public class DeploymentStatistics extends WingsStatistics {
  private Map<EnvironmentType, AggregatedDayStats> statsMap = new HashMap<>();

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
  public Map<EnvironmentType, AggregatedDayStats> getStatsMap() {
    return statsMap;
  }

  /**
   * Sets stats map.
   *
   * @param statsMap the stats map
   */
  public void setStatsMap(Map<EnvironmentType, AggregatedDayStats> statsMap) {
    this.statsMap = statsMap;
  }

  /**
   * The type Aggregrated day stats.
   */
  public static class AggregatedDayStats {
    private int totalCount;
    private int failedCount;
    private int instancesCount;
    private List<DayStat> daysStats;

    public AggregatedDayStats(int totalCount, int failedCount, int instancesCount, List<DayStat> daysStats) {
      this.totalCount = totalCount;
      this.failedCount = failedCount;
      this.instancesCount = instancesCount;
      this.daysStats = daysStats;
    }

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
     * Gets days stats.
     *
     * @return the days stats
     */
    public List<DayStat> getDaysStats() {
      return daysStats;
    }

    /**
     * Sets days stats.
     *
     * @param daysStats the days stats
     */
    public void setDaysStats(List<DayStat> daysStats) {
      this.daysStats = daysStats;
    }

    /**
     * The type Day stats.
     */
    public static class DayStat {
      private int totalCount;
      private int failedCount;
      private int instancesCount;
      private Long date;

      public DayStat(int totalCount, int failedCount, int instancesCount, Long date) {
        this.totalCount = totalCount;
        this.failedCount = failedCount;
        this.instancesCount = instancesCount;
        this.date = date;
      }

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

      @Override
      public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("totalCount", totalCount)
            .add("failedCount", failedCount)
            .add("instancesCount", instancesCount)
            .add("date", date)
            .toString();
      }

      @Override
      public int hashCode() {
        return Objects.hash(totalCount, failedCount, instancesCount, date);
      }

      @Override
      public boolean equals(Object obj) {
        if (this == obj) {
          return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
          return false;
        }
        final DayStat other = (DayStat) obj;
        return Objects.equals(this.totalCount, other.totalCount) && Objects.equals(this.failedCount, other.failedCount)
            && Objects.equals(this.instancesCount, other.instancesCount) && Objects.equals(this.date, other.date);
      }
    }
  }
}
