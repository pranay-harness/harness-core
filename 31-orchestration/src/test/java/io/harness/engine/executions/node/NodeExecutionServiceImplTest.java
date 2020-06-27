package io.harness.engine.executions.node;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.PRASHANT;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.OrchestrationTest;
import io.harness.category.element.UnitTests;
import io.harness.execution.NodeExecution;
import io.harness.execution.status.Status;
import io.harness.plan.PlanNode;
import io.harness.rule.Owner;
import io.harness.state.core.dummy.DummyStep;
import io.harness.testlib.RealMongo;
import io.harness.utils.AmbianceTestUtils;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class NodeExecutionServiceImplTest extends OrchestrationTest {
  @Inject private NodeExecutionService nodeExecutionService;

  @Test
  @RealMongo
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestSave() {
    String nodeExecutionId = generateUuid();
    NodeExecution nodeExecution = NodeExecution.builder()
                                      .uuid(nodeExecutionId)
                                      .ambiance(AmbianceTestUtils.buildAmbiance())
                                      .node(PlanNode.builder()
                                                .uuid(generateUuid())
                                                .name("name")
                                                .identifier("dummy")
                                                .stepType(DummyStep.STEP_TYPE)
                                                .build())
                                      .startTs(System.currentTimeMillis())
                                      .status(Status.QUEUED)
                                      .build();
    NodeExecution savedExecution = nodeExecutionService.save(nodeExecution);
    assertThat(savedExecution.getUuid()).isEqualTo(nodeExecutionId);
    assertThat(savedExecution.getCreatedAt()).isNotNull();
    assertThat(savedExecution.getVersion()).isEqualTo(0);
  }
}