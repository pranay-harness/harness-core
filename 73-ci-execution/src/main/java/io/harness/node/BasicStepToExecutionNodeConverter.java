package io.harness.node;

import com.google.inject.Singleton;

import graph.StepInfoGraph;
import io.harness.adviser.AdviserObtainment;
import io.harness.adviser.AdviserType;
import io.harness.adviser.impl.success.OnSuccessAdviserParameters;
import io.harness.beans.steps.AbstractStepWithMetaInfo;
import io.harness.beans.steps.StepMetadata;
import io.harness.facilitator.FacilitatorObtainment;
import io.harness.facilitator.FacilitatorType;
import io.harness.plan.ExecutionNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Converts a step to execution Node by adding SYNC facilitators and ON_SUCCESS advisers
 */

@Singleton
public class BasicStepToExecutionNodeConverter implements StepToExecutionNodeConverter<AbstractStepWithMetaInfo> {
  @Override
  public ExecutionNode convertStep(AbstractStepWithMetaInfo step, List<String> nextStepUuids) {
    return ExecutionNode.builder()
        .name(step.getStepMetadata().getUuid())
        .uuid(step.getStepMetadata().getUuid())
        .stepType(step.getNonYamlInfo().getStepType())
        .identifier(step.getIdentifier())
        .stepParameters(step)
        .facilitatorObtainment(getFacilitatorsFromMetaData(step.getStepMetadata()))
        .adviserObtainments(getAdviserObtainmentFromMetaData(step.getStepMetadata(), nextStepUuids))
        .build();
  }

  private FacilitatorObtainment getFacilitatorsFromMetaData(StepMetadata stepMetadata) {
    return FacilitatorObtainment.builder().type(FacilitatorType.builder().type(FacilitatorType.SYNC).build()).build();
  }

  private List<AdviserObtainment> getAdviserObtainmentFromMetaData(
      StepMetadata stepMetadata, List<String> nextStepUuids) {
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
