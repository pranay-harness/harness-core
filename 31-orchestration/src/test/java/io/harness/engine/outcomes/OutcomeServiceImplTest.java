package io.harness.engine.outcomes;

import static io.harness.rule.OwnerRule.ALEXEI;
import static io.harness.rule.OwnerRule.PRASHANT;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.OrchestrationModuleListProvider;
import io.harness.OrchestrationTest;
import io.harness.ambiance.Ambiance;
import io.harness.category.element.UnitTests;
import io.harness.data.Outcome;
import io.harness.references.OutcomeRefObject;
import io.harness.rule.Owner;
import io.harness.runners.GuiceRunner;
import io.harness.runners.ModuleProvider;
import io.harness.utils.AmbianceTestUtils;
import io.harness.utils.DummyOutcome;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RunWith(GuiceRunner.class)
@ModuleProvider(OrchestrationModuleListProvider.class)
public class OutcomeServiceImplTest extends OrchestrationTest {
  @Inject private OutcomeService outcomeService;

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestSaveAndFind() {
    Ambiance ambiance = AmbianceTestUtils.buildAmbiance();
    String outcomeName = "outcomeName";
    Outcome outcome = DummyOutcome.builder().test("test").build();
    outcomeService.consume(ambiance, outcomeName, outcome, "PHASE");

    // Resolve with producer id
    DummyOutcome savedOutcome = (DummyOutcome) outcomeService.resolve(ambiance,
        OutcomeRefObject.builder().producerId(ambiance.obtainCurrentLevel().getSetupId()).name(outcomeName).build());
    assertThat(savedOutcome).isNotNull();
    assertThat(savedOutcome.getTest()).isEqualTo("test");

    // Resolve with scope
    savedOutcome =
        (DummyOutcome) outcomeService.resolve(ambiance, OutcomeRefObject.builder().name(outcomeName).build());
    assertThat(savedOutcome).isNotNull();
    assertThat(savedOutcome.getTest()).isEqualTo("test");
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestSaveAndFindForNull() {
    Ambiance ambiance = AmbianceTestUtils.buildAmbiance();
    String outcomeName = "outcomeName";
    outcomeService.consume(ambiance, outcomeName, null, null);

    Outcome outcome = outcomeService.resolve(ambiance, OutcomeRefObject.builder().name(outcomeName).build());
    assertThat(outcome).isNull();
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldFetchAllOutcomesByRuntimeId() {
    Ambiance ambiance = AmbianceTestUtils.buildAmbiance();
    String outcomeName = "outcome";
    Ambiance ambiance1 = AmbianceTestUtils.buildAmbiance();
    String outcomeName1 = "outcome1";

    outcomeService.consume(ambiance, outcomeName, DummyOutcome.builder().test("test").build(), null);
    outcomeService.consume(ambiance1, outcomeName1, DummyOutcome.builder().test("test1").build(), null);

    List<Outcome> outcomes =
        outcomeService.findAllByRuntimeId(ambiance.getPlanExecutionId(), ambiance.obtainCurrentRuntimeId());
    assertThat(outcomes.size()).isEqualTo(2);
  }

  @Test
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