package io.harness.pms.cdng.artifact.bean;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXTERNAL_PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import io.harness.delegate.task.artifacts.ArtifactSourceType;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.TypeAlias;

@Data
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@TypeAlias("artifactSpecWrapperPms")
public class ArtifactSpecWrapperPms {
  String uuid;
  @NotNull @JsonProperty("type") ArtifactSourceType sourceType;
  @JsonProperty("spec")
  @JsonTypeInfo(use = NAME, property = "type", include = EXTERNAL_PROPERTY, visible = true)
  ArtifactConfigPms artifactConfigPms;

  // Use Builder as Constructor then only external property(visible) will be filled.
  @Builder
  public ArtifactSpecWrapperPms(ArtifactSourceType sourceType, ArtifactConfigPms artifactConfigPms) {
    this.sourceType = sourceType;
    this.artifactConfigPms = artifactConfigPms;
  }
}
