/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.engine.interrupts.helpers;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.PRASHANT;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;

import io.harness.OrchestrationTestBase;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.engine.ExecutionEngineDispatcher;
import io.harness.engine.OrchestrationEngine;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.execution.NodeExecution;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.ambiance.Level;
import io.harness.pms.contracts.execution.ExecutableResponse;
import io.harness.pms.contracts.execution.ExecutionMode;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.contracts.execution.TaskExecutableResponse;
import io.harness.pms.contracts.execution.tasks.TaskCategory;
import io.harness.pms.contracts.interrupts.InterruptConfig;
import io.harness.pms.contracts.interrupts.InterruptType;
import io.harness.pms.contracts.interrupts.IssuedBy;
import io.harness.pms.contracts.interrupts.ManualIssuer;
import io.harness.pms.contracts.plan.PlanNodeProto;
import io.harness.pms.contracts.steps.StepCategory;
import io.harness.pms.contracts.steps.StepType;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

@OwnedBy(HarnessTeam.PIPELINE)
public class RetryHelperTest extends OrchestrationTestBase {
  @Mock OrchestrationEngine engine;
  @Inject NodeExecutionService nodeExecutionService;
  @Mock ExecutorService executorService;
  @Inject @InjectMocks RetryHelper retryHelper;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @After
  public void verifyMocks() {
    Mockito.verifyNoMoreInteractions(engine);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTest() {
    NodeExecution nodeExecution =
        NodeExecution.builder()
            .uuid(generateUuid())
            .ambiance(Ambiance.newBuilder().setPlanExecutionId(generateUuid()).build())
            .status(Status.FAILED)
            .mode(ExecutionMode.TASK)
            .node(PlanNodeProto.newBuilder()
                      .setUuid(generateUuid())
                      .setStepType(StepType.newBuilder().setType("DUMMY").setStepCategory(StepCategory.STEP).build())
                      .build())
            .executableResponse(ExecutableResponse.newBuilder()
                                    .setTask(TaskExecutableResponse.newBuilder()
                                                 .setTaskId(generateUuid())
                                                 .setTaskCategory(TaskCategory.UNKNOWN_CATEGORY)
                                                 .build())
                                    .build())
            .interruptHistories(new ArrayList<>())
            .startTs(System.currentTimeMillis())
            .startTs(System.currentTimeMillis())
            .build();
    nodeExecutionService.save(nodeExecution);
    InterruptConfig interruptConfig =
        InterruptConfig.newBuilder()
            .setIssuedBy(IssuedBy.newBuilder()
                             .setManualIssuer(ManualIssuer.newBuilder().setIdentifier("admin@admin").build())
                             .build())
            .build();
    retryHelper.retryNodeExecution(nodeExecution.getUuid(), null, generateUuid(), interruptConfig);
    verify(executorService).submit(any(ExecutionEngineDispatcher.class));
  }
  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestCloneForRetry() {
    NodeExecution nodeExecution =
        NodeExecution.builder()
            .uuid(generateUuid())
            .ambiance(
                Ambiance.newBuilder().setPlanExecutionId(generateUuid()).addLevels(Level.newBuilder().build()).build())
            .status(Status.FAILED)
            .mode(ExecutionMode.TASK)
            .executableResponse(ExecutableResponse.newBuilder()
                                    .setTask(TaskExecutableResponse.newBuilder()
                                                 .setTaskId(generateUuid())
                                                 .setTaskCategory(TaskCategory.UNKNOWN_CATEGORY)
                                                 .build())
                                    .build())
            .interruptHistories(new ArrayList<>())
            .startTs(System.currentTimeMillis())
            .startTs(System.currentTimeMillis())
            .build();
    String newNodeUuid = generateUuid();
    InterruptConfig interruptConfig =
        InterruptConfig.newBuilder()
            .setIssuedBy(IssuedBy.newBuilder()
                             .setManualIssuer(ManualIssuer.newBuilder().setIdentifier("admin@admin").build())
                             .build())
            .build();
    NodeExecution clonedNodeExecution = retryHelper.cloneForRetry(
        nodeExecution, newNodeUuid, nodeExecution.getAmbiance(), interruptConfig, generateUuid());

    assertThat(clonedNodeExecution).isNotNull();
    assertThat(clonedNodeExecution.getUuid()).isEqualTo(newNodeUuid);
    assertThat(clonedNodeExecution.getRetryIds()).containsExactly(nodeExecution.getUuid());
    assertThat(clonedNodeExecution.getInterruptHistories()).hasSize(1);
    assertThat(clonedNodeExecution.getInterruptHistories().get(0).getInterruptType()).isEqualTo(InterruptType.RETRY);
    assertThat(clonedNodeExecution.getStartTs()).isEqualTo(0L);
    assertThat(clonedNodeExecution.getEndTs()).isNull();
    assertThat(clonedNodeExecution.getStatus()).isEqualTo(Status.QUEUED);
  }
}
