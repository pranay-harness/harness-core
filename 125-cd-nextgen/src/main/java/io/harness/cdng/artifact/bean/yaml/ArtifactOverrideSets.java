package io.harness.cdng.artifact.bean.yaml;

import io.harness.cdng.visitor.helpers.artifact.ArtifactOverridesVisitorHelper;
import io.harness.data.validator.EntityIdentifier;
import io.harness.walktree.beans.VisitableChildren;
import io.harness.walktree.visitor.SimpleVisitorHelper;
import io.harness.walktree.visitor.Visitable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName("overrideSet")
@SimpleVisitorHelper(helperClass = ArtifactOverridesVisitorHelper.class)
@TypeAlias("artifactOverrideSets")
public class ArtifactOverrideSets implements Visitable {
  @EntityIdentifier String identifier;
  ArtifactListConfig artifacts;

  // For Visitor Framework Impl
  @Getter(onMethod_ = { @ApiModelProperty(hidden = true) }) @ApiModelProperty(hidden = true) String metadata;

  @Override
  public VisitableChildren getChildrenToWalk() {
    VisitableChildren children = VisitableChildren.builder().build();
    children.add("artifacts", artifacts);
    return children;
  }
}
