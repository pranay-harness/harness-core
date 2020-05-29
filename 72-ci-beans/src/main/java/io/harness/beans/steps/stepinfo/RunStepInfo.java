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
import java.util.Optional;
import javax.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("run")
public class RunStepInfo extends AbstractStepWithMetaInfo {
  @JsonView(JsonViews.Internal.class)
  @NotNull
  public static final TypeInfo typeInfo = TypeInfo.builder()
                                              .stepInfoType(StepInfoType.RUN)
                                              .stepType(StepType.builder().type(StepInfoType.RUN.name()).build())
                                              .build();

  private String workingDirectory;
  private boolean runInBackground;
  private List<String> envVariables;
  private String image;
  private List<String> command;
  private String envVarsOutput;

  @Builder
  public RunStepInfo(String type, String identifier, String name, List<String> dependencies, Integer retry,
      Integer timeout, String workingDirectory, Boolean runInBackground, List<String> envVariables, String image,
      List<String> command, String envVarsOutput) {
    super(type, identifier, name, dependencies, retry, timeout);
    this.workingDirectory = workingDirectory;
    this.runInBackground = Optional.ofNullable(runInBackground).orElse(Boolean.FALSE);
    this.envVariables = envVariables;
    this.image = image;
    this.command = command;
    this.envVarsOutput = envVarsOutput;
  }

  @Override
  public TypeInfo getNonYamlInfo() {
    return typeInfo;
  }
}
