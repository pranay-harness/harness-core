package io.harness.plan;

import static io.harness.rule.OwnerRule.PRASHANTSHARMA;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.pms.contracts.advisers.AdviserObtainment;
import io.harness.pms.contracts.advisers.AdviserType;
import io.harness.pms.contracts.steps.StepCategory;
import io.harness.pms.contracts.steps.StepType;
import io.harness.rule.Owner;

import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(HarnessTeam.PIPELINE)
public class IdentityPlanNodeTest {
  StepType TEST_STEP_TYPE = StepType.newBuilder().setType("TEST_STEP_PLAN").setStepCategory(StepCategory.STEP).build();

  @Test
  @Owner(developers = PRASHANTSHARMA)
  @Category(UnitTests.class)
  public void testMapPlanNodeToIdentityNode() {
    PlanNode planNode =
        PlanNode.builder()
            .name("Test Node")
            .uuid("uuid")
            .identifier("test")
            .stepType(TEST_STEP_TYPE)
            .adviserObtainment(
                AdviserObtainment.newBuilder().setType(AdviserType.newBuilder().setType("NEXT_STEP").build()).build())
            .build();
    IdentityPlanNode identityPlanNodeExpected = IdentityPlanNode.builder()
                                                    .uuid("uuid")
                                                    .originalNodeExecutionId("originalNodeExecutionId")
                                                    .identifier("test")
                                                    .name("Test Node")
                                                    .build();
    IdentityPlanNode identityPlanNodeActual =
        IdentityPlanNode.mapPlanNodeToIdentityNode(planNode, "originalNodeExecutionId");
    assertThat(identityPlanNodeExpected).isEqualTo(identityPlanNodeActual);
  }
}
