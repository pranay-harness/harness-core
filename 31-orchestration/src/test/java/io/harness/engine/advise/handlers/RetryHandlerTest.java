package io.harness.engine.advise.handlers;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.PRASHANT;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import io.harness.OrchestrationTest;
import io.harness.adviser.impl.retry.RetryAdvise;
import io.harness.ambiance.Ambiance;
import io.harness.ambiance.Level;
import io.harness.category.element.UnitTests;
import io.harness.execution.NodeExecution;
import io.harness.execution.NodeExecution.NodeExecutionKeys;
import io.harness.execution.status.NodeExecutionStatus;
import io.harness.persistence.HPersistence;
import io.harness.plan.ExecutionNode;
import io.harness.rule.Owner;
import io.harness.state.StepType;
import io.harness.testlib.RealMongo;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.Collections;

public class RetryHandlerTest extends OrchestrationTest {
  @Inject RetryHandler retryHandler;
  @Inject @Named("enginePersistence") private HPersistence hPersistence;

  private static final String PLAN_EXECUTION_ID = generateUuid();
  private static final String NODE_EXECUTION_ID = generateUuid();
  private static final String NODE_SETUP_ID = generateUuid();

  private Ambiance ambiance;
  private RetryAdvise advise;
  private NodeExecution nodeExecution;

  @Before
  public void setup() {
    ambiance = Ambiance.builder()
                   .planExecutionId(PLAN_EXECUTION_ID)
                   .levels(Collections.singletonList(
                       Level.builder().runtimeId(NODE_EXECUTION_ID).setupId(NODE_SETUP_ID).build()))
                   .build();

    NodeExecution nodeExecution = NodeExecution.builder()
                                      .uuid(NODE_EXECUTION_ID)
                                      .ambiance(ambiance)
                                      .node(ExecutionNode.builder()
                                                .uuid(NODE_SETUP_ID)
                                                .name("DUMMY")
                                                .identifier("dummy")
                                                .stepType(StepType.builder().type("DUMMY").build())
                                                .build())
                                      .startTs(System.currentTimeMillis())
                                      .status(NodeExecutionStatus.FAILED)
                                      .build();
    hPersistence.save(nodeExecution);
    advise = RetryAdvise.builder().waitInterval(0).retryNodeExecutionId(NODE_EXECUTION_ID).build();
  }

  @Test
  @RealMongo
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestHandleAdvise() {
    retryHandler.handleAdvise(ambiance, advise);
    NodeExecution newNodeExecution =
        hPersistence.createQuery(NodeExecution.class).filter(NodeExecutionKeys.uuid, NODE_EXECUTION_ID).get();
    assertThat(newNodeExecution).isNotNull();
    assertThat(newNodeExecution.getRetryIds()).hasSize(1);
    assertThat(newNodeExecution.getRetryIds()).doesNotContain(NODE_EXECUTION_ID);
  }
}