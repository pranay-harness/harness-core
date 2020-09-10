package io.harness.cdng.artifact.bean.yaml;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.harness.cdng.artifact.bean.ArtifactSpecWrapper;
import io.harness.cdng.artifact.bean.SidecarArtifactWrapper;
import io.harness.cdng.visitor.helpers.serviceconfig.ArtifactListConfigVisitorHelper;
import io.harness.walktree.beans.VisitableChildren;
import io.harness.walktree.visitor.SimpleVisitorHelper;
import io.harness.walktree.visitor.Visitable;
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
@SimpleVisitorHelper(helperClass = ArtifactListConfigVisitorHelper.class)
public class ArtifactListConfig implements Visitable {
  ArtifactSpecWrapper primary;
  @Singular List<SidecarArtifactWrapper> sidecars;

  @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
  public ArtifactListConfig(@JsonProperty("primary") ArtifactSpecWrapper primary,
      @JsonProperty("sidecars") List<SidecarArtifactWrapper> sidecars) {
    this.primary = primary;
    if (primary != null) {
      this.primary.getArtifactConfig().setIdentifier("primary");
      this.primary.getArtifactConfig().setPrimaryArtifact(true);
    }
    this.sidecars = sidecars;
    if (isNotEmpty(sidecars)) {
      for (SidecarArtifactWrapper sidecar : this.sidecars) {
        sidecar.getArtifactConfig().setIdentifier(sidecar.getIdentifier());
        sidecar.getArtifactConfig().setPrimaryArtifact(false);
      }
    }
  }

  @Override
  public VisitableChildren getChildrenToWalk() {
    VisitableChildren children = VisitableChildren.builder().build();
    children.add("primary", primary);
    sidecars.forEach(sidecar -> children.add("sidecars", sidecar));
    return children;
  }
}
