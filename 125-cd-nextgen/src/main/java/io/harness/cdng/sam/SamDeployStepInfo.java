package io.harness.cdng.sam;

import io.harness.cdng.pipeline.CDStepInfo;
import io.harness.executions.steps.StepSpecTypeConstants;
import io.harness.plancreator.steps.common.SpecParameters;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.execution.OrchestrationFacilitatorType;
import io.harness.pms.yaml.ParameterField;
import io.harness.yaml.core.variables.NGVariable;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonTypeName(StepSpecTypeConstants.AWS_SAM_DEPLOY)
public class SamDeployStepInfo implements CDStepInfo, SpecParameters {
  String region;
  String stackName;
  List<NGVariable> overrides;
  String globalAdditionalFlags;
  String s3BucketName;
  private ParameterField<List<String>> delegateSelectors;

  @Override
  public StepType getStepType() {
    return SamDeployStep.STEP_TYPE;
  }

  @Override
  public String getFacilitatorType() {
    return OrchestrationFacilitatorType.TASK;
  }

  @Override
  public SpecParameters getSpecParameters() {
    return this;
  }
}
