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
@JsonTypeName("cleanup")
public class CleanupStepInfo extends AbstractStepWithMetaInfo {
  @JsonView(JsonViews.Internal.class)
  @NotNull
  public static final TypeInfo typeInfo = TypeInfo.builder()
                                              .stepInfoType(StepInfoType.CLEANUP)
                                              .stepType(StepType.builder().type(StepInfoType.CLEANUP.name()).build())
                                              .build();

  @Builder
  public CleanupStepInfo(
      String type, String identifier, String name, List<String> dependencies, Integer retry, Integer timeout) {
    super(type, identifier, name, dependencies, retry, timeout);
  }

  @Override
  public TypeInfo getNonYamlInfo() {
    return typeInfo;
  }
}
