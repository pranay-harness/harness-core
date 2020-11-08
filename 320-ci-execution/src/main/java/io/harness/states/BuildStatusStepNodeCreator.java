package io.harness.states;

import static io.harness.data.structure.UUIDGenerator.generateUuid;

import io.harness.facilitator.FacilitatorObtainment;
import io.harness.facilitator.FacilitatorType;
import io.harness.ngpipeline.status.BuildStatusUpdateParameter;
import io.harness.plan.PlanNode;

public class BuildStatusStepNodeCreator {
  private static final String POST_COMMIT_STATUS_NAME = "POST_COMMIT_STATUS";

  public static PlanNode prepareBuildStatusStepNode(
      String state, String desc, String sha, String identifier, String connectorRef) {
    final String statusPlanId = generateUuid();

    return PlanNode.builder()
        .uuid(statusPlanId)
        .name(POST_COMMIT_STATUS_NAME)
        .identifier(statusPlanId)
        .stepType(BuildStatusStep.STEP_TYPE)
        .stepParameters(BuildStatusUpdateParameter.builder()
                            .state(state)
                            .sha(sha)
                            .desc(desc)
                            .connectorIdentifier(connectorRef)
                            .identifier(identifier)
                            .build())
        .facilitatorObtainment(FacilitatorObtainment.builder()
                                   .type(FacilitatorType.builder().type(FacilitatorType.TASK_V3).build())
                                   .build())
        .build();
  }
}