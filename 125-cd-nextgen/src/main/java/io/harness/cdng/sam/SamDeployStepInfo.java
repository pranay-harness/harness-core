package io.harness.cdng.sam;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.cdng.pipeline.CDStepInfo;
import io.harness.executions.steps.StepSpecTypeConstants;
import io.harness.plancreator.steps.common.SpecParameters;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.execution.OrchestrationFacilitatorType;
import io.harness.yaml.core.variables.NGVariable;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@JsonTypeName(StepSpecTypeConstants.AWS_SAM_DEPLOY)
public class SamDeployStepInfo implements CDStepInfo, SpecParameters {
    String region;
    String stackName;
    List<NGVariable> overrides;
    String globalAdditionalFlags;

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
