package software.wings.beans.stats.dashboard;

import java.util.List;
import java.util.Map;

/**
 * @author rktummala on 08/13/17
 */
public class InstanceSummaryStats {
  private long totalCount;
  /**
   * Key - groupByEntityType, Value - List<EntitySummaryStats>
   */
  private Map<String, List<EntitySummaryStats>> countMap;

  public long getTotalCount() {
    return totalCount;
  }

  public void setTotalCount(long totalCount) {
    this.totalCount = totalCount;
  }

  public Map<String, List<EntitySummaryStats>> getCountMap() {
    return countMap;
  }

  public void setCountMap(Map<String, List<EntitySummaryStats>> countMap) {
    this.countMap = countMap;
  }

  public static final class Builder {
    private long totalCount;
    /**
     * Key - groupByEntityType, Value - List<EntitySummaryWithCount>
     */
    private Map<String, List<EntitySummaryStats>> countMap;

    private Builder() {}

    public static Builder anInstanceSummaryStats() {
      return new Builder();
    }

    public Builder withTotalCount(long totalCount) {
      this.totalCount = totalCount;
      return this;
    }

    public Builder withCountMap(Map<String, List<EntitySummaryStats>> countMap) {
      this.countMap = countMap;
      return this;
    }

    public Builder but() {
      return anInstanceSummaryStats().withTotalCount(totalCount).withCountMap(countMap);
    }

    public InstanceSummaryStats build() {
      InstanceSummaryStats instanceSummaryStats = new InstanceSummaryStats();
      instanceSummaryStats.setCountMap(countMap);
      instanceSummaryStats.setTotalCount(totalCount);
      return instanceSummaryStats;
    }
  }
}