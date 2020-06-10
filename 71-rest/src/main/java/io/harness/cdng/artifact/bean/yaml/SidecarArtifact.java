package io.harness.cdng.artifact.bean.yaml;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.cdng.artifact.bean.ArtifactConfigWrapper;
import io.harness.cdng.artifact.bean.ArtifactSourceType;
import io.harness.cdng.artifact.bean.SidecarArtifactWrapper;
import io.harness.cdng.artifact.utils.ArtifactUtils;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonTypeName("sidecar")
public class SidecarArtifact implements SidecarArtifactWrapper {
  String identifier;
  @JsonIgnore ArtifactConfigWrapper artifact;

  @JsonProperty(ArtifactSourceType.DOCKER_HUB)
  public void setDockerHub(DockerHubArtifactConfig sidecar) {
    this.artifact = sidecar;
    this.artifact.setIdentifier(this.identifier);
    this.artifact.setArtifactType(ArtifactUtils.SIDECAR_ARTIFACT);
  }
  @JsonProperty(ArtifactSourceType.GCR)
  public void setGCR(GcrArtifactConfig sidecar) {
    this.artifact = sidecar;
    this.artifact.setIdentifier(this.identifier);
    this.artifact.setArtifactType(ArtifactUtils.SIDECAR_ARTIFACT);
  }
}
