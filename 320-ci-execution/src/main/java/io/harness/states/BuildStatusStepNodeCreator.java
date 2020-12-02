package io.harness.states;

import static io.harness.data.structure.UUIDGenerator.generateUuid;

import io.harness.facilitator.OrchestrationFacilitatorType;
import io.harness.ngpipeline.status.BuildStatusUpdateParameter;
import io.harness.pms.facilitators.FacilitatorObtainment;
import io.harness.pms.facilitators.FacilitatorType;
import io.harness.pms.sdk.core.plan.PlanNode;

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
        .facilitatorObtainment(
            FacilitatorObtainment.newBuilder()
                .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.TASK_V3).build())
                .build())
        .build();
  }
}
