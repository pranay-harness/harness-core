package io.harness.engine.expressions;

import static io.harness.rule.OwnerRule.PRASHANT;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.OrchestrationTest;
import io.harness.ambiance.Ambiance;
import io.harness.category.element.UnitTests;
import io.harness.engine.services.OutcomeService;
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

  private static final String OUTCOME_NAME = "dummyOutcome";

  private Ambiance ambiance;

  @Before
  public void setup() {
    ambiance = AmbianceTestUtils.buildAmbiance();
    outcomeService.consume(ambiance, OUTCOME_NAME, DummyOutcome.builder().test("harness").build());
  }

  @Test
  @RealMongo
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestRenderExpression() {
    String resolvedExpression = engineExpressionService.renderExpression(ambiance, "${dummyOutcome.test}");
    assertThat(resolvedExpression).isNotNull();
    assertThat(resolvedExpression).isEqualTo("harness");
  }
}