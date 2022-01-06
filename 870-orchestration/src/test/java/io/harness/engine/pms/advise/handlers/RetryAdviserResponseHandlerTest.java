/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.engine.pms.advise.handlers;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.PRASHANT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.OrchestrationTestBase;
import io.harness.category.element.UnitTests;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.executions.plan.PlanExecutionService;
import io.harness.execution.NodeExecution;
import io.harness.execution.PlanExecution;
import io.harness.pms.contracts.advisers.AdviseType;
import io.harness.pms.contracts.advisers.AdviserResponse;
import io.harness.pms.contracts.advisers.RetryAdvise;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.ambiance.Level;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.plan.PlanNodeProto;
import io.harness.pms.contracts.steps.StepCategory;
import io.harness.pms.contracts.steps.StepType;
import io.harness.rule.Owner;
import io.harness.testlib.RealMongo;

import com.google.inject.Inject;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class RetryAdviserResponseHandlerTest extends OrchestrationTestBase {
  @Inject private RetryAdviserResponseHandler retryAdviseHandler;
  @Inject private PlanExecutionService planExecutionService;
  @Inject private NodeExecutionService nodeExecutionService;

  private static final String PLAN_EXECUTION_ID = generateUuid();
  private static final String NODE_EXECUTION_ID = generateUuid();
  private static final String NODE_SETUP_ID = generateUuid();

  private NodeExecution nodeExecution;
  private RetryAdvise advise;

  @Before
  public void setup() {
    Ambiance ambiance = Ambiance.newBuilder()
                            .setPlanExecutionId(PLAN_EXECUTION_ID)
                            .addAllLevels(Collections.singletonList(
                                Level.newBuilder().setRuntimeId(NODE_EXECUTION_ID).setSetupId(NODE_SETUP_ID).build()))
                            .build();

    planExecutionService.save(PlanExecution.builder().uuid(PLAN_EXECUTION_ID).build());

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
    advise = RetryAdvise.newBuilder().setWaitInterval(0).setRetryNodeExecutionId(NODE_EXECUTION_ID).build();
  }

  @Test
  @RealMongo
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestHandleAdvise() {
    retryAdviseHandler.handleAdvise(
        nodeExecution, AdviserResponse.newBuilder().setRetryAdvise(advise).setType(AdviseType.RETRY).build());
    List<NodeExecution> executions = nodeExecutionService.fetchNodeExecutions(PLAN_EXECUTION_ID);
    assertThat(executions).hasSize(2);
    NodeExecution newNodeExecution =
        executions.stream().filter(ex -> !ex.getUuid().equals(NODE_EXECUTION_ID)).findFirst().orElse(null);
    assertThat(newNodeExecution).isNotNull();
    assertThat(newNodeExecution.getRetryIds()).hasSize(1);
    assertThat(newNodeExecution.getRetryIds()).containsExactly(NODE_EXECUTION_ID);
  }
}
