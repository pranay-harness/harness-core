package io.harness.cdng.manifest.yaml;

import io.harness.cdng.visitor.YamlTypes;
import io.harness.cdng.visitor.helpers.manifest.ManifestWrapperConfigVisitorHelper;
import io.harness.walktree.beans.LevelNode;
import io.harness.walktree.beans.VisitableChildren;
import io.harness.walktree.visitor.SimpleVisitorHelper;
import io.harness.walktree.visitor.Visitable;

import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@SimpleVisitorHelper(helperClass = ManifestWrapperConfigVisitorHelper.class)
@TypeAlias("manifestConfigWrapper")
public class ManifestConfigWrapper implements Visitable {
  ManifestConfig manifest;

  @Override
  public LevelNode getLevelNode() {
    return LevelNode.builder().qualifierName(YamlTypes.MANIFEST_LIST_CONFIG).build();
  }

  @Override
  public VisitableChildren getChildrenToWalk() {
    VisitableChildren children = VisitableChildren.builder().build();
    children.add("manifest", manifest);
    return children;
  }
}
