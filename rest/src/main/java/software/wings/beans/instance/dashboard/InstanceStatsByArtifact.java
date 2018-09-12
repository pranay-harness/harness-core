package software.wings.beans.instance.dashboard;

import io.harness.exception.WingsException;

/**
 * @author rktummala on 08/13/17
 */
public class InstanceStatsByArtifact extends InstanceStatsByEntity {
  @Override
  public ArtifactSummary getEntitySummary() {
    return (ArtifactSummary) super.getEntitySummary();
  }

  @Override
  protected void setEntitySummary(AbstractEntitySummary entitySummary) {
    if (!(entitySummary instanceof ArtifactSummary)) {
      throw new WingsException("EntitySummary is not instance of type ArtifactSummary");
    }
    this.entitySummary = entitySummary;
  }

  public static final class Builder extends InstanceStatsByEntity.Builder {
    private Builder() {}

    public static Builder anInstanceStatsByArtifact() {
      return new Builder();
    }

    @Override
    public Builder withEntitySummary(AbstractEntitySummary entitySummary) {
      if (!(entitySummary instanceof ArtifactSummary)) {
        throw new WingsException("EntitySummary is not instance of type ArtifactSummary");
      }
      this.entitySummary = entitySummary;
      return this;
    }

    public Builder but() {
      return (Builder) anInstanceStatsByArtifact().withEntitySummary(entitySummary).withInstanceStats(instanceStats);
    }

    public InstanceStatsByArtifact build() {
      InstanceStatsByArtifact instanceStatsByArtifact = new InstanceStatsByArtifact();
      instanceStatsByArtifact.setInstanceStats(instanceStats);
      instanceStatsByArtifact.setEntitySummary(entitySummary);
      return instanceStatsByArtifact;
    }
  }
}