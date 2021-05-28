package io.harness.cvng.cdng.beans;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXTERNAL_PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cvng.cdng.services.impl.CVNGStep;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.execution.OrchestrationFacilitatorType;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.yaml.ParameterField;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModelProperty;
import java.beans.ConstructorProperties;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;

@Data
@NoArgsConstructor
@EqualsAndHashCode
@JsonTypeName("Verify")
@TypeAlias("verificationStepInfo")
@OwnedBy(HarnessTeam.CV)
public class CVNGStepInfo implements CVStepInfoBase {
  @Getter(onMethod_ = { @ApiModelProperty(hidden = true) }) @ApiModelProperty(hidden = true) String name;
  @NotNull String identifier;
  ParameterField<String> description;
  @NotNull String verificationJobRef;
  @NotNull String type;
  @JsonTypeInfo(use = NAME, property = "type", include = EXTERNAL_PROPERTY, visible = true) VerificationJobSpec spec;
  @Builder
  @ConstructorProperties({"name", "identifier", "verificationJobRef", "type", "spec"})
  public CVNGStepInfo(
      String name, String identifier, String verificationJobRef, String type, VerificationJobSpec spec) {
    this.name = name;
    this.identifier = identifier;
    this.verificationJobRef = verificationJobRef;
    this.type = spec.getType();
    this.spec = spec;
  }

  @Override
  public StepType getStepType() {
    return CVNGStep.STEP_TYPE;
  }

  @Override
  public String getFacilitatorType() {
    return OrchestrationFacilitatorType.ASYNC;
  }

  @Override
  public StepParameters getStepParameters() {
    return CVNGStepParameter.builder()
        .verificationJobIdentifier(verificationJobRef)
        .serviceIdentifier(spec.getServiceRef())
        .envIdentifier(spec.getEnvRef())
        .deploymentTag(spec.getDeploymentTag())
        .runtimeValues(spec.getRuntimeValues())
        .build();
  }
}
