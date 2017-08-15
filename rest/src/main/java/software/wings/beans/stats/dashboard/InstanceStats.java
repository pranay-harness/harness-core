package software.wings.beans.stats.dashboard;

import java.util.List;
import java.util.Map;

/**
 * @author rktummala on 08/13/17
 */
public class InstanceStats {
  private long totalCount;
  private List<EntitySummary> entitySummaryList;

  public long getTotalCount() {
    return totalCount;
  }

  public void setTotalCount(long totalCount) {
    this.totalCount = totalCount;
  }

  public List<EntitySummary> getEntitySummaryList() {
    return entitySummaryList;
  }

  public void setEntitySummaryList(List<EntitySummary> entitySummaryList) {
    this.entitySummaryList = entitySummaryList;
  }

  public static final class Builder {
    private long totalCount;
    private List<EntitySummary> entitySummaryList;

    private Builder() {}

    public static Builder anInstanceSummaryStats() {
      return new Builder();
    }

    public Builder withTotalCount(long totalCount) {
      this.totalCount = totalCount;
      return this;
    }

    public Builder withEntitySummaryList(List<EntitySummary> entitySummaryList) {
      this.entitySummaryList = entitySummaryList;
      return this;
    }

    public Builder but() {
      return anInstanceSummaryStats().withTotalCount(totalCount).withEntitySummaryList(entitySummaryList);
    }

    public InstanceStats build() {
      InstanceStats instanceSummaryStats = new InstanceStats();
      instanceSummaryStats.setEntitySummaryList(entitySummaryList);
      instanceSummaryStats.setTotalCount(totalCount);
      return instanceSummaryStats;
    }
  }
}