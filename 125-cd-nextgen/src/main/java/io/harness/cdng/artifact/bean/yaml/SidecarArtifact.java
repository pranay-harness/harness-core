package io.harness.cdng.artifact.bean.yaml;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXTERNAL_PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.cdng.artifact.bean.ArtifactConfig;
import io.harness.cdng.artifact.bean.SidecarArtifactWrapper;
import io.harness.delegate.task.artifacts.ArtifactSourceType;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@JsonTypeName("sidecar")
public class SidecarArtifact implements SidecarArtifactWrapper {
  String identifier;
  @NotNull @JsonProperty("type") ArtifactSourceType sourceType;
  @JsonProperty("spec")
  @JsonTypeInfo(use = NAME, property = "type", include = EXTERNAL_PROPERTY, visible = true)
  ArtifactConfig artifactConfig;

  // Use Builder as Constructor then only external property(visible) will be filled.
  @Builder
  public SidecarArtifact(String identifier, ArtifactSourceType sourceType, ArtifactConfig artifactConfig) {
    this.identifier = identifier;
    this.sourceType = sourceType;
    this.artifactConfig = artifactConfig;
  }
}
