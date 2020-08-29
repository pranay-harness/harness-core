package io.harness.cdng.artifact.bean;

import io.harness.data.Outcome;
import io.harness.yaml.core.intfc.WithIdentifier;

public interface ArtifactOutcome extends Outcome, WithIdentifier { String getArtifactType(); }
