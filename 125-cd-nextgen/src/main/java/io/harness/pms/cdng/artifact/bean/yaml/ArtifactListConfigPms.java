package io.harness.pms.cdng.artifact.bean.yaml;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.pms.cdng.artifact.bean.ArtifactSpecWrapperPms;
import io.harness.pms.cdng.artifact.bean.SidecarArtifactWrapperPms;

import java.beans.ConstructorProperties;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import org.springframework.data.annotation.TypeAlias;

/**
 * This is Yaml POJO class which may contain expressions as well.
 * Used mainly for converter layer to store yaml.
 */
@Data
@Builder
@TypeAlias("artifactListConfigPms")
public class ArtifactListConfigPms {
  ArtifactSpecWrapperPms primary;
  @Singular List<SidecarArtifactWrapperPms> sidecars;

  @ConstructorProperties({"primary", "sidecars", "metadata"})
  public ArtifactListConfigPms(ArtifactSpecWrapperPms primary, List<SidecarArtifactWrapperPms> sidecars) {
    this.primary = primary;
    if (primary != null) {
      this.primary.getArtifactConfigPms().setIdentifier("primary");
      this.primary.getArtifactConfigPms().setPrimaryArtifact(true);
    }
    this.sidecars = sidecars;
    if (isNotEmpty(sidecars)) {
      for (SidecarArtifactWrapperPms sidecar : this.sidecars) {
        sidecar.getArtifactConfigPms().setIdentifier(sidecar.getIdentifier());
        sidecar.getArtifactConfigPms().setPrimaryArtifact(false);
      }
    }
  }
}
