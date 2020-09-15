package io.harness;

import io.harness.advisers.retry.RetryAdviserTest;
import io.harness.beans.StepExecutionStatusTest;
import io.harness.engine.OrchestrationEngineTest;
import io.harness.engine.advise.handlers.RetryAdviseHandlerTest;
import io.harness.engine.events.OrchestrationEventEmitterTest;
import io.harness.engine.executions.node.NodeExecutionServiceImplTest;
import io.harness.engine.executions.plan.PlanExecutionServiceImplTest;
import io.harness.engine.expressions.EngineExpressionServiceImplTest;
import io.harness.engine.expressions.SampleExpressionEvaluatorTest;
import io.harness.engine.expressions.functors.NodeExecutionValueTest;
import io.harness.engine.interrupts.InterruptServiceImplTest;
import io.harness.engine.outcomes.OutcomeServiceImplTest;
import io.harness.engine.outputs.ExecutionSweepingOutputServiceImplTest;
import io.harness.expression.AmbianceExpressionEvaluatorTest;
import io.harness.runners.GuiceSuiteRunner;
import io.harness.runners.ModuleProvider;
import io.harness.runners.ModuleRunner;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(GuiceSuiteRunner.class)
@Suite.SuiteClasses({RetryAdviserTest.class, StepExecutionStatusTest.class, RetryAdviseHandlerTest.class,
    OrchestrationEventEmitterTest.class, NodeExecutionServiceImplTest.class, PlanExecutionServiceImplTest.class,
    EngineExpressionServiceImplTest.class, SampleExpressionEvaluatorTest.class, NodeExecutionValueTest.class,
    InterruptServiceImplTest.class, OutcomeServiceImplTest.class, ExecutionSweepingOutputServiceImplTest.class,
    OrchestrationEngineTest.class, AmbianceExpressionEvaluatorTest.class})
@ModuleProvider(OrchestrationModuleListProvider.class)
@ModuleRunner(OrchestrationGuiceRunner.class)
public class OrchestrationTestSuite {}
