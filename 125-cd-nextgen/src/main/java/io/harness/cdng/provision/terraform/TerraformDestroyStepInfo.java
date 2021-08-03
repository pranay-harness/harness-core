package io.harness.cdng.provision.terraform;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.ParameterField;
import io.harness.cdng.pipeline.CDStepInfo;
import io.harness.data.structure.EmptyPredicate;
import io.harness.executions.steps.StepSpecTypeConstants;
import io.harness.filters.WithConnectorRef;
import io.harness.plancreator.steps.common.SpecParameters;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.execution.OrchestrationFacilitatorType;
import io.harness.validation.Validator;
import io.harness.walktree.visitor.Visitable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.TypeAlias;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@OwnedBy(HarnessTeam.CDP)
@TypeAlias("terraformDestroyStepInfo")
@JsonTypeName(StepSpecTypeConstants.TERRAFORM_DESTROY)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TerraformDestroyStepInfo
    extends TerraformDestroyBaseStepInfo implements CDStepInfo, Visitable, WithConnectorRef {
  @NotNull @JsonProperty("configuration") TerraformStepConfiguration terraformStepConfiguration;

  @Builder(builderMethodName = "infoBuilder")
  public TerraformDestroyStepInfo(ParameterField<String> provisionerIdentifier,
      ParameterField<List<String>> delegateSelectors, TerraformStepConfiguration terraformStepConfiguration) {
    super(provisionerIdentifier, delegateSelectors);
    this.terraformStepConfiguration = terraformStepConfiguration;
  }

  @Override
  @JsonIgnore
  public StepType getStepType() {
    return TerraformDestroyStep.STEP_TYPE;
  }

  @Override
  @JsonIgnore
  public String getFacilitatorType() {
    return OrchestrationFacilitatorType.TASK;
  }

  @Override
  public SpecParameters getSpecParameters() {
    validateSpecParams();
    return TerraformDestroyStepParameters.infoBuilder()
        .provisionerIdentifier(provisionerIdentifier)
        .delegateSelectors(delegateSelectors)
        .configuration(terraformStepConfiguration.toStepParameters())
        .build();
  }

  void validateSpecParams() {
    Validator.notNullCheck("Terraform Step configuration is null", terraformStepConfiguration);
    terraformStepConfiguration.validateParams();
  }

  @Override
  public Map<String, ParameterField<String>> extractConnectorRefs() {
    validateSpecParams();
    Map<String, ParameterField<String>> connectorRefMap = new HashMap<>();

    if (terraformStepConfiguration.terraformStepConfigurationType == TerraformStepConfigurationType.INLINE) {
      TerraformExecutionData terraformExecutionData = terraformStepConfiguration.terraformExecutionData;

      connectorRefMap.put("configuration.spec.configFiles.store.spec.connectorRef",
          terraformExecutionData.getTerraformConfigFilesWrapper().store.getSpec().getConnectorReference());

      List<TerraformVarFileWrapper> terraformVarFiles = terraformExecutionData.getTerraformVarFiles();

      if (EmptyPredicate.isNotEmpty(terraformVarFiles)) {
        for (TerraformVarFileWrapper terraformVarFile : terraformVarFiles) {
          if (terraformVarFile.getVarFile().getType().equals(TerraformVarFileTypes.Remote)) {
            connectorRefMap.put("configuration.spec.varFiles." + terraformVarFile.getVarFile().identifier
                    + ".spec.store.spec.connectorRef",
                ((RemoteTerraformVarFileSpec) terraformVarFile.varFile.spec).store.getSpec().getConnectorReference());
          }
        }
      }
    }
    return connectorRefMap;
  }
}
