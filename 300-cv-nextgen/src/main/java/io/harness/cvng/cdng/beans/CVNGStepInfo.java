package io.harness.cvng.cdng.beans;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXTERNAL_PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.common.SwaggerConstants;
import io.harness.cvng.cdng.services.impl.CVNGStep;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.execution.OrchestrationFacilitatorType;
import io.harness.pms.sdk.core.steps.io.StepParameters;
import io.harness.pms.yaml.ParameterField;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.base.Preconditions;
import io.swagger.annotations.ApiModelProperty;
import java.beans.ConstructorProperties;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;

@Data
@NoArgsConstructor
@EqualsAndHashCode
@JsonTypeName("Verify")
@TypeAlias("verificationStepInfo")
@OwnedBy(HarnessTeam.CV)
public class CVNGStepInfo implements CVStepInfoBase {
  private static final String SERVICE_IDENTIFIER_EXPRESSION = "<+service.identifier>";
  private static final String ENV_IDENTIFIER_EXPRESSION = "<+env.identifier>";
  @NotNull
  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH, value = "It supports runtime input and expression")
  ParameterField<String> monitoredServiceRef;
  @NotNull List<HealthSource> healthSources;
  @NotNull String type;
  @JsonTypeInfo(use = NAME, property = "type", include = EXTERNAL_PROPERTY, visible = true) VerificationJobSpec spec;
  @Builder
  @ConstructorProperties({"monitoredServiceRef", "healthSources", "type", "spec"})
  public CVNGStepInfo(ParameterField<String> monitoredServiceRef, List<HealthSource> healthSources, String type,
      VerificationJobSpec spec) {
    this.type = spec.getType();
    this.healthSources = healthSources;
    this.monitoredServiceRef = monitoredServiceRef;
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
        .serviceIdentifier(createExpressionField(SERVICE_IDENTIFIER_EXPRESSION))
        .envIdentifier(createExpressionField(ENV_IDENTIFIER_EXPRESSION))
        .deploymentTag(spec.getDeploymentTag())
        .monitoredServiceRef(monitoredServiceRef)
        .verificationJobBuilder(spec.getVerificationJobBuilder())
        .build();
  }

  private ParameterField<String> createExpressionField(String expression) {
    return ParameterField.createExpressionField(true, expression, null, true);
  }

  public void validate() {
    Preconditions.checkNotNull(monitoredServiceRef, "monitoredServiceRef can not be null");
    spec.validate();
  }
}
