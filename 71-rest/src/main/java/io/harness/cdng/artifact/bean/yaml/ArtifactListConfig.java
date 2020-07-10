package io.harness.cdng.artifact.bean.yaml;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.WRAPPER_OBJECT;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.harness.cdng.artifact.bean.ArtifactConfigWrapper;
import io.harness.cdng.artifact.bean.SidecarArtifactWrapper;
import io.harness.cdng.artifact.utils.ArtifactUtils;
import io.harness.data.Outcome;
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
public class ArtifactListConfig implements Outcome {
  ArtifactConfigWrapper primary;
  @Singular List<SidecarArtifactWrapper> sidecars;

  @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
  public ArtifactListConfig(
      @JsonProperty("primary") @JsonTypeInfo(use = NAME, include = WRAPPER_OBJECT) ArtifactConfigWrapper primary,
      @JsonProperty("sidecars") List<SidecarArtifactWrapper> sidecars) {
    this.primary = primary;
    if (primary != null) {
      this.primary.setIdentifier("primary");
      this.primary.setArtifactType(ArtifactUtils.PRIMARY_ARTIFACT);
    }
    this.sidecars = sidecars;
  }
}
