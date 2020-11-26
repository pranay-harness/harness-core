package io.harness.cdng.artifact.bean.yaml;

import io.harness.cdng.visitor.YamlTypes;
import io.harness.cdng.visitor.helpers.artifact.ArtifactOverridesVisitorHelper;
import io.harness.data.validator.EntityIdentifier;
import io.harness.walktree.beans.LevelNode;
import io.harness.walktree.beans.VisitableChildren;
import io.harness.walktree.visitor.SimpleVisitorHelper;
import io.harness.walktree.visitor.Visitable;
import io.harness.yaml.core.intfc.OverrideSetsWrapper;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName("overrideSet")
@SimpleVisitorHelper(helperClass = ArtifactOverridesVisitorHelper.class)
@TypeAlias("artifactOverrideSets")
public class ArtifactOverrideSets implements OverrideSetsWrapper, Visitable {
  @EntityIdentifier String identifier;
  ArtifactListConfig artifacts;

  @Override
  public VisitableChildren getChildrenToWalk() {
    VisitableChildren children = VisitableChildren.builder().build();
    children.add("artifacts", artifacts);
    return children;
  }
  @Override
  public LevelNode getLevelNode() {
    return LevelNode.builder()
        .qualifierName(YamlTypes.ARTIFACT_OVERRIDE_SETS + YamlTypes.PATH_CONNECTOR + identifier)
        .build();
  }
}
