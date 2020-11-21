package io.harness.cdng.artifact.bean;

import io.harness.data.Outcome;
import io.harness.ngpipeline.pipeline.executions.beans.WithArtifactSummary;
import io.harness.yaml.core.intfc.WithIdentifier;

public interface ArtifactOutcome extends Outcome, WithIdentifier, WithArtifactSummary {
  boolean isPrimaryArtifact();
}
