package io.harness.cdng.pipeline.executions;

import static io.harness.pms.contracts.execution.events.OrchestrationEventType.ORCHESTRATION_START;
import static io.harness.rule.OwnerRule.SAHIL;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.cdng.pipeline.beans.CDPipelineSetupParameters;
import io.harness.cdng.pipeline.executions.service.NgPipelineExecutionService;
import io.harness.cdng.pipeline.steps.PipelineSetupStep;
import io.harness.engine.executions.node.NodeExecutionServiceImpl;
import io.harness.engine.executions.plan.PlanExecutionService;
import io.harness.execution.PlanExecution;
import io.harness.ngpipeline.pipeline.beans.yaml.NgPipeline;
import io.harness.plan.Plan;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.ambiance.Level;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.sdk.core.events.OrchestrationEvent;
import io.harness.pms.sdk.core.plan.PlanNode;
import io.harness.pms.sdk.core.plan.creation.yaml.StepOutcomeGroup;
import io.harness.rule.Owner;

import io.fabric8.utils.Lists;
import org.apache.groovy.util.Maps;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class PipelineExecutionStartEventHandlerTest extends CategoryTest {
  @Mock private NgPipelineExecutionService ngPipelineExecutionService;
  @Mock private NodeExecutionServiceImpl nodeExecutionService;
  @Mock private PlanExecutionService planExecutionService;

  @InjectMocks private PipelineExecutionStartEventHandler pipelineExecutionStartEventHandler;

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
            .ambiance(Ambiance.newBuilder()
                          .putAllSetupAbstractions(Maps.of("accountId", "accountId", "projectIdentifier",
                              "projectIdentfier", "orgIdentifier", "orgIdentifier"))
                          .addAllLevels(Lists.newArrayList(Level.newBuilder().setRuntimeId("node1").build()))
                          .setPlanExecutionId("executionId")
                          .build())
            .eventType(ORCHESTRATION_START)
            .build();
    PlanNode planNode =
        PlanNode.builder()
            .group(StepOutcomeGroup.PIPELINE.name())
            .uuid("uuid")
            .stepType(PipelineSetupStep.STEP_TYPE)
            .stepParameters(CDPipelineSetupParameters.builder().ngPipeline(NgPipeline.builder().build()).build())
            .build();
    PlanExecution planExecution =
        PlanExecution.builder().plan(Plan.builder().startingNodeId("uuid").node(planNode).internalBuild()).build();
    when(planExecutionService.get("executionId")).thenReturn(planExecution);
    pipelineExecutionStartEventHandler.handleEvent(orchestrationEvent);

    verify(planExecutionService).get("executionId");
    verify(ngPipelineExecutionService)
        .createPipelineExecutionSummary(
            anyString(), anyString(), anyString(), any(PlanExecution.class), any(CDPipelineSetupParameters.class));
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testHandleEventStagesGroup() {
    OrchestrationEvent orchestrationEvent =
        OrchestrationEvent.builder()
            .ambiance(Ambiance.newBuilder()
                          .putAllSetupAbstractions(Maps.of("accountId", "accountId", "projectIdentifier",
                              "projectIdentfier", "orgIdentifier", "orgIdentifier"))
                          .addAllLevels(Lists.newArrayList(Level.newBuilder().setRuntimeId("node1").build()))
                          .setPlanExecutionId("executionId")
                          .build())
            .eventType(ORCHESTRATION_START)
            .build();
    PlanNode planNode =
        PlanNode.builder()
            .group(StepOutcomeGroup.STAGES.name())
            .uuid("uuid")
            .uuid("uuid")
            .stepType(PipelineSetupStep.STEP_TYPE)
            .stepParameters(CDPipelineSetupParameters.builder().ngPipeline(NgPipeline.builder().build()).build())
            .build();
    PlanExecution planExecution =
        PlanExecution.builder().plan(Plan.builder().startingNodeId("uuid").node(planNode).internalBuild()).build();
    when(planExecutionService.get("executionId")).thenReturn(planExecution);
    pipelineExecutionStartEventHandler.handleEvent(orchestrationEvent);

    verify(planExecutionService).get("executionId");
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
                          .setPlanExecutionId("executionId")
                          .build())
            .eventType(ORCHESTRATION_START)
            .build();
    PlanNode planNode =
        PlanNode.builder()
            .uuid("uuid")
            .stepType(StepType.newBuilder().setType("TEST").build())
            .stepParameters(CDPipelineSetupParameters.builder().ngPipeline(NgPipeline.builder().build()).build())
            .build();
    PlanExecution planExecution =
        PlanExecution.builder().plan(Plan.builder().startingNodeId("uuid").node(planNode).build()).build();
    when(planExecutionService.get("executionId")).thenReturn(planExecution);
    pipelineExecutionStartEventHandler.handleEvent(orchestrationEvent);

    verify(planExecutionService).get("executionId");
  }
}
