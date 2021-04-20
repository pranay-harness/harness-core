package io.harness.event.handlers;

import static io.harness.rule.OwnerRule.SAHIL;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import io.harness.OrchestrationTestBase;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.engine.ExecutionEngineDispatcher;
import io.harness.engine.OrchestrationEngine;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.execution.NodeExecution;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.ExecutionMode;
import io.harness.pms.contracts.execution.NodeExecutionProto;
import io.harness.pms.contracts.execution.events.QueueNodeExecutionRequest;
import io.harness.pms.contracts.execution.events.SdkResponseEventRequest;
import io.harness.pms.contracts.execution.events.SdkResponseEventType;
import io.harness.pms.contracts.plan.PlanNodeProto;
import io.harness.pms.execution.SdkResponseEventInternal;
import io.harness.rule.Owner;

import java.util.concurrent.ExecutorService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@OwnedBy(HarnessTeam.PIPELINE)
public class QueueNodeExecutionEventHandlerTest extends OrchestrationTestBase {
  @Mock private NodeExecutionService nodeExecutionService;
  @Mock private OrchestrationEngine engine;
  @Mock private ExecutorService executorService;
  @InjectMocks QueueNodeExecutionEventHandler queueNodeExecutionEventHandler;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @After
  public void verifyInteractions() {
    verifyNoMoreInteractions(engine);
    verifyNoMoreInteractions(executorService);
    verifyNoMoreInteractions(nodeExecutionService);
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testHandleEvent() {
    NodeExecutionProto nodeExecutionProto = NodeExecutionProto.newBuilder()
                                                .setNode(PlanNodeProto.newBuilder().setUuid("uuid").build())
                                                .setAmbiance(Ambiance.newBuilder().build())
                                                .setMode(ExecutionMode.UNKNOWN)
                                                .build();
    QueueNodeExecutionRequest request =
        QueueNodeExecutionRequest.newBuilder().setNodeExecution(nodeExecutionProto).build();
    queueNodeExecutionEventHandler.handleEvent(
        SdkResponseEventInternal.builder()
            .sdkResponseEventRequest(SdkResponseEventRequest.newBuilder().setQueueNodeExecutionRequest(request).build())
            .sdkResponseEventType(SdkResponseEventType.QUEUE_NODE)
            .build());

    verify(nodeExecutionService).save(any(NodeExecution.class));
    verify(executorService)
        .submit(ExecutionEngineDispatcher.builder()
                    .ambiance(Ambiance.newBuilder().build())
                    .orchestrationEngine(engine)
                    .build());
  }
}