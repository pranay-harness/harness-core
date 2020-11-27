package io.harness.engine.outcomes;

import static io.harness.rule.OwnerRule.ALEXEI;
import static io.harness.rule.OwnerRule.PRASHANT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.AmbianceUtils;
import io.harness.OrchestrationTestBase;
import io.harness.category.element.UnitTests;
import io.harness.data.Outcome;
import io.harness.pms.ambiance.Ambiance;
import io.harness.refObjects.RefObjectUtil;
import io.harness.rule.Owner;
import io.harness.testlib.RealMongo;
import io.harness.utils.AmbianceTestUtils;
import io.harness.utils.DummyOutcome;

import com.google.inject.Inject;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class OutcomeServiceImplTest extends OrchestrationTestBase {
  @Inject private OutcomeService outcomeService;

  @Test
  @RealMongo
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestSaveAndFind() {
    Ambiance ambiance = AmbianceTestUtils.buildAmbiance();
    String outcomeName = "outcomeName";
    Outcome outcome = DummyOutcome.builder().test("test").build();
    outcomeService.consume(ambiance, outcomeName, outcome, "PHASE");

    // Resolve with producer id
    DummyOutcome savedOutcome = (DummyOutcome) outcomeService.resolve(
        ambiance, RefObjectUtil.getOutcomeRefObject(outcomeName, AmbianceUtils.obtainCurrentSetupId(ambiance), null));
    assertThat(savedOutcome).isNotNull();
    assertThat(savedOutcome.getTest()).isEqualTo("test");

    // Resolve with scope
    savedOutcome = (DummyOutcome) outcomeService.resolve(ambiance, RefObjectUtil.getOutcomeRefObject(outcomeName));
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
    outcomeService.consume(ambiance, outcomeName, null, null);

    Outcome outcome = outcomeService.resolve(ambiance, RefObjectUtil.getOutcomeRefObject(outcomeName));
    assertThat(outcome).isNull();
  }

  @Test
  @RealMongo
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldFetchAllOutcomesByRuntimeId() {
    Ambiance ambiance = AmbianceTestUtils.buildAmbiance();
    String outcomeName = "outcome";
    Ambiance ambiance1 = AmbianceTestUtils.buildAmbiance();
    String outcomeName1 = "outcome1";

    outcomeService.consume(ambiance, outcomeName, DummyOutcome.builder().test("test").build(), null);
    outcomeService.consume(ambiance1, outcomeName1, DummyOutcome.builder().test("test1").build(), null);

    List<Outcome> outcomes = outcomeService.findAllByRuntimeId(
        ambiance.getPlanExecutionId(), AmbianceUtils.obtainCurrentRuntimeId(ambiance));
    assertThat(outcomes.size()).isEqualTo(2);
  }

  @Test
  @RealMongo
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldFetchOutcomes() {
    Ambiance ambiance = AmbianceTestUtils.buildAmbiance();
    String outcomeName = "outcome";
    Ambiance ambiance1 = AmbianceTestUtils.buildAmbiance();
    String outcomeName1 = "outcome1";

    String instanceId1 =
        outcomeService.consume(ambiance, outcomeName, DummyOutcome.builder().test("test1").build(), null);
    String instanceId2 =
        outcomeService.consume(ambiance1, outcomeName1, DummyOutcome.builder().test("test2").build(), null);

    List<Outcome> outcomes = outcomeService.fetchOutcomes(Arrays.asList(instanceId1, instanceId2));
    assertThat(outcomes.size()).isEqualTo(2);
    assertThat(outcomes.stream().map(oc -> ((DummyOutcome) oc).getTest()).collect(Collectors.toList()))
        .containsExactlyInAnyOrder("test1", "test2");
  }

  @Test
  @RealMongo
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldFetchOutcome() {
    Ambiance ambiance = AmbianceTestUtils.buildAmbiance();
    String outcomeName = "outcome";

    String instanceId =
        outcomeService.consume(ambiance, outcomeName, DummyOutcome.builder().test("test").build(), null);

    Outcome outcome = outcomeService.fetchOutcome(instanceId);
    assertThat(outcome).isInstanceOf(DummyOutcome.class);
    assertThat(((DummyOutcome) outcome).getTest()).isEqualTo("test");
  }
}
