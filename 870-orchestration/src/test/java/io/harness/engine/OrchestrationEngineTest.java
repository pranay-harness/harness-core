/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.engine;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.plan.NodeType.PLAN_NODE;
import static io.harness.rule.OwnerRule.PRASHANT;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.OrchestrationTestBase;
import io.harness.category.element.UnitTests;
import io.harness.engine.pms.execution.strategy.NodeExecutionStrategyFactory;
import io.harness.engine.pms.execution.strategy.plan.PlanExecutionStrategy;
import io.harness.engine.pms.execution.strategy.plannode.PlanNodeExecutionStrategy;
import io.harness.execution.PlanExecution;
import io.harness.plan.NodeType;
import io.harness.plan.Plan;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.ambiance.Level;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import com.google.protobuf.ByteString;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class OrchestrationEngineTest extends OrchestrationTestBase {
  @Mock PlanNodeExecutionStrategy planNodeExecutionStrategy;
  @Mock PlanExecutionStrategy planExecutionStrategy;
  @Mock NodeExecutionStrategyFactory factory;
  @Inject @InjectMocks private OrchestrationEngine orchestrationEngine;

  @Before
  public void setup() {
    doReturn(planExecutionStrategy).when(factory).obtainStrategy(NodeType.PLAN);
    doReturn(planNodeExecutionStrategy).when(factory).obtainStrategy(PLAN_NODE);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestTriggerNode() {
    Ambiance ambiance = Ambiance.newBuilder().build();
    Plan plan = Plan.builder().build();
    when(planExecutionStrategy.triggerNode(ambiance, plan, null)).thenReturn(PlanExecution.builder().build());
    orchestrationEngine.triggerNode(ambiance, plan, null);
    verify(planExecutionStrategy).triggerNode(ambiance, plan, null);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestStartNodeExecution() {
    Ambiance ambiance =
        Ambiance.newBuilder()
            .setPlanExecutionId(generateUuid())
            .addLevels(Level.newBuilder().setRuntimeId(generateUuid()).setNodeType(PLAN_NODE.toString()).build())
            .build();
    doNothing().when(planNodeExecutionStrategy).startExecution(ambiance);
    orchestrationEngine.startNodeExecution(ambiance);
    verify(planNodeExecutionStrategy).startExecution(eq(ambiance));
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestResume() {
    Ambiance ambiance =
        Ambiance.newBuilder()
            .setPlanExecutionId(generateUuid())
            .addLevels(Level.newBuilder().setRuntimeId(generateUuid()).setNodeType(PLAN_NODE.toString()).build())
            .build();
    doNothing().when(planNodeExecutionStrategy).startExecution(ambiance);
    Map<String, ByteString> response = new HashMap<>();
    orchestrationEngine.resumeNodeExecution(ambiance, response, false);
    verify(planNodeExecutionStrategy).resumeNodeExecution(eq(ambiance), eq(response), eq(false));
  }
}
