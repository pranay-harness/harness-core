package software.wings.beans.instance.dashboard;

/**
 * @author rktummala on 08/13/17
 */
public class EntitySummaryStats {
  private EntitySummary entitySummary;
  private long count;

  public EntitySummary getEntitySummary() {
    return entitySummary;
  }

  public void setEntitySummary(EntitySummary entitySummary) {
    this.entitySummary = entitySummary;
  }

  public long getCount() {
    return count;
  }

  public void setCount(long count) {
    this.count = count;
  }

  public static final class Builder {
    private EntitySummary entitySummary;
    private long count;

    private Builder() {}

    public static Builder anEntitySummaryStats() {
      return new Builder();
    }

    public Builder entitySummary(EntitySummary entitySummary) {
      this.entitySummary = entitySummary;
      return this;
    }

    public Builder count(long count) {
      this.count = count;
      return this;
    }

    public EntitySummaryStats build() {
      EntitySummaryStats entitySummaryStats = new EntitySummaryStats();
      entitySummaryStats.setEntitySummary(entitySummary);
      entitySummaryStats.setCount(count);
      return entitySummaryStats;
    }
  }
}
