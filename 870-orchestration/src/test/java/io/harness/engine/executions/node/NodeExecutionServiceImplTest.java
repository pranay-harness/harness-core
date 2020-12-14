package io.harness.engine.executions.node;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.PRASHANT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.OrchestrationTestBase;
import io.harness.category.element.UnitTests;
import io.harness.execution.NodeExecution;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.plan.PlanNodeProto;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.plan.PlanNode;
import io.harness.rule.Owner;
import io.harness.testlib.RealMongo;
import io.harness.utils.AmbianceTestUtils;

import com.google.inject.Inject;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class NodeExecutionServiceImplTest extends OrchestrationTestBase {
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
                                      .node(PlanNodeProto.newBuilder()
                                                .setUuid(generateUuid())
                                                .setName("name")
                                                .setIdentifier("dummy")
                                                .setStepType(StepType.newBuilder().setType("DUMMY").build())
                                                .build())
                                      .startTs(System.currentTimeMillis())
                                      .status(Status.QUEUED)
                                      .build();
    NodeExecution savedExecution = nodeExecutionService.save(nodeExecution);
    assertThat(savedExecution.getUuid()).isEqualTo(nodeExecutionId);
  }
}
