package io.harness.cdng.artifact.bean.yaml;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXTERNAL_PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.cdng.artifact.bean.ArtifactConfig;
import io.harness.cdng.artifact.bean.SidecarArtifactWrapper;
import io.harness.cdng.visitor.LevelNodeQualifierName;
import io.harness.cdng.visitor.helpers.artifact.SidecarArtifactVisitorHelper;
import io.harness.data.validator.EntityIdentifier;
import io.harness.delegate.task.artifacts.ArtifactSourceType;
import io.harness.walktree.beans.LevelNode;
import io.harness.walktree.beans.VisitableChildren;
import io.harness.walktree.visitor.SimpleVisitorHelper;
import io.harness.walktree.visitor.Visitable;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@JsonTypeName("sidecar")
@SimpleVisitorHelper(helperClass = SidecarArtifactVisitorHelper.class)
public class SidecarArtifact implements SidecarArtifactWrapper, Visitable {
  @EntityIdentifier String identifier;
  @NotNull @JsonProperty("type") ArtifactSourceType sourceType;
  @JsonProperty("spec")
  @JsonTypeInfo(use = NAME, property = "type", include = EXTERNAL_PROPERTY, visible = true)
  ArtifactConfig artifactConfig;

  // For Visitor Framework Impl
  String metadata;

  // Use Builder as Constructor then only external property(visible) will be filled.
  @Builder
  public SidecarArtifact(String identifier, ArtifactSourceType sourceType, ArtifactConfig artifactConfig) {
    this.identifier = identifier;
    this.sourceType = sourceType;
    this.artifactConfig = artifactConfig;
  }

  @Override
  public VisitableChildren getChildrenToWalk() {
    VisitableChildren children = VisitableChildren.builder().build();
    children.add("artifactConfig", artifactConfig);
    return children;
  }

  @Override
  public LevelNode getLevelNode() {
    return LevelNode.builder().qualifierName(LevelNodeQualifierName.SIDECAR_ARTIFACT_CONFIG).build();
  }
}
