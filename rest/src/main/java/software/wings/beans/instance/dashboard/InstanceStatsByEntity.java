package software.wings.beans.instance.dashboard;

/**
 * @author rktummala on 08/13/17
 */
public class InstanceStatsByEntity {
  protected AbstractEntitySummary entitySummary;
  protected InstanceStats instanceStats;

  public InstanceStats getInstanceStats() {
    return instanceStats;
  }

  protected void setInstanceStats(InstanceStats instanceStats) {
    this.instanceStats = instanceStats;
  }

  public AbstractEntitySummary getEntitySummary() {
    return entitySummary;
  }

  protected void setEntitySummary(AbstractEntitySummary entitySummary) {
    this.entitySummary = entitySummary;
  }

  public static class Builder {
    protected AbstractEntitySummary entitySummary;
    protected InstanceStats instanceStats;

    protected Builder() {}

    public static Builder anInstanceSummaryStats() {
      return new Builder();
    }

    public Builder withEntitySummary(AbstractEntitySummary entitySummary) {
      this.entitySummary = entitySummary;
      return this;
    }

    public Builder withInstanceStats(InstanceStats instanceStats) {
      this.instanceStats = instanceStats;
      return this;
    }

    public Builder but() {
      return anInstanceSummaryStats().withInstanceStats(instanceStats).withEntitySummary(entitySummary);
    }

    public InstanceStatsByEntity build() {
      InstanceStatsByEntity instanceStatsByArtifact = new InstanceStatsByEntity();
      instanceStatsByArtifact.setInstanceStats(instanceStats);
      instanceStatsByArtifact.setEntitySummary(entitySummary);
      return instanceStatsByArtifact;
    }
  }
}