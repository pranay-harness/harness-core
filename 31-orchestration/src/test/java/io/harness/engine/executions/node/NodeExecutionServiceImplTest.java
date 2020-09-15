package io.harness.engine.executions.node;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.PRASHANT;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.OrchestrationModuleListProvider;
import io.harness.OrchestrationTest;
import io.harness.category.element.UnitTests;
import io.harness.execution.NodeExecution;
import io.harness.execution.status.Status;
import io.harness.plan.PlanNode;
import io.harness.rule.Owner;
import io.harness.runners.GuiceRunner;
import io.harness.runners.ModuleProvider;
import io.harness.state.StepType;
import io.harness.utils.AmbianceTestUtils;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

@RunWith(GuiceRunner.class)
@ModuleProvider(OrchestrationModuleListProvider.class)
public class NodeExecutionServiceImplTest extends OrchestrationTest {
  @Inject private NodeExecutionService nodeExecutionService;

  @Test
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
                                                .stepType(StepType.builder().type("DUMMY").build())
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