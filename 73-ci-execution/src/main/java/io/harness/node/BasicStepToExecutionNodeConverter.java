package io.harness.node;

import com.google.inject.Singleton;

import graph.CIStepsGraph;
import io.harness.adviser.AdviserObtainment;
import io.harness.adviser.AdviserType;
import io.harness.adviser.impl.success.OnSuccessAdviserParameters;
import io.harness.beans.steps.CIStep;
import io.harness.beans.steps.CIStepMetadata;
import io.harness.facilitator.FacilitatorObtainment;
import io.harness.facilitator.FacilitatorType;
import io.harness.plan.ExecutionNode;
import io.harness.redesign.levels.StepLevel;

/**
 * Converts a step to execution Node by adding SYNC facilitators and ON_SUCCESS advisers
 */

@Singleton
public class BasicStepToExecutionNodeConverter implements StepToExecutionNodeConverter<CIStep> {
  @Override
  public ExecutionNode convertStep(CIStep step, String nextStepUuid) {
    return ExecutionNode.builder()
        .name(step.getCiStepInfo().getStepName())
        .uuid(step.getCiStepMetadata().getUuid())
        .stateType(step.getCiStepInfo().getStateType())
        .levelType(StepLevel.LEVEL_TYPE)
        .stateParameters(step.getCiStepInfo())
        .facilitatorObtainment(getFacilitatorsFromMetaData(step.getCiStepMetadata()))
        .adviserObtainment(getAdviserObtainmentFromMetaData(step.getCiStepMetadata(), nextStepUuid))
        .build();
  }

  private FacilitatorObtainment getFacilitatorsFromMetaData(CIStepMetadata ciStepMetadata) {
    return FacilitatorObtainment.builder().type(FacilitatorType.builder().type(FacilitatorType.SYNC).build()).build();
  }

  private AdviserObtainment getAdviserObtainmentFromMetaData(CIStepMetadata ciStepMetadata, String nextStepUuid) {
    AdviserObtainment adviserObtainment = null;

    if (!CIStepsGraph.isNILStepUuId(nextStepUuid)) {
      adviserObtainment = AdviserObtainment.builder()
                              .type(AdviserType.builder().type(AdviserType.ON_SUCCESS).build())
                              .parameters(OnSuccessAdviserParameters.builder().nextNodeId(nextStepUuid).build())
                              .build();
    }

    return adviserObtainment;
  }
}
