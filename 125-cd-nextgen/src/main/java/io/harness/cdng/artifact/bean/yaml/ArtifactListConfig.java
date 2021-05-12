package io.harness.cdng.artifact.bean.yaml;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.cdng.artifact.bean.ArtifactSpecWrapper;
import io.harness.cdng.artifact.bean.SidecarArtifactWrapper;
import io.harness.cdng.visitor.helpers.artifact.ArtifactListConfigVisitorHelper;
import io.harness.walktree.beans.VisitableChildren;
import io.harness.walktree.visitor.SimpleVisitorHelper;
import io.harness.walktree.visitor.Visitable;

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
@SimpleVisitorHelper(helperClass = ArtifactListConfigVisitorHelper.class)
@TypeAlias("artifactListConfig")
public class ArtifactListConfig implements Visitable {
  ArtifactSpecWrapper primary;
  @Singular List<SidecarArtifactWrapper> sidecars;

  // For Visitor Framework Impl
  String metadata;

  @ConstructorProperties({"primary", "sidecars", "metadata"})
  public ArtifactListConfig(ArtifactSpecWrapper primary, List<SidecarArtifactWrapper> sidecars, String metadata) {
    this.primary = primary;
    if (primary != null) {
      this.primary.getArtifactConfig().setIdentifier("primary");
      this.primary.getArtifactConfig().setPrimaryArtifact(true);
    }
    this.sidecars = sidecars;
    if (isNotEmpty(sidecars)) {
      for (SidecarArtifactWrapper sidecar : this.sidecars) {
        sidecar.getSidecar().getArtifactConfig().setIdentifier(sidecar.getSidecar().getIdentifier());
        sidecar.getSidecar().getArtifactConfig().setPrimaryArtifact(false);
      }
    }
  }

  @Override
  public VisitableChildren getChildrenToWalk() {
    VisitableChildren children = VisitableChildren.builder().build();
    children.add("primary", primary);
    if (isNotEmpty(sidecars)) {
      sidecars.forEach(sidecar -> children.add("sidecars", sidecar));
    }
    return children;
  }
}
