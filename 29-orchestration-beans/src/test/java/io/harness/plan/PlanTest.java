package io.harness.plan;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.PRASHANT;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.OrchestrationBeansTestBase;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.pms.steps.StepType;
import io.harness.rule.Owner;

import org.junit.Test;
import org.junit.experimental.categories.Category;

public class PlanTest extends OrchestrationBeansTestBase {
  private static final String PLAN_ID = generateUuid();
  private static final String DUMMY_NODE_1_ID = generateUuid();
  private static final String DUMMY_NODE_2_ID = generateUuid();
  private static final String DUMMY_NODE_3_ID = generateUuid();

  private static final StepType DUMMY_STEP_TYPE = StepType.newBuilder().setType("DUMMY").build();

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestFetchNode() {
    Plan plan = buildDummyPlan();
    PlanNode node1 = plan.fetchNode(DUMMY_NODE_1_ID);
    assertThat(node1).isNotNull();
    assertThat(node1.getName()).isEqualTo("Dummy Node 1");

    PlanNode node2 = plan.fetchNode(DUMMY_NODE_2_ID);
    assertThat(node2).isNotNull();
    assertThat(node2.getName()).isEqualTo("Dummy Node 2");

    assertThatThrownBy(() -> plan.fetchNode(generateUuid())).isInstanceOf(InvalidRequestException.class);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestFetchStartingNode() {
    Plan plan = buildDummyPlan();
    PlanNode startingNode = plan.fetchStartingNode();
    assertThat(startingNode).isNotNull();
    assertThat(startingNode.getName()).isEqualTo("Dummy Node 1");
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestIsEmpty() {
    Plan plan = Plan.builder().build();
    assertThat(plan.isEmpty()).isEqualTo(true);
  }

  private Plan buildDummyPlan() {
    return Plan.builder()
        .node(PlanNode.builder()
                  .uuid(DUMMY_NODE_1_ID)
                  .name("Dummy Node 1")
                  .stepType(DUMMY_STEP_TYPE)
                  .identifier("dummy1")
                  .build())
        .node(PlanNode.builder()
                  .uuid(DUMMY_NODE_2_ID)
                  .name("Dummy Node 2")
                  .stepType(DUMMY_STEP_TYPE)
                  .identifier("dummy2")
                  .build())
        .node(PlanNode.builder()
                  .uuid(DUMMY_NODE_3_ID)
                  .name("Dummy Node 3")
                  .stepType(DUMMY_STEP_TYPE)
                  .identifier("dummy3")
                  .build())
        .startingNodeId(DUMMY_NODE_1_ID)
        .build();
  }
}
