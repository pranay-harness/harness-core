package io.harness.cdng.artifact.bean.yaml;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.cdng.artifact.bean.ArtifactConfigWrapper;
import io.harness.cdng.artifact.bean.SidecarArtifactWrapper;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonTypeName("sidecar")
public class SidecarArtifact implements SidecarArtifactWrapper {
  String identifier;
  ArtifactConfigWrapper artifact;

  @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
  public SidecarArtifact(
      @JsonProperty("identifier") String identifier, @JsonProperty("dockerhub") ArtifactConfigWrapper sidecar) {
    this.identifier = identifier;
    this.artifact = sidecar;
  }
}
