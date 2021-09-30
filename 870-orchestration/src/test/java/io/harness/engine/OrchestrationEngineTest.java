package io.harness.engine;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.plan.NodeType.PLAN_NODE;
import static io.harness.rule.OwnerRule.PRASHANT;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import io.harness.OrchestrationTestBase;
import io.harness.category.element.UnitTests;
import io.harness.engine.pms.execution.strategy.NodeExecutionStrategyFactory;
import io.harness.engine.pms.execution.strategy.plan.PlanExecutionStrategy;
import io.harness.engine.pms.execution.strategy.plannode.PlanNodeExecutionStrategy;
import io.harness.plan.NodeType;
import io.harness.plan.Plan;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.ambiance.Level;
import io.harness.rule.Owner;

import com.google.inject.Inject;
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
    doNothing().when(planExecutionStrategy).triggerNode(ambiance, plan);
    orchestrationEngine.triggerNode(ambiance, plan);
    verify(planExecutionStrategy).triggerNode(ambiance, plan);
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
}
