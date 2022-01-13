/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

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

    @Override
    public Builder but() {
      return (Builder) anInstanceStatsByArtifact().withEntitySummary(entitySummary).withInstanceStats(instanceStats);
    }

    @Override
    public InstanceStatsByArtifact build() {
      InstanceStatsByArtifact instanceStatsByArtifact = new InstanceStatsByArtifact();
      instanceStatsByArtifact.setInstanceStats(instanceStats);
      instanceStatsByArtifact.setEntitySummary(entitySummary);
      return instanceStatsByArtifact;
    }
  }
}
