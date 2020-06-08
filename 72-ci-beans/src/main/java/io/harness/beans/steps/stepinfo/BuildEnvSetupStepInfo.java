package io.harness.beans.steps.stepinfo;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonView;
import io.harness.beans.environment.BuildJobEnvInfo;
import io.harness.beans.steps.AbstractStepWithMetaInfo;
import io.harness.beans.steps.StepInfoType;
import io.harness.beans.steps.TypeInfo;
import io.harness.executionplan.plancreator.beans.GenericStepInfo;
import io.harness.facilitator.FacilitatorType;
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
@JsonTypeName("setup-env")
public class BuildEnvSetupStepInfo extends AbstractStepWithMetaInfo implements GenericStepInfo {
  @JsonView(JsonViews.Internal.class)
  @NotNull
  public static final TypeInfo typeInfo = TypeInfo.builder()
                                              .stepInfoType(StepInfoType.SETUP_ENV)
                                              .stepType(StepType.builder().type(StepInfoType.SETUP_ENV.name()).build())
                                              .build();
  @NotNull private BuildJobEnvInfo buildJobEnvInfo;
  @NotNull private String gitConnectorIdentifier;
  @NotNull private String branchName;

  @Builder
  public BuildEnvSetupStepInfo(String type, String identifier, String name, List<String> dependencies, Integer retry,
      Integer timeout, BuildJobEnvInfo buildJobEnvInfo, String gitConnectorIdentifier, String branchName) {
    super(type, identifier, name, dependencies, retry, timeout);
    this.buildJobEnvInfo = buildJobEnvInfo;
    this.gitConnectorIdentifier = gitConnectorIdentifier;
    this.branchName = branchName;
  }

  @Override
  public TypeInfo getNonYamlInfo() {
    return typeInfo;
  }

  @Override
  public StepType getStepType() {
    return typeInfo.getStepType();
  }

  @Override
  public String getFacilitatorType() {
    return FacilitatorType.SYNC;
  }
}
