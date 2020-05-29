package io.harness.beans.steps.stepinfo;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonView;
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
@JsonTypeName("git-clone")
public class GitCloneStepInfo extends AbstractStepWithMetaInfo {
  @JsonView(JsonViews.Internal.class)
  @NotNull
  public static final TypeInfo typeInfo = TypeInfo.builder()
                                              .stepInfoType(StepInfoType.GIT_CLONE)
                                              .stepType(StepType.builder().type(StepInfoType.GIT_CLONE.name()).build())
                                              .build();
  private String gitConnector;
  private String branch;
  private String path;

  @Builder
  public GitCloneStepInfo(String type, String identifier, String name, List<String> dependencies, Integer retry,
      Integer timeout, String gitConnector, String branch, String path) {
    super(type, identifier, name, dependencies, retry, timeout);
    this.gitConnector = gitConnector;
    this.branch = branch;
    this.path = path;
  }

  @Override
  public TypeInfo getNonYamlInfo() {
    return typeInfo;
  }
}
