package io.harness.cdng.pipeline.executions;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.pms.contracts.execution.events.OrchestrationEventType.NODE_EXECUTION_STATUS_UPDATE;
import static io.harness.rule.OwnerRule.SAHIL;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.cdng.environment.EnvironmentOutcome;
import io.harness.cdng.infra.steps.InfrastructureStep;
import io.harness.cdng.pipeline.executions.service.NgPipelineExecutionService;
import io.harness.cdng.service.beans.ServiceOutcome;
import io.harness.cdng.service.steps.ServiceSpecStep;
import io.harness.engine.executions.node.NodeExecutionServiceImpl;
import io.harness.execution.NodeExecution;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.ambiance.Level;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.plan.PlanNodeProto;
import io.harness.pms.sdk.core.events.OrchestrationEvent;
import io.harness.pms.sdk.core.resolver.outcome.OutcomeService;
import io.harness.rule.Owner;
import io.harness.steps.StepOutcomeGroup;

import io.fabric8.utils.Lists;
import org.apache.groovy.util.Maps;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@OwnedBy(HarnessTeam.PIPELINE)
public class PipelineExecutionUpdateEventHandlerTest extends CategoryTest {
  @Mock private NgPipelineExecutionService ngPipelineExecutionService;
  @Mock private NodeExecutionServiceImpl nodeExecutionService;
  @Mock private OutcomeService outcomeService;

  @InjectMocks private PipelineExecutionUpdateEventHandler pipelineExecutionUpdateEventHandler;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testHandleEvent() {
    Ambiance ambiance = Ambiance.newBuilder()
                            .putAllSetupAbstractions(Maps.of("accountId", "accountId", "projectIdentifier",
                                "projectIdentfier", "orgIdentifier", "orgIdentifier"))
                            .addAllLevels(Lists.newArrayList(Level.newBuilder().setRuntimeId("node1").build()))
                            .setPlanExecutionId(generateUuid())
                            .build();
    OrchestrationEvent orchestrationEvent =
        OrchestrationEvent.builder().ambiance(ambiance).eventType(NODE_EXECUTION_STATUS_UPDATE).build();
    NodeExecution nodeExecution = NodeExecution.builder()
                                      .ambiance(ambiance)
                                      .node(PlanNodeProto.newBuilder().setGroup(StepOutcomeGroup.STAGE.name()).build())
                                      .build();
    when(nodeExecutionService.get("node1")).thenReturn(nodeExecution);
    pipelineExecutionUpdateEventHandler.handleEvent(orchestrationEvent);

    verify(nodeExecutionService).get("node1");
    verify(ngPipelineExecutionService)
        .updateStatusForGivenNode(
            "accountId", "orgIdentifier", "projectIdentfier", ambiance.getPlanExecutionId(), nodeExecution);
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testHandleEventNullGroup() {
    OrchestrationEvent orchestrationEvent =
        OrchestrationEvent.builder()
            .ambiance(Ambiance.newBuilder()
                          .putAllSetupAbstractions(Maps.of("accountId", "accountId", "projectIdentifier",
                              "projectIdentfier", "orgIdentifier", "orgIdentifier"))
                          .addAllLevels(Lists.newArrayList(Level.newBuilder().setRuntimeId("node1").build()))
                          .build())
            .eventType(NODE_EXECUTION_STATUS_UPDATE)
            .build();
    NodeExecution nodeExecution = NodeExecution.builder().node(PlanNodeProto.newBuilder().build()).build();
    when(nodeExecutionService.get("node1")).thenReturn(nodeExecution);
    pipelineExecutionUpdateEventHandler.handleEvent(orchestrationEvent);

    verify(nodeExecutionService).get("node1");
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testHandleEventService() {
    OrchestrationEvent orchestrationEvent =
        OrchestrationEvent.builder()
            .ambiance(Ambiance.newBuilder()
                          .putAllSetupAbstractions(Maps.of("accountId", "accountId", "projectIdentifier",
                              "projectIdentfier", "orgIdentifier", "orgIdentifier"))
                          .addAllLevels(Lists.newArrayList(Level.newBuilder().setRuntimeId("node1").build()))
                          .build())
            .eventType(NODE_EXECUTION_STATUS_UPDATE)
            .build();
    NodeExecution nodeExecution = NodeExecution.builder()
                                      .node(PlanNodeProto.newBuilder().setStepType(ServiceSpecStep.STEP_TYPE).build())
                                      .status(Status.SUCCEEDED)
                                      .build();
    when(nodeExecutionService.get("node1")).thenReturn(nodeExecution);
    when(outcomeService.findAllByRuntimeId(any(), anyString()))
        .thenReturn(Lists.newArrayList(ServiceOutcome.builder().build()));
    pipelineExecutionUpdateEventHandler.handleEvent(orchestrationEvent);

    verify(nodeExecutionService).get("node1");
    verify(ngPipelineExecutionService)
        .addServiceInformationToPipelineExecutionNode(
            eq("accountId"), eq("orgIdentifier"), eq("projectIdentfier"), any(), any(), any());
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testHandleEventEnvironment() {
    OrchestrationEvent orchestrationEvent =
        OrchestrationEvent.builder()
            .ambiance(Ambiance.newBuilder()
                          .putAllSetupAbstractions(Maps.of("accountId", "accountId", "projectIdentifier",
                              "projectIdentfier", "orgIdentifier", "orgIdentifier"))
                          .addAllLevels(Lists.newArrayList(Level.newBuilder().setRuntimeId("node1").build()))
                          .build())
            .eventType(NODE_EXECUTION_STATUS_UPDATE)
            .build();
    NodeExecution nodeExecution =
        NodeExecution.builder()
            .node(PlanNodeProto.newBuilder().setStepType(InfrastructureStep.STEP_TYPE).build())
            .status(Status.SUCCEEDED)
            .build();
    when(nodeExecutionService.get("node1")).thenReturn(nodeExecution);
    when(outcomeService.findAllByRuntimeId(any(), anyString()))
        .thenReturn(Lists.newArrayList(EnvironmentOutcome.builder().build()));
    pipelineExecutionUpdateEventHandler.handleEvent(orchestrationEvent);

    verify(nodeExecutionService).get("node1");
    verify(ngPipelineExecutionService)
        .addEnvironmentInformationToPipelineExecutionNode(
            eq("accountId"), eq("orgIdentifier"), eq("projectIdentfier"), any(), any(), any());
  }
}
