package io.harness.cdng.artifact.bean.yaml;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.cdng.artifact.bean.ArtifactConfigWrapper;
import io.harness.cdng.artifact.bean.SidecarArtifactWrapper;
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
  // TODO(archit): include sidecars after primary is done.
}
