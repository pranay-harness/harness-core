package io.harness.engine.services.impl;

import static io.harness.rule.OwnerRule.PRASHANT;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.OrchestrationTest;
import io.harness.ambiance.Ambiance;
import io.harness.category.element.UnitTests;
import io.harness.data.Outcome;
import io.harness.engine.services.OutcomeService;
import io.harness.rule.Owner;
import io.harness.testlib.RealMongo;
import io.harness.utils.AmbianceTestUtils;
import io.harness.utils.DummyOutcome;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class OutcomeServiceImplTest extends OrchestrationTest {
  @Inject private OutcomeService outcomeService;

  @Test
  @RealMongo
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestSaveAndFind() {
    Ambiance ambiance = AmbianceTestUtils.buildAmbiance();
    String outcomeName = "outcomeName";
    Outcome outcome = outcomeService.consume(ambiance, outcomeName, DummyOutcome.builder().test("test").build());
    assertThat(outcome).isNotNull();

    DummyOutcome savedOutcome = outcomeService.findOutcome(ambiance, outcomeName);
    assertThat(savedOutcome).isNotNull();
    assertThat(savedOutcome.getTest()).isEqualTo("test");
  }

  @Test
  @RealMongo
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestSaveAndFindForNull() {
    Ambiance ambiance = AmbianceTestUtils.buildAmbiance();
    String outcomeName = "outcomeName";
    Outcome savedOutCome = outcomeService.consume(ambiance, outcomeName, null);
    assertThat(savedOutCome).isNull();

    Outcome outcome = outcomeService.findOutcome(ambiance, outcomeName);
    assertThat(outcome).isNull();
  }
}