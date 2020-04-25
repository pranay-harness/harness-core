package io.harness.plan;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.PRASHANT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.inject.Inject;

import io.harness.OrchestrationBeansTest;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.persistence.HPersistence;
import io.harness.plan.ExecutionPlan.ExecutionPlanKeys;
import io.harness.rule.Owner;
import io.harness.rule.RealMongo;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class ExecutionPlanTest extends OrchestrationBeansTest {
  String planId = generateUuid();
  String dummyNode1Id = generateUuid();
  String dummyNode2Id = generateUuid();
  String dummyNode3Id = generateUuid();

  @Inject private HPersistence hPersistence;

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  @RealMongo
  public void shouldTestExecutionPlanSave() {
    String savedPlanId = hPersistence.save(buildDummyPlan());
    assertThat(savedPlanId).isNotNull();
    assertThat(savedPlanId).isEqualTo(planId);
    ExecutionPlan savedPlan =
        hPersistence.createQuery(ExecutionPlan.class).filter(ExecutionPlanKeys.uuid, savedPlanId).get();

    assertThat(savedPlan).isNotNull();
    assertThat(savedPlan.getNodes()).hasSize(3);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestFetchNode() {
    ExecutionPlan plan = buildDummyPlan();
    ExecutionNode node1 = plan.fetchNode(dummyNode1Id);
    assertThat(node1).isNotNull();
    assertThat(node1.getName()).isEqualTo("Dummy Node 1");

    ExecutionNode node2 = plan.fetchNode(dummyNode2Id);
    assertThat(node2).isNotNull();
    assertThat(node2.getName()).isEqualTo("Dummy Node 2");

    assertThatThrownBy(() -> plan.fetchNode(generateUuid())).isInstanceOf(InvalidRequestException.class);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestFetchStartingNode() {
    ExecutionPlan plan = buildDummyPlan();
    ExecutionNode startingNode = plan.fetchStartingNode();
    assertThat(startingNode).isNotNull();
    assertThat(startingNode.getName()).isEqualTo("Dummy Node 1");
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestIsEmpty() {
    ExecutionPlan plan = ExecutionPlan.builder().uuid(generateUuid()).build();
    assertThat(plan.isEmpty()).isEqualTo(true);
  }

  private ExecutionPlan buildDummyPlan() {
    return ExecutionPlan.builder()
        .node(ExecutionNode.builder().uuid(dummyNode1Id).name("Dummy Node 1").stateType("DUMMY1").build())
        .node(ExecutionNode.builder().uuid(dummyNode2Id).name("Dummy Node 2").stateType("DUMMY1").build())
        .node(ExecutionNode.builder().uuid(dummyNode3Id).name("Dummy Node 3").stateType("DUMMY3").build())
        .startingNodeId(dummyNode1Id)
        .uuid(planId)
        .build();
  }
}