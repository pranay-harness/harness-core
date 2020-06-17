package io.harness.engine.advise.handlers;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.PRASHANT;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.OrchestrationTest;
import io.harness.adviser.advise.RetryAdvise;
import io.harness.ambiance.Ambiance;
import io.harness.ambiance.Level;
import io.harness.beans.EmbeddedUser;
import io.harness.category.element.UnitTests;
import io.harness.engine.services.NodeExecutionService;
import io.harness.engine.services.PlanExecutionService;
import io.harness.execution.NodeExecution;
import io.harness.execution.PlanExecution;
import io.harness.execution.status.Status;
import io.harness.plan.PlanNode;
import io.harness.rule.Owner;
import io.harness.state.StepType;
import io.harness.testlib.RealMongo;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;

import java.util.Collections;
import java.util.List;

public class RetryAdviseHandlerTest extends OrchestrationTest {
  @InjectMocks @Inject private RetryAdviseHandler retryAdviseHandler;
  @Inject private PlanExecutionService planExecutionService;
  @Inject private NodeExecutionService nodeExecutionService;

  private static final String PLAN_EXECUTION_ID = generateUuid();
  private static final String NODE_EXECUTION_ID = generateUuid();
  private static final String NODE_SETUP_ID = generateUuid();

  private static final EmbeddedUser EMBEDDED_USER = new EmbeddedUser(generateUuid(), PRASHANT, PRASHANT);

  private Ambiance ambiance;
  private RetryAdvise advise;

  @Before
  public void setup() {
    ambiance = Ambiance.builder()
                   .planExecutionId(PLAN_EXECUTION_ID)
                   .levels(Collections.singletonList(
                       Level.builder().runtimeId(NODE_EXECUTION_ID).setupId(NODE_SETUP_ID).build()))
                   .build();

    planExecutionService.save(PlanExecution.builder().uuid(PLAN_EXECUTION_ID).createdBy(EMBEDDED_USER).build());

    NodeExecution nodeExecution = NodeExecution.builder()
                                      .uuid(NODE_EXECUTION_ID)
                                      .planExecutionId(ambiance.getPlanExecutionId())
                                      .levels(ambiance.getLevels())
                                      .node(PlanNode.builder()
                                                .uuid(NODE_SETUP_ID)
                                                .name("DUMMY")
                                                .identifier("dummy")
                                                .stepType(StepType.builder().type("DUMMY").build())
                                                .build())
                                      .startTs(System.currentTimeMillis())
                                      .status(Status.FAILED)
                                      .build();
    nodeExecutionService.save(nodeExecution);
    advise = RetryAdvise.builder().waitInterval(0).retryNodeExecutionId(NODE_EXECUTION_ID).build();
  }

  @Test
  @RealMongo
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestHandleAdvise() {
    retryAdviseHandler.handleAdvise(ambiance, advise);
    List<NodeExecution> executions = nodeExecutionService.fetchNodeExecutions(PLAN_EXECUTION_ID);
    assertThat(executions).hasSize(2);
    NodeExecution newNodeExecution =
        executions.stream().filter(ex -> !ex.getUuid().equals(NODE_EXECUTION_ID)).findFirst().orElse(null);
    assertThat(newNodeExecution).isNotNull();
    assertThat(newNodeExecution.getRetryIds()).hasSize(1);
    assertThat(newNodeExecution.getRetryIds()).containsExactly(NODE_EXECUTION_ID);
  }
}