package io.harness.ngtriggers.beans.source.artifact;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXTERNAL_PROPERTY;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ngtriggers.beans.source.NGTriggerSpecV2;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@OwnedBy(PIPELINE)
public class ArtifactTriggerConfig implements NGTriggerSpecV2, BuildAware {
  String stageIdentifier;
  String artifactRef;
  ArtifactType type;

  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = EXTERNAL_PROPERTY, property = "type", visible = true)
  ArtifactTypeSpec spec;

  @Builder
  public ArtifactTriggerConfig(ArtifactType type, ArtifactTypeSpec spec) {
    this.type = type;
    this.spec = spec;
  }

  @Override
  public String fetchStageRef() {
    return stageIdentifier;
  }

  @Override
  public String fetchbuildRef() {
    return artifactRef;
  }

  @Override
  public String fetchBuildType() {
    return spec.fetchBuildType();
  }
}
