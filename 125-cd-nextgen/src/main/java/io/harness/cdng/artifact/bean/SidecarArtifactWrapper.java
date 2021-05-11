package io.harness.cdng.artifact.bean;

import io.harness.cdng.artifact.bean.yaml.SidecarArtifact;
import io.harness.cdng.visitor.helpers.artifact.SidecarWrapperArtifactVisitorHelper;
import io.harness.walktree.beans.VisitableChildren;
import io.harness.walktree.visitor.SimpleVisitorHelper;
import io.harness.walktree.visitor.Visitable;

import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@SimpleVisitorHelper(helperClass = SidecarWrapperArtifactVisitorHelper.class)
@TypeAlias("sidecarArtifactWrapper")
public class SidecarArtifactWrapper implements Visitable {
  SidecarArtifact sidecar;

  // For Visitor Framework Impl
  @Getter(onMethod_ = { @ApiModelProperty(hidden = true) }) @ApiModelProperty(hidden = true) String metadata;

  @Override
  public VisitableChildren getChildrenToWalk() {
    VisitableChildren children = VisitableChildren.builder().build();
    children.add(sidecar.getIdentifier(), sidecar);
    return children;
  }
}
