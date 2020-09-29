package io.harness.cdng.pipeline.executions;

import static io.harness.rule.OwnerRule.SAHIL;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.fabric8.utils.Lists;
import io.harness.CategoryTest;
import io.harness.ambiance.Ambiance;
import io.harness.ambiance.Level;
import io.harness.category.element.UnitTests;
import io.harness.cdng.pipeline.executions.service.NgPipelineExecutionService;
import io.harness.engine.executions.node.NodeExecutionServiceImpl;
import io.harness.execution.NodeExecution;
import io.harness.execution.events.OrchestrationEvent;
import io.harness.executionplan.plancreator.beans.StepOutcomeGroup;
import io.harness.plan.PlanNode;
import io.harness.rule.Owner;
import org.apache.groovy.util.Maps;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class PipelineExecutionUpdateEventHandlerTest extends CategoryTest {
  @Mock private NgPipelineExecutionService ngPipelineExecutionService;
  @Mock private NodeExecutionServiceImpl nodeExecutionService;

  @InjectMocks private PipelineExecutionUpdateEventHandler pipelineExecutionUpdateEventHandler;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testHandleEvent() {
    OrchestrationEvent orchestrationEvent =
        OrchestrationEvent.builder()
            .ambiance(Ambiance.builder()
                          .setupAbstractions(Maps.of("accountId", "accountId", "projectIdentifier", "projectIdentfier",
                              "orgIdentifier", "orgIdentifier"))
                          .levels(Lists.newArrayList(Level.builder().runtimeId("node1").build()))
                          .build())
            .build();
    NodeExecution nodeExecution =
        NodeExecution.builder().node(PlanNode.builder().group(StepOutcomeGroup.STAGE.name()).build()).build();
    when(nodeExecutionService.get("node1")).thenReturn(nodeExecution);
    pipelineExecutionUpdateEventHandler.handleEvent(orchestrationEvent);

    verify(nodeExecutionService).get("node1");
    verify(ngPipelineExecutionService)
        .updateStatusForGivenNode("accountId", "orgIdentifier", "projectIdentfier", null, nodeExecution);
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testHandleEventNullGroup() {
    OrchestrationEvent orchestrationEvent =
        OrchestrationEvent.builder()
            .ambiance(Ambiance.builder()
                          .setupAbstractions(Maps.of("accountId", "accountId", "projectIdentifier", "projectIdentfier",
                              "orgIdentifier", "orgIdentifier"))
                          .levels(Lists.newArrayList(Level.builder().runtimeId("node1").build()))
                          .build())
            .build();
    NodeExecution nodeExecution = NodeExecution.builder().node(PlanNode.builder().group(null).build()).build();
    when(nodeExecutionService.get("node1")).thenReturn(nodeExecution);
    pipelineExecutionUpdateEventHandler.handleEvent(orchestrationEvent);

    verify(nodeExecutionService).get("node1");
  }
}