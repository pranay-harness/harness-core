package io.harness.cdng.artifact.bean;

import io.harness.ngpipeline.pipeline.executions.beans.WithArtifactSummary;
import io.harness.pms.sdk.core.data.Outcome;
import io.harness.yaml.core.intfc.WithIdentifier;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "artifactType")
@JsonSubTypes({ @JsonSubTypes.Type(value = DockerArtifactOutcome.class, name = "Dockerhub") })
public interface ArtifactOutcome extends Outcome, WithIdentifier, WithArtifactSummary {
  boolean isPrimaryArtifact();
  String getArtifactType();
}
