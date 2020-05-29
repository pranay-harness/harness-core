package io.harness.beans.steps.stepinfo;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonView;
import io.harness.beans.script.ScriptInfo;
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
@JsonTypeName("build")
public class BuildStepInfo extends AbstractStepWithMetaInfo {
  @JsonView(JsonViews.Internal.class)
  @NotNull
  public static final TypeInfo typeInfo = TypeInfo.builder()
                                              .stepInfoType(StepInfoType.BUILD)
                                              .stepType(StepType.builder().type(StepInfoType.BUILD.name()).build())
                                              .build();

  private List<ScriptInfo> scriptInfos;

  @Builder
  public BuildStepInfo(String type, String identifier, String name, List<String> dependencies, Integer retry,
      Integer timeout, List<ScriptInfo> scriptInfos) {
    super(type, identifier, name, dependencies, retry, timeout);
    this.scriptInfos = scriptInfos;
  }

  @Override
  public TypeInfo getNonYamlInfo() {
    return typeInfo;
  }
}
