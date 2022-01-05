package io.harness.cdng.k8s;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXTERNAL_PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.pipeline.CdAbstractStepNode;
import io.harness.executions.steps.StepSpecTypeConstants;
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
@JsonTypeName(StepSpecTypeConstants.K8S_BG_SWAP_SERVICES)
@TypeAlias("K8sBGSwapServicesStepNode")
@OwnedBy(CDP)
@RecasterAlias("io.harness.cdng.k8s.K8sBGSwapServicesStepNode")
public class K8sBGSwapServicesStepNode extends CdAbstractStepNode {
  @JsonProperty("type") @NotNull StepType type = StepType.K8sBGSwapServices;
  @JsonProperty("spec")
  @JsonTypeInfo(use = NAME, property = "type", include = EXTERNAL_PROPERTY, visible = true)
  K8sBGSwapServicesStepInfo k8sBGSwapServicesStepInfo;
  @Override
  public String getType() {
    return StepSpecTypeConstants.K8S_BG_SWAP_SERVICES;
  }

  @Override
  public StepSpecType getStepSpecType() {
    return k8sBGSwapServicesStepInfo;
  }

  enum StepType {
    K8sBGSwapServices(StepSpecTypeConstants.K8S_BG_SWAP_SERVICES);
    @Getter String name;
    StepType(String name) {
      this.name = name;
    }
  }
}
