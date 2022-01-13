/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.engine.pms.advise.handlers;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.ARCHIT;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.harness.OrchestrationTestBase;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.executions.plan.PlanExecutionService;
import io.harness.engine.interrupts.InterruptManager;
import io.harness.engine.interrupts.InterruptPackage;
import io.harness.execution.NodeExecution;
import io.harness.execution.PlanExecution;
import io.harness.pms.contracts.advisers.AdviseType;
import io.harness.pms.contracts.advisers.AdviserResponse;
import io.harness.pms.contracts.advisers.EndPlanAdvise;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.ambiance.Level;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.interrupts.AdviserIssuer;
import io.harness.pms.contracts.interrupts.InterruptConfig;
import io.harness.pms.contracts.interrupts.InterruptType;
import io.harness.pms.contracts.interrupts.IssuedBy;
import io.harness.pms.contracts.plan.PlanNodeProto;
import io.harness.pms.contracts.steps.StepCategory;
import io.harness.pms.contracts.steps.StepType;
import io.harness.rule.Owner;
import io.harness.serializer.ProtoUtils;

import com.google.inject.Inject;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@OwnedBy(PIPELINE)
public class EndPlanAdviserResponseHandlerTest extends OrchestrationTestBase {
  @Mock private InterruptManager interruptManager;
  @InjectMocks @Inject private EndPlanAdviserResponseHandler endPlanAdviserResponseHandler;

  @Inject private PlanExecutionService planExecutionService;
  @Inject private NodeExecutionService nodeExecutionService;

  private static final String PLAN_EXECUTION_ID = generateUuid();
  private static final String NODE_EXECUTION_ID = generateUuid();
  private static final String NODE_SETUP_ID = generateUuid();

  private NodeExecution nodeExecution;
  private EndPlanAdvise advise;

  @Before
  public void setup() {
    Ambiance ambiance = Ambiance.newBuilder()
                            .setPlanExecutionId(PLAN_EXECUTION_ID)
                            .addAllLevels(Collections.singletonList(
                                Level.newBuilder().setRuntimeId(NODE_EXECUTION_ID).setSetupId(NODE_SETUP_ID).build()))
                            .build();

    planExecutionService.save(PlanExecution.builder().uuid(PLAN_EXECUTION_ID).status(Status.RUNNING).build());

    nodeExecution =
        NodeExecution.builder()
            .uuid(NODE_EXECUTION_ID)
            .ambiance(ambiance)
            .node(PlanNodeProto.newBuilder()
                      .setUuid(NODE_SETUP_ID)
                      .setName("DUMMY")
                      .setIdentifier("dummy")
                      .setStepType(StepType.newBuilder().setType("DUMMY").setStepCategory(StepCategory.STEP).build())
                      .build())
            .startTs(System.currentTimeMillis())
            .status(Status.FAILED)
            .build();
    nodeExecutionService.save(nodeExecution);
    advise = EndPlanAdvise.newBuilder().build();
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void shouldTestHandleAdviseWithEndTransition() {
    endPlanAdviserResponseHandler.handleAdvise(
        nodeExecution, AdviserResponse.newBuilder().setEndPlanAdvise(advise).setType(AdviseType.END_PLAN).build());
    PlanExecution planExecution = planExecutionService.get(PLAN_EXECUTION_ID);
    assertThat(planExecution.getStatus()).isEqualTo(Status.FAILED);
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void shouldTestHandlerAdviseWithAbort() {
    advise = EndPlanAdvise.newBuilder().setIsAbort(true).build();
    endPlanAdviserResponseHandler.handleAdvise(
        nodeExecution, AdviserResponse.newBuilder().setEndPlanAdvise(advise).setType(AdviseType.END_PLAN).build());
    verify(interruptManager, times(1))
        .register(
            InterruptPackage.builder()
                .planExecutionId(PLAN_EXECUTION_ID)
                .interruptType(InterruptType.ABORT_ALL)
                .nodeExecutionId(nodeExecution.getUuid())
                .interruptConfig(
                    InterruptConfig.newBuilder()
                        .setIssuedBy(IssuedBy.newBuilder()
                                         .setAdviserIssuer(
                                             AdviserIssuer.newBuilder().setAdviserType(AdviseType.END_PLAN).build())
                                         .setIssueTime(ProtoUtils.unixMillisToTimestamp(anyLong()))
                                         .build())
                        .build())
                .build());
  }
}
