package io.harness.states;

import static io.harness.data.structure.UUIDGenerator.generateUuid;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ngpipeline.status.BuildStatusUpdateParameter;
import io.harness.pms.contracts.facilitators.FacilitatorObtainment;
import io.harness.pms.contracts.facilitators.FacilitatorType;
import io.harness.pms.execution.OrchestrationFacilitatorType;
import io.harness.pms.sdk.core.plan.PlanNode;

@OwnedBy(HarnessTeam.CI)
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
                .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.TASK).build())
                .build())
        .build();
  }
}
