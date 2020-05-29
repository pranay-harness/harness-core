package io.harness.beans.steps.stepinfo;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonView;
import io.harness.beans.ArtifactUploadInfo;
import io.harness.beans.steps.AbstractStepWithMetaInfo;
import io.harness.beans.steps.StepInfoType;
import io.harness.beans.steps.TypeInfo;
import io.harness.state.StepType;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.jersey.JsonViews;

import java.util.List;
import javax.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("upload-artifact")
public class ArtifactUploadStepInfo extends AbstractStepWithMetaInfo {
  @JsonView(JsonViews.Internal.class)
  @NotNull
  public static final TypeInfo typeInfo =
      TypeInfo.builder()
          .stepInfoType(StepInfoType.UPLOAD_ARTIFACT)
          .stepType(StepType.builder().type(StepInfoType.UPLOAD_ARTIFACT.name()).build())
          .build();

  private ArtifactUploadInfo artifactUploadInfo;

  @Builder
  public ArtifactUploadStepInfo(String type, String identifier, String name, List<String> dependencies, Integer retry,
      Integer timeout, ArtifactUploadInfo artifactUploadInfo) {
    super(type, identifier, name, dependencies, retry, timeout);
    this.artifactUploadInfo = artifactUploadInfo;
  }

  @Override
  public TypeInfo getNonYamlInfo() {
    return typeInfo;
  }
}
