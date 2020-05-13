package io.harness.node;

import com.google.inject.Singleton;

import graph.StepGraph;
import io.harness.adviser.AdviserObtainment;
import io.harness.adviser.AdviserType;
import io.harness.adviser.impl.success.OnSuccessAdviserParameters;
import io.harness.beans.steps.CIStep;
import io.harness.beans.steps.StepMetadata;
import io.harness.facilitator.FacilitatorObtainment;
import io.harness.facilitator.FacilitatorType;
import io.harness.plan.ExecutionNode;
import io.harness.redesign.levels.StepLevel;

import java.util.ArrayList;
import java.util.List;

/**
 * Converts a step to execution Node by adding SYNC facilitators and ON_SUCCESS advisers
 */

@Singleton
public class BasicStepToExecutionNodeConverter implements StepToExecutionNodeConverter<CIStep> {
  @Override
  public ExecutionNode convertStep(CIStep step, String nextStepUuid) {
    return ExecutionNode.builder()
        .name(step.getStepInfo().getStepIdentifier())
        .uuid(step.getStepMetadata().getUuid())
        .stateType(step.getStepInfo().getStateType())
        .levelType(StepLevel.LEVEL_TYPE)
        .stateParameters(step.getStepInfo())
        .facilitatorObtainment(getFacilitatorsFromMetaData(step.getStepMetadata()))
        .adviserObtainments(getAdviserObtainmentFromMetaData(step.getStepMetadata(), nextStepUuid))
        .build();
  }

  private FacilitatorObtainment getFacilitatorsFromMetaData(StepMetadata stepMetadata) {
    return FacilitatorObtainment.builder().type(FacilitatorType.builder().type(FacilitatorType.SYNC).build()).build();
  }

  private List<AdviserObtainment> getAdviserObtainmentFromMetaData(StepMetadata stepMetadata, String nextStepUuid) {
    List<AdviserObtainment> adviserObtainments = new ArrayList<>();

    if (!StepGraph.isNILStepUuId(nextStepUuid)) {
      adviserObtainments.add(AdviserObtainment.builder()
                                 .type(AdviserType.builder().type(AdviserType.ON_SUCCESS).build())
                                 .parameters(OnSuccessAdviserParameters.builder().nextNodeId(nextStepUuid).build())
                                 .build());
    }

    return adviserObtainments;
  }
}
