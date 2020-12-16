package io.harness.engine.expressions;

import static io.harness.rule.OwnerRule.PRASHANT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.OrchestrationTestBase;
import io.harness.category.element.UnitTests;
import io.harness.engine.executions.plan.PlanExecutionService;
import io.harness.engine.pms.data.PmsOutcomeService;
import io.harness.engine.pms.data.PmsSweepingOutputService;
import io.harness.execution.PlanExecution;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.serializer.persistence.DocumentOrchestrationUtils;
import io.harness.rule.Owner;
import io.harness.testlib.RealMongo;
import io.harness.utils.AmbianceTestUtils;
import io.harness.utils.DummyOutcome;
import io.harness.utils.DummySweepingOutput;

import com.google.inject.Inject;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class EngineExpressionServiceImplTest extends OrchestrationTestBase {
  @Inject EngineExpressionService engineExpressionService;
  @Inject PmsOutcomeService pmsOutcomeService;
  @Inject PmsSweepingOutputService pmsSweepingOutputService;
  @Inject PlanExecutionService planExecutionService;

  private static final String OUTCOME_NAME = "dummyOutcome";
  private static final String OUTPUT_NAME = "dummyOutput";

  private Ambiance ambiance;

  @Before
  public void setup() {
    ambiance = AmbianceTestUtils.buildAmbiance();
    planExecutionService.save(PlanExecution.builder().uuid(ambiance.getPlanExecutionId()).build());
    pmsOutcomeService.consume(ambiance, OUTCOME_NAME,
        DocumentOrchestrationUtils.convertToDocumentJson(DummyOutcome.builder().test("harness").build()), null);
    pmsSweepingOutputService.consume(ambiance, OUTPUT_NAME,
        DocumentOrchestrationUtils.convertToDocumentJson(DummySweepingOutput.builder().test("harness").build()), null);
  }

  @Test
  @RealMongo
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestRenderExpressionOutcome() {
    String resolvedExpression =
        engineExpressionService.renderExpression(ambiance, "${dummyOutcome.test} == \"harness\"");
    assertThat(resolvedExpression).isNotNull();
    assertThat(resolvedExpression).isEqualTo("harness == \"harness\"");
  }

  @Test
  @RealMongo
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestRenderExpressionOutput() {
    String resolvedExpression =
        engineExpressionService.renderExpression(ambiance, "${dummyOutput.test} == \"harness\"");
    assertThat(resolvedExpression).isNotNull();
    assertThat(resolvedExpression).isEqualTo("harness == \"harness\"");
  }

  @Test
  @RealMongo
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestEvaluateExpression() {
    Object value = engineExpressionService.evaluateExpression(ambiance, "${dummyOutcome.test} == \"harness\"");
    assertThat(value).isNotNull();
    assertThat(value).isEqualTo(true);
  }
}
