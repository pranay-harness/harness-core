package io.harness.beans.steps.nodes;

import static io.harness.annotations.dev.HarnessTeam.CI;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXTERNAL_PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.steps.CIAbstractStepNode;
import io.harness.beans.steps.CIStepInfoType;
import io.harness.beans.steps.stepinfo.RunStepInfo;
import io.harness.yaml.core.StepSpecType;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("Run")
@TypeAlias("RunStepNode")
@OwnedBy(CI)
@RecasterAlias("io.harness.beans.steps.nodes.RunStepNode")
public class RunStepNode extends CIAbstractStepNode {
  @JsonProperty("type") @NotNull StepType type = StepType.Run;
  @JsonProperty("spec")
  @JsonTypeInfo(use = NAME, property = "type", include = EXTERNAL_PROPERTY, visible = true)
  RunStepInfo runStepInfo;
  @Override
  public String getType() {
    return CIStepInfoType.RUN.getDisplayName();
  }

  @Override
  public StepSpecType getStepSpecType() {
    return runStepInfo;
  }

  enum StepType {
    Run(CIStepInfoType.RUN.getDisplayName());
    @Getter String name;
    StepType(String name) {
      this.name = name;
    }
  }
}
