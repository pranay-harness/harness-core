package io.harness.cdng.artifact.bean.yaml;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.cdng.visitor.LevelNodeQualifierName;
import io.harness.cdng.visitor.helpers.artifact.ArtifactOverridesVisitorHelper;
import io.harness.data.validator.EntityIdentifier;
import io.harness.walktree.beans.LevelNode;
import io.harness.walktree.beans.VisitableChildren;
import io.harness.walktree.visitor.SimpleVisitorHelper;
import io.harness.walktree.visitor.Visitable;
import io.harness.yaml.core.intfc.OverrideSetsWrapper;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName("overrideSet")
@SimpleVisitorHelper(helperClass = ArtifactOverridesVisitorHelper.class)
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
    return LevelNode.builder().qualifierName(LevelNodeQualifierName.ARTIFACT_OVERRIDE_SETS).build();
  }
}
