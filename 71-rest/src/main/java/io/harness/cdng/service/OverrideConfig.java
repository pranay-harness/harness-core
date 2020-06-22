package io.harness.cdng.service;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.harness.cdng.artifact.bean.yaml.ArtifactListConfig;
import io.harness.cdng.manifest.state.ManifestListConfig;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
@Builder
public class OverrideConfig implements Serializable {
  private ManifestListConfig manifestListConfig;
  private ArtifactListConfig artifactListConfig;

  @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
  public OverrideConfig(@JsonProperty("manifests") ManifestListConfig manifestListConfig,
      @JsonProperty("artifacts") ArtifactListConfig artifactListConfig) {
    this.manifestListConfig = manifestListConfig;
    this.artifactListConfig = artifactListConfig;
  }
}
