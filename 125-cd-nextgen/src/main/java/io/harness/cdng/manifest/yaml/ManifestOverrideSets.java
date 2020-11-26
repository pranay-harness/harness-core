package io.harness.cdng.manifest.yaml;

import io.harness.cdng.visitor.YamlTypes;
import io.harness.cdng.visitor.helpers.manifest.ManifestOverridesVisitorHelper;
import io.harness.data.validator.EntityIdentifier;
import io.harness.walktree.beans.LevelNode;
import io.harness.walktree.beans.VisitableChildren;
import io.harness.walktree.visitor.SimpleVisitorHelper;
import io.harness.walktree.visitor.Visitable;
import io.harness.yaml.core.intfc.OverrideSetsWrapper;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName("overrideSet")
@SimpleVisitorHelper(helperClass = ManifestOverridesVisitorHelper.class)
@TypeAlias("manifestOverrideSets")
public class ManifestOverrideSets implements OverrideSetsWrapper, Visitable {
  @EntityIdentifier String identifier;
  List<ManifestConfigWrapper> manifests;

  // For Visitor Framework Impl
  String metadata;

  @Override
  public VisitableChildren getChildrenToWalk() {
    VisitableChildren children = VisitableChildren.builder().build();
    manifests.forEach(manifest -> children.add("manifests", manifest));
    return children;
  }

  @Override
  public LevelNode getLevelNode() {
    return LevelNode.builder()
        .qualifierName(YamlTypes.MANIFEST_OVERRIDE_SETS + YamlTypes.PATH_CONNECTOR + identifier)
        .build();
  }
}
