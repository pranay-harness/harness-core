package io.harness.cdng.artifact.bean.yaml;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.harness.cdng.artifact.bean.ArtifactSpecWrapper;
import io.harness.cdng.artifact.bean.SidecarArtifactWrapper;
import io.harness.cdng.artifact.utils.ArtifactUtils;
import io.harness.data.structure.EmptyPredicate;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.List;

/**
 * This is Yaml POJO class which may contain expressions as well.
 * Used mainly for converter layer to store yaml.
 */
@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class ArtifactListConfig {
  ArtifactSpecWrapper primary;
  @Singular List<SidecarArtifactWrapper> sidecars;

  @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
  public ArtifactListConfig(@JsonProperty("primary") ArtifactSpecWrapper primary,
      @JsonProperty("sidecars") List<SidecarArtifactWrapper> sidecars) {
    this.primary = primary;
    if (primary != null) {
      this.primary.getArtifactConfig().setIdentifier("primary");
      this.primary.getArtifactConfig().setArtifactType(ArtifactUtils.PRIMARY_ARTIFACT);
    }
    this.sidecars = sidecars;
    if (EmptyPredicate.isNotEmpty(sidecars)) {
      for (SidecarArtifactWrapper sidecar : this.sidecars) {
        sidecar.getArtifactConfig().setIdentifier(sidecar.getIdentifier());
        sidecar.getArtifactConfig().setArtifactType(ArtifactUtils.SIDECAR_ARTIFACT);
      }
    }
  }
}
