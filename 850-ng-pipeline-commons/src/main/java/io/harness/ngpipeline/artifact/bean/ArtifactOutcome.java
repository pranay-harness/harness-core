package io.harness.ngpipeline.artifact.bean;

import io.harness.ngpipeline.pipeline.executions.beans.WithArtifactSummary;
import io.harness.pms.sdk.core.data.Outcome;
import io.harness.yaml.core.intfc.WithIdentifier;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "artifactType")
@JsonSubTypes({
  @JsonSubTypes.Type(value = DockerArtifactOutcome.class, name = "Dockerhub")
  , @JsonSubTypes.Type(value = GcrArtifactOutcome.class, name = "Gcr"),
      @JsonSubTypes.Type(value = EcrArtifactOutcome.class, name = "Ecr")
})
// TODO : Create a shared Module b/w pipline and CD/CI where these entities can go to and eventually We need to
// deprecate that module 878-pms-coupling
// @TargetModule(878-pms-coupling)
public interface ArtifactOutcome extends Outcome, WithIdentifier, WithArtifactSummary {
  boolean isPrimaryArtifact();
  String getArtifactType();
}
