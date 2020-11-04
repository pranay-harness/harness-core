package io.harness.node;

import com.google.inject.Singleton;

import graph.StepInfoGraph;
import io.harness.adviser.AdviserObtainment;
import io.harness.adviser.AdviserType;
import io.harness.advisers.success.OnSuccessAdviserParameters;
import io.harness.beans.steps.CIStepInfo;
import io.harness.facilitator.FacilitatorObtainment;
import io.harness.facilitator.FacilitatorType;
import io.harness.plan.PlanNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Converts a step to execution Node by adding SYNC facilitators and ON_SUCCESS advisers
 */

@Singleton
public class BasicStepToExecutionNodeConverter implements StepToExecutionNodeConverter<CIStepInfo> {
  @Override
  public PlanNode convertStep(CIStepInfo step, List<String> nextStepUuids) {
    return PlanNode.builder()
        .name(step.getIdentifier())
        .uuid(step.getIdentifier())
        .stepType(step.getNonYamlInfo().getStepType())
        .identifier(step.getIdentifier())
        .stepParameters(step)
        .facilitatorObtainment(getFacilitatorsFromMetaData())
        .adviserObtainments(getAdviserObtainmentFromMetaData(nextStepUuids))
        .build();
  }

  private FacilitatorObtainment getFacilitatorsFromMetaData() {
    return FacilitatorObtainment.builder().type(FacilitatorType.builder().type(FacilitatorType.SYNC).build()).build();
  }

  private List<AdviserObtainment> getAdviserObtainmentFromMetaData(List<String> nextStepUuids) {
    List<AdviserObtainment> adviserObtainments = new ArrayList<>();

    // TODO Handle parallel execution
    if (!nextStepUuids.isEmpty() && !StepInfoGraph.isNILStepUuId(nextStepUuids.get(0))) {
      adviserObtainments.add(
          AdviserObtainment.builder()
              .type(AdviserType.builder().type(AdviserType.ON_SUCCESS).build())
              .parameters(OnSuccessAdviserParameters.builder().nextNodeId(nextStepUuids.get(0)).build())
              .build());
    }

    return adviserObtainments;
  }
}
