package io.harness.cdng.artifact.bean;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXTERNAL_PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import io.harness.cdng.visitor.YamlTypes;
import io.harness.cdng.visitor.helpers.artifact.ArtifactSpecWrapperVisitorHelper;
import io.harness.delegate.task.artifacts.ArtifactSourceType;
import io.harness.walktree.beans.LevelNode;
import io.harness.walktree.beans.VisitableChildren;
import io.harness.walktree.visitor.SimpleVisitorHelper;
import io.harness.walktree.visitor.Visitable;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@SimpleVisitorHelper(helperClass = ArtifactSpecWrapperVisitorHelper.class)
public class ArtifactSpecWrapper implements Visitable {
  @NotNull @JsonProperty("type") ArtifactSourceType sourceType;
  @JsonProperty("spec")
  @JsonTypeInfo(use = NAME, property = "type", include = EXTERNAL_PROPERTY, visible = true)
  ArtifactConfig artifactConfig;

  // For Visitor Framework Impl
  String metadata;

  // Use Builder as Constructor then only external property(visible) will be filled.
  @Builder
  public ArtifactSpecWrapper(ArtifactSourceType sourceType, ArtifactConfig artifactConfig) {
    this.sourceType = sourceType;
    this.artifactConfig = artifactConfig;
  }

  @Override
  public VisitableChildren getChildrenToWalk() {
    VisitableChildren visitableChildren = VisitableChildren.builder().build();
    visitableChildren.add("artifactConfig", artifactConfig);
    return visitableChildren;
  }

  @Override
  public LevelNode getLevelNode() {
    return LevelNode.builder().qualifierName(YamlTypes.ARTIFACT_SPEC_WRAPPER).build();
  }
}
