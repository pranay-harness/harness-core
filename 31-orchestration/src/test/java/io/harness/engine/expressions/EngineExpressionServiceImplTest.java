package io.harness.engine.expressions;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.PRASHANT;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.OrchestrationTest;
import io.harness.ambiance.Ambiance;
import io.harness.beans.EmbeddedUser;
import io.harness.category.element.UnitTests;
import io.harness.engine.services.OutcomeService;
import io.harness.engine.services.PlanExecutionService;
import io.harness.execution.PlanExecution;
import io.harness.rule.Owner;
import io.harness.testlib.RealMongo;
import io.harness.utils.AmbianceTestUtils;
import io.harness.utils.DummyOutcome;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class EngineExpressionServiceImplTest extends OrchestrationTest {
  @Inject EngineExpressionService engineExpressionService;
  @Inject OutcomeService outcomeService;
  @Inject PlanExecutionService planExecutionService;

  private static final String OUTCOME_NAME = "dummyOutcome";

  private Ambiance ambiance;

  private static final EmbeddedUser EMBEDDED_USER = new EmbeddedUser(generateUuid(), PRASHANT, PRASHANT);

  @Before
  public void setup() {
    ambiance = AmbianceTestUtils.buildAmbiance();
    planExecutionService.save(
        PlanExecution.builder().uuid(ambiance.getPlanExecutionId()).createdBy(EMBEDDED_USER).build());
    outcomeService.consume(ambiance, OUTCOME_NAME, DummyOutcome.builder().test("harness").build(), null);
  }

  @Test
  @RealMongo
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestRenderExpression() {
    String resolvedExpression =
        engineExpressionService.renderExpression(ambiance, "${dummyOutcome.test} == \"harness\"");
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