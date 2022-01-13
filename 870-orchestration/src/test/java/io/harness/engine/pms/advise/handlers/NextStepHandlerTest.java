/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.engine.pms.advise.handlers;

import static io.harness.rule.OwnerRule.ALEXEI;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.OrchestrationTestBase;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.engine.OrchestrationEngine;
import io.harness.engine.executions.node.NodeExecutionService;
import io.harness.engine.executions.plan.PlanService;
import io.harness.execution.NodeExecution;
import io.harness.pms.contracts.advisers.AdviserResponse;
import io.harness.pms.contracts.advisers.NextStepAdvise;
import io.harness.pms.contracts.execution.Status;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@OwnedBy(HarnessTeam.PIPELINE)
public class NextStepHandlerTest extends OrchestrationTestBase {
  @Mock private OrchestrationEngine engine;
  @Mock private NodeExecutionService nodeExecutionService;
  @Mock private PlanService planService;

  @Inject @InjectMocks private NextStepHandler nextStepHandler;

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void handleAdviseWhenNextNodeIsIsEmpty() {
    AdviserResponse adviserResponse =
        AdviserResponse.newBuilder()
            .setNextStepAdvise(NextStepAdvise.newBuilder().setToStatus(Status.RUNNING).build())
            .build();

    when(nodeExecutionService.updateStatusWithOps(anyString(), any(), any(), any())).thenReturn(null);
    doNothing().when(engine).endNodeExecution(any());

    nextStepHandler.handleAdvise(NodeExecution.builder().build(), adviserResponse);

    verify(nodeExecutionService).updateStatusWithOps(anyString(), any(), any(), any());
    verify(engine).endNodeExecution(any());
  }
}
