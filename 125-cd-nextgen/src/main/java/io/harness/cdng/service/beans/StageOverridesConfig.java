package io.harness.cdng.service.beans;

import io.harness.cdng.artifact.bean.yaml.ArtifactListConfig;
import io.harness.cdng.manifest.yaml.ManifestConfigWrapper;
import io.harness.cdng.visitor.helpers.serviceconfig.StageOverridesVisitorHelper;
import io.harness.walktree.beans.VisitableChildren;
import io.harness.walktree.visitor.SimpleVisitorHelper;
import io.harness.walktree.visitor.Visitable;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
@SimpleVisitorHelper(helperClass = StageOverridesVisitorHelper.class)
public class StageOverridesConfig implements Visitable {
  List<String> useArtifactOverrideSets;
  ArtifactListConfig artifacts;
  List<String> useManifestOverrideSets;
  List<ManifestConfigWrapper> manifests;

  // For Visitor Framework Impl
  String metadata;

  @Override
  public VisitableChildren getChildrenToWalk() {
    VisitableChildren children = VisitableChildren.builder().build();
    children.add("artifacts", artifacts);
    manifests.forEach(manifest -> children.add("manifests", manifest));
    // add override sets if necessary
    return children;
  }
}
