/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.engine.pms.execution.strategy.plannode;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.logging.LoggingInitializer.initializeLogging;
import static io.harness.pms.contracts.execution.failure.FailureType.APPLICATION_FAILURE;
import static io.harness.pms.contracts.plan.TriggerType.MANUAL;
import static io.harness.rule.OwnerRule.PRASHANT;
import static io.harness.rule.OwnerRule.SAHIL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.OrchestrationTestBase;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.engine.ExecutionCheck;
import io.harness.engine.ExecutionEngineDispatcher;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.facilitation.facilitator.publisher.FacilitateEventPublisher;
import io.harness.engine.interrupts.InterruptService;
import io.harness.engine.pms.advise.NodeAdviseHelper;
import io.harness.engine.pms.execution.strategy.EndNodeExecutionHelper;
import io.harness.engine.pms.resume.NodeResumeHelper;
import io.harness.engine.pms.start.NodeStartHelper;
import io.harness.exception.InvalidRequestException;
import io.harness.execution.NodeExecution;
import io.harness.execution.NodeExecution.NodeExecutionBuilder;
import io.harness.plan.PlanNode;
import io.harness.pms.contracts.advisers.AdviserObtainment;
import io.harness.pms.contracts.advisers.AdviserType;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.ambiance.Level;
import io.harness.pms.contracts.execution.ExecutionMode;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.facilitators.FacilitatorObtainment;
import io.harness.pms.contracts.facilitators.FacilitatorResponseProto;
import io.harness.pms.contracts.facilitators.FacilitatorType;
import io.harness.pms.contracts.plan.ExecutionTriggerInfo;
import io.harness.pms.contracts.plan.TriggeredBy;
import io.harness.pms.contracts.steps.StepCategory;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.contracts.steps.io.StepResponseProto;
import io.harness.pms.execution.OrchestrationFacilitatorType;
import io.harness.pms.execution.utils.NodeProjectionUtils;
import io.harness.rule.Owner;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.google.protobuf.ByteString;
import java.util.EnumSet;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.transaction.CannotCreateTransactionException;

@OwnedBy(HarnessTeam.PIPELINE)
public class PlanNodeExecutionStrategyTest extends OrchestrationTestBase {
  @Mock @Named("EngineExecutorService") ExecutorService executorService;
  @Mock private NodeExecutionService nodeExecutionService;
  @Mock private FacilitateEventPublisher facilitateEventPublisher;
  @Mock private EndNodeExecutionHelper endNodeExecutionHelper;
  @Mock private NodeResumeHelper resumeHelper;
  @Mock private NodeStartHelper startHelper;
  @Mock private NodeAdviseHelper adviseHelper;
  @Mock private InterruptService interruptService;

  @Inject @InjectMocks @Spy PlanNodeExecutionStrategy executionStrategy;

  private static final StepType TEST_STEP_TYPE =
      StepType.newBuilder().setType("TEST_STEP_PLAN").setStepCategory(StepCategory.STEP).build();

  private static final TriggeredBy triggeredBy =
      TriggeredBy.newBuilder().putExtraInfo("email", PRASHANT).setIdentifier(PRASHANT).setUuid(generateUuid()).build();
  private static final ExecutionTriggerInfo triggerInfo =
      ExecutionTriggerInfo.newBuilder().setTriggerType(MANUAL).setTriggeredBy(triggeredBy).build();

  @Before
  public void setUp() {
    initializeLogging();
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestTriggerNode() {
    String planExecutionId = generateUuid();
    Ambiance ambiance = Ambiance.newBuilder()
                            .setPlanExecutionId(planExecutionId)
                            .putAllSetupAbstractions(prepareInputArgs())
                            .addLevels(Level.newBuilder().setRuntimeId(generateUuid()).build())
                            .build();
    PlanNode planNode =
        PlanNode.builder()
            .name("Test Node")
            .uuid(generateUuid())
            .identifier("test")
            .stepType(TEST_STEP_TYPE)
            .facilitatorObtainment(
                FacilitatorObtainment.newBuilder()
                    .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.SYNC).build())
                    .build())
            .build();
    executionStrategy.triggerNode(ambiance, planNode, null);
    verify(executorService).submit(any(ExecutionEngineDispatcher.class));
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestStartExecutionWithCustomFacilitator() {
    String planExecutionId = generateUuid();
    String nodeExecutionId = generateUuid();
    Ambiance ambiance = Ambiance.newBuilder()
                            .setPlanExecutionId(planExecutionId)
                            .putAllSetupAbstractions(prepareInputArgs())
                            .addLevels(Level.newBuilder().setRuntimeId(nodeExecutionId).build())
                            .build();
    PlanNode planNode = PlanNode.builder()
                            .name("Test Node")
                            .uuid(generateUuid())
                            .identifier("test")
                            .stepType(TEST_STEP_TYPE)
                            .facilitatorObtainment(FacilitatorObtainment.newBuilder()
                                                       .setType(FacilitatorType.newBuilder().setType("CUSTOM").build())
                                                       .build())
                            .build();

    NodeExecution nodeExecution =
        NodeExecution.builder().uuid(nodeExecutionId).ambiance(ambiance).planNode(planNode).build();

    when(nodeExecutionService.get(eq(nodeExecutionId))).thenReturn(nodeExecution);
    when(nodeExecutionService.update(eq(nodeExecutionId), any())).thenReturn(nodeExecution);

    executionStrategy.startExecution(ambiance);
    verify(facilitateEventPublisher).publishEvent(eq(nodeExecutionId));
    verify(executionStrategy, times(0)).processFacilitationResponse(any(), any());
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestStartExecution() {
    String planExecutionId = generateUuid();
    String nodeExecutionId = generateUuid();
    Ambiance ambiance = Ambiance.newBuilder()
                            .setPlanExecutionId(planExecutionId)
                            .putAllSetupAbstractions(prepareInputArgs())
                            .addLevels(Level.newBuilder().setRuntimeId(nodeExecutionId).build())
                            .build();
    PlanNode planNode =
        PlanNode.builder()
            .name("Test Node")
            .uuid(generateUuid())
            .identifier("test")
            .stepType(TEST_STEP_TYPE)
            .facilitatorObtainment(
                FacilitatorObtainment.newBuilder()
                    .setType(FacilitatorType.newBuilder().setType(OrchestrationFacilitatorType.SYNC).build())
                    .build())
            .build();

    NodeExecution nodeExecution =
        NodeExecution.builder().uuid(nodeExecutionId).ambiance(ambiance).planNode(planNode).build();

    when(nodeExecutionService.get(eq(nodeExecutionId))).thenReturn(nodeExecution);
    when(nodeExecutionService.update(eq(nodeExecutionId), any())).thenReturn(nodeExecution);
    doNothing().when(executionStrategy).processFacilitationResponse(any(), any());

    executionStrategy.startExecution(ambiance);
    ArgumentCaptor<Ambiance> ambianceCaptor = ArgumentCaptor.forClass(Ambiance.class);
    ArgumentCaptor<FacilitatorResponseProto> facilitatorResponseCaptor =
        ArgumentCaptor.forClass(FacilitatorResponseProto.class);
    verify(executionStrategy)
        .processFacilitationResponse(ambianceCaptor.capture(), facilitatorResponseCaptor.capture());

    assertThat(ambianceCaptor.getValue().getPlanExecutionId()).isEqualTo(planExecutionId);
    assertThat(facilitatorResponseCaptor.getValue().getExecutionMode()).isEqualTo(ExecutionMode.SYNC);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestResumeNodeExecutionWithStatusRunning() {
    String planExecutionId = generateUuid();
    String nodeExecutionId = generateUuid();
    Ambiance ambiance = Ambiance.newBuilder()
                            .setPlanExecutionId(planExecutionId)
                            .putAllSetupAbstractions(prepareInputArgs())
                            .addLevels(Level.newBuilder().setRuntimeId(nodeExecutionId).build())
                            .build();
    NodeExecution nodeExecution =
        NodeExecution.builder().uuid(nodeExecutionId).ambiance(ambiance).status(Status.RUNNING).build();
    Map<String, ByteString> responseMap = ImmutableMap.of(generateUuid(), ByteString.copyFromUtf8(generateUuid()));
    when(nodeExecutionService.getWithFieldsIncluded(eq(nodeExecutionId), eq(NodeProjectionUtils.fieldsForResume)))
        .thenReturn(nodeExecution);
    executionStrategy.resumeNodeExecution(ambiance, responseMap, false);
    verify(resumeHelper).resume(eq(nodeExecution), eq(responseMap), eq(false));
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestResumeNodeExecutionWithStatusAborted() {
    String planExecutionId = generateUuid();
    String nodeExecutionId = generateUuid();
    Ambiance ambiance = Ambiance.newBuilder()
                            .setPlanExecutionId(planExecutionId)
                            .putAllSetupAbstractions(prepareInputArgs())
                            .addLevels(Level.newBuilder().setRuntimeId(nodeExecutionId).build())
                            .build();
    NodeExecution nodeExecution =
        NodeExecution.builder().uuid(nodeExecutionId).ambiance(ambiance).status(Status.ABORTED).build();
    Map<String, ByteString> responseMap = ImmutableMap.of(generateUuid(), ByteString.copyFromUtf8(generateUuid()));
    when(nodeExecutionService.get(eq(nodeExecutionId))).thenReturn(nodeExecution);
    executionStrategy.resumeNodeExecution(ambiance, responseMap, false);
    verify(resumeHelper, times(0)).resume(eq(nodeExecution), eq(responseMap), eq(false));
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestProcessFacilitatorResponse() {
    String planExecutionId = generateUuid();
    String nodeExecutionId = generateUuid();
    Ambiance ambiance = Ambiance.newBuilder()
                            .setPlanExecutionId(planExecutionId)
                            .putAllSetupAbstractions(prepareInputArgs())
                            .addLevels(Level.newBuilder().setRuntimeId(nodeExecutionId).build())
                            .build();

    when(nodeExecutionService.update(eq(nodeExecutionId), any()))
        .thenReturn(NodeExecution.builder()
                        .uuid(nodeExecutionId)
                        .ambiance(ambiance)
                        .status(Status.QUEUED)
                        .mode(ExecutionMode.ASYNC)
                        .build());
    when(interruptService.checkInterruptsPreInvocation(eq(planExecutionId), eq(nodeExecutionId)))
        .thenReturn(ExecutionCheck.builder().proceed(true).build());

    FacilitatorResponseProto facilitatorResponse =
        FacilitatorResponseProto.newBuilder().setExecutionMode(ExecutionMode.ASYNC).build();
    executionStrategy.processFacilitationResponse(ambiance, facilitatorResponse);
    verify(startHelper).startNode(eq(ambiance), eq(facilitatorResponse));
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestProcessFacilitatorResponseWithInterrupt() {
    String planExecutionId = generateUuid();
    String nodeExecutionId = generateUuid();
    Ambiance ambiance = Ambiance.newBuilder()
                            .setPlanExecutionId(planExecutionId)
                            .putAllSetupAbstractions(prepareInputArgs())
                            .addLevels(Level.newBuilder().setRuntimeId(nodeExecutionId).build())
                            .build();

    when(nodeExecutionService.update(eq(nodeExecutionId), any()))
        .thenReturn(NodeExecution.builder()
                        .uuid(nodeExecutionId)
                        .ambiance(ambiance)
                        .status(Status.QUEUED)
                        .mode(ExecutionMode.ASYNC)
                        .build());
    when(interruptService.checkInterruptsPreInvocation(eq(planExecutionId), eq(nodeExecutionId)))
        .thenReturn(ExecutionCheck.builder().proceed(false).build());

    FacilitatorResponseProto facilitatorResponse =
        FacilitatorResponseProto.newBuilder().setExecutionMode(ExecutionMode.ASYNC).build();
    executionStrategy.processFacilitationResponse(ambiance, facilitatorResponse);
    verify(startHelper, times(0)).startNode(eq(ambiance), eq(facilitatorResponse));
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestConcludeNodeExecutionNoAdvisers() {
    String planExecutionId = generateUuid();
    String nodeExecutionId = generateUuid();
    Ambiance ambiance = Ambiance.newBuilder()
                            .setPlanExecutionId(planExecutionId)
                            .putAllSetupAbstractions(prepareInputArgs())
                            .addLevels(Level.newBuilder().setRuntimeId(nodeExecutionId).build())
                            .build();
    PlanNode planNode =
        PlanNode.builder().name("Test Node").uuid(generateUuid()).identifier("test").stepType(TEST_STEP_TYPE).build();
    NodeExecutionBuilder nodeExecutionBuilder = NodeExecution.builder()
                                                    .uuid(nodeExecutionId)
                                                    .planNode(planNode)
                                                    .ambiance(ambiance)
                                                    .status(Status.INTERVENTION_WAITING)
                                                    .mode(ExecutionMode.ASYNC);

    when(nodeExecutionService.getWithFieldsIncluded(nodeExecutionId, NodeProjectionUtils.withStatusAndNode))
        .thenReturn(nodeExecutionBuilder.build());
    when(nodeExecutionService.updateStatusWithOps(eq(nodeExecutionId), any(), any(), any()))
        .thenReturn(nodeExecutionBuilder.status(Status.FAILED).build());
    doNothing().when(executionStrategy).endNodeExecution(ambiance);

    executionStrategy.concludeExecution(ambiance, Status.FAILED, EnumSet.noneOf(Status.class));
    verify(executionStrategy).endNodeExecution(eq(ambiance));
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestConcludeNodeExecutionWithAdvisers() {
    String planExecutionId = generateUuid();
    String nodeExecutionId = generateUuid();
    Ambiance ambiance = Ambiance.newBuilder()
                            .setPlanExecutionId(planExecutionId)
                            .putAllSetupAbstractions(prepareInputArgs())
                            .addLevels(Level.newBuilder().setRuntimeId(nodeExecutionId).build())
                            .build();
    PlanNode planNode =
        PlanNode.builder()
            .name("Test Node")
            .uuid(generateUuid())
            .identifier("test")
            .stepType(TEST_STEP_TYPE)
            .adviserObtainment(
                AdviserObtainment.newBuilder().setType(AdviserType.newBuilder().setType("NEXT_STEP").build()).build())
            .build();

    NodeExecutionBuilder nodeExecutionBuilder = NodeExecution.builder()
                                                    .uuid(nodeExecutionId)
                                                    .planNode(planNode)
                                                    .ambiance(ambiance)
                                                    .status(Status.INTERVENTION_WAITING)
                                                    .mode(ExecutionMode.ASYNC);

    when(nodeExecutionService.getWithFieldsIncluded(nodeExecutionId, NodeProjectionUtils.withStatusAndNode))
        .thenReturn(nodeExecutionBuilder.build());
    NodeExecution updated = nodeExecutionBuilder.status(Status.FAILED).endTs(System.currentTimeMillis()).build();
    when(nodeExecutionService.updateStatusWithOps(eq(nodeExecutionId), eq(Status.FAILED), any(), any()))
        .thenReturn(updated);

    executionStrategy.concludeExecution(ambiance, Status.FAILED, EnumSet.noneOf(Status.class));
    verify(adviseHelper).queueAdvisingEvent(eq(updated), eq(Status.INTERVENTION_WAITING));
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestHandleErrorWithExceptionManager() {
    ArgumentCaptor<NodeExecution> nExCaptor = ArgumentCaptor.forClass(NodeExecution.class);
    ArgumentCaptor<StepResponseProto> sCaptor = ArgumentCaptor.forClass(StepResponseProto.class);
    NodeExecution nodeExecution = NodeExecution.builder().uuid(generateUuid()).build();
    when(nodeExecutionService.get(nodeExecution.getUuid())).thenReturn(nodeExecution);
    String planExecutionId = generateUuid();
    Ambiance ambiance = Ambiance.newBuilder()
                            .setPlanExecutionId(planExecutionId)
                            .putAllSetupAbstractions(prepareInputArgs())
                            .addLevels(Level.newBuilder().setRuntimeId(nodeExecution.getUuid()).build())
                            .build();
    CannotCreateTransactionException ex = new CannotCreateTransactionException("Cannot Create Transaction");
    executionStrategy.handleError(ambiance, ex);
    verify(executionStrategy).handleStepResponseInternal(nExCaptor.capture(), sCaptor.capture());
    assertThat(nExCaptor.getValue().getUuid()).isEqualTo(nodeExecution.getUuid());
    assertThat(sCaptor.getValue().getFailureInfo()).isNotNull();
    assertThat(sCaptor.getValue().getFailureInfo().getErrorMessage()).isEqualTo("Cannot Create Transaction");
    assertThat(sCaptor.getValue().getFailureInfo().getFailureTypesList().get(0)).isEqualTo(APPLICATION_FAILURE);
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void handleStepResponseWithError() {
    String nodeExecutionId = generateUuid();
    StepResponseProto stepResponseProto = StepResponseProto.newBuilder().build();
    Ambiance ambiance = Ambiance.newBuilder()
                            .setPlanExecutionId("planExecutionId")
                            .putAllSetupAbstractions(prepareInputArgs())
                            .addLevels(Level.newBuilder().setRuntimeId(nodeExecutionId).build())
                            .build();
    NodeExecution nodeExecution = NodeExecution.builder().uuid(nodeExecutionId).ambiance(ambiance).build();
    when(nodeExecutionService.get(nodeExecution.getUuid())).thenReturn(nodeExecution);
    doThrow(new InvalidRequestException("test"))
        .when(endNodeExecutionHelper)
        .endNodeExecutionWithNoAdvisers(nodeExecution, stepResponseProto);
    doNothing().when(executionStrategy).handleError(any(), any());
    executionStrategy.processStepResponse(ambiance, stepResponseProto);
    verify(executionStrategy).handleError(any(), any());
  }

  private static Map<String, String> prepareInputArgs() {
    return ImmutableMap.of("accountId", "kmpySmUISimoRrJL6NL73w", "appId", "XEsfW6D_RJm1IaGpDidD3g", "userId",
        triggeredBy.getUuid(), "userName", triggeredBy.getIdentifier(), "userEmail",
        triggeredBy.getExtraInfoOrThrow("email"));
  }
}
