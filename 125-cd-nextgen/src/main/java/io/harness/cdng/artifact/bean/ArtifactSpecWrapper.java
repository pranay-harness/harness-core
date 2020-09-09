package io.harness.cdng.artifact.bean;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXTERNAL_PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.harness.delegate.task.artifacts.ArtifactSourceType;
import io.harness.walktree.visitor.SimpleVisitorHelper;
import io.harness.walktree.visitor.Visitable;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.ArrayList;
import java.util.List;
import javax.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@SimpleVisitorHelper(helperClass = ArtifactSpecWrapper.class)
public class ArtifactSpecWrapper implements Visitable {
  @NotNull @JsonProperty("type") ArtifactSourceType sourceType;
  @JsonProperty("spec")
  @JsonTypeInfo(use = NAME, property = "type", include = EXTERNAL_PROPERTY, visible = true)
  ArtifactConfig artifactConfig;

  // Use Builder as Constructor then only external property(visible) will be filled.
  @Builder
  public ArtifactSpecWrapper(ArtifactSourceType sourceType, ArtifactConfig artifactConfig) {
    this.sourceType = sourceType;
    this.artifactConfig = artifactConfig;
  }

  @Override
  public List<Object> getChildrenToWalk() {
    List<Object> children = new ArrayList<>();
    children.add(artifactConfig);
    return children;
  }
}
