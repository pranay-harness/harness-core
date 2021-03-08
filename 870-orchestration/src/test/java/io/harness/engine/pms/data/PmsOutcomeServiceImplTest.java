package io.harness.engine.pms.data;

import static io.harness.rule.OwnerRule.ALEXEI;
import static io.harness.rule.OwnerRule.PRASHANT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.OrchestrationTestBase;
import io.harness.category.element.UnitTests;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.sdk.core.data.Outcome;
import io.harness.pms.sdk.core.resolver.RefObjectUtils;
import io.harness.pms.serializer.recaster.RecastOrchestrationUtils;
import io.harness.rule.Owner;
import io.harness.testlib.RealMongo;
import io.harness.utils.AmbianceTestUtils;
import io.harness.utils.DummyOrchestrationOutcome;

import com.google.inject.Inject;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class PmsOutcomeServiceImplTest extends OrchestrationTestBase {
  @Inject private PmsOutcomeService pmsOutcomeService;

  @Test
  @RealMongo
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestSaveAndFind() {
    Ambiance ambiance = AmbianceTestUtils.buildAmbiance();
    String outcomeName = "outcomeName";
    Outcome outcome = DummyOrchestrationOutcome.builder().test("test").build();
    pmsOutcomeService.consume(ambiance, outcomeName, RecastOrchestrationUtils.toDocumentJson(outcome), "PHASE", false);

    // Resolve with producer id
    DummyOrchestrationOutcome savedOutcome = RecastOrchestrationUtils.fromDocumentJson(
        pmsOutcomeService.resolve(ambiance,
            RefObjectUtils.getOutcomeRefObject(outcomeName, AmbianceUtils.obtainCurrentSetupId(ambiance), null)),
        DummyOrchestrationOutcome.class);
    assertThat(savedOutcome).isNotNull();
    assertThat(savedOutcome.getTest()).isEqualTo("test");

    // Resolve with scope
    savedOutcome = RecastOrchestrationUtils.fromDocumentJson(
        pmsOutcomeService.resolve(ambiance, RefObjectUtils.getOutcomeRefObject(outcomeName)),
        DummyOrchestrationOutcome.class);
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
    pmsOutcomeService.consume(ambiance, outcomeName, null, null, false);

    Outcome outcome = RecastOrchestrationUtils.fromDocumentJson(
        pmsOutcomeService.resolve(ambiance, RefObjectUtils.getOutcomeRefObject(outcomeName)), Outcome.class);
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

    pmsOutcomeService.consume(ambiance, outcomeName,
        RecastOrchestrationUtils.toDocumentJson(DummyOrchestrationOutcome.builder().test("test").build()), null, false);
    pmsOutcomeService.consume(ambiance1, outcomeName1,
        RecastOrchestrationUtils.toDocumentJson(DummyOrchestrationOutcome.builder().test("test1").build()), null,
        false);

    List<String> outcomes = pmsOutcomeService.findAllByRuntimeId(
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

    String instanceId1 = pmsOutcomeService.consume(ambiance, outcomeName,
        RecastOrchestrationUtils.toDocumentJson(DummyOrchestrationOutcome.builder().test("test1").build()), null,
        false);
    String instanceId2 = pmsOutcomeService.consume(ambiance1, outcomeName1,
        RecastOrchestrationUtils.toDocumentJson(DummyOrchestrationOutcome.builder().test("test2").build()), null,
        false);

    List<String> outcomes = pmsOutcomeService.fetchOutcomes(Arrays.asList(instanceId1, instanceId2));
    assertThat(outcomes.size()).isEqualTo(2);
    assertThat(
        outcomes.stream()
            .map(oc -> (RecastOrchestrationUtils.fromDocumentJson(oc, DummyOrchestrationOutcome.class)).getTest())
            .collect(Collectors.toList()))
        .containsExactlyInAnyOrder("test1", "test2");
  }

  @Test
  @RealMongo
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldFetchOutcome() {
    Ambiance ambiance = AmbianceTestUtils.buildAmbiance();
    String outcomeName = "outcome";

    String instanceId = pmsOutcomeService.consume(ambiance, outcomeName,
        RecastOrchestrationUtils.toDocumentJson(DummyOrchestrationOutcome.builder().test("test").build()), null, false);

    String outcomeJson = pmsOutcomeService.fetchOutcome(instanceId);
    Outcome outcome = RecastOrchestrationUtils.fromDocumentJson(outcomeJson, Outcome.class);
    assertThat(outcome).isInstanceOf(DummyOrchestrationOutcome.class);
    assertThat(((DummyOrchestrationOutcome) outcome).getTest()).isEqualTo("test");
  }

  @Test
  @RealMongo
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldResolveOptional() {
    Ambiance ambiance = AmbianceTestUtils.buildAmbiance();
    String outcomeName = "outcome";

    String outcomeJson =
        RecastOrchestrationUtils.toDocumentJson(DummyOrchestrationOutcome.builder().test("test").build());
    pmsOutcomeService.consume(ambiance, outcomeName, outcomeJson, null, false);

    // Resolve with producer id
    OptionalOutcome optionalOutcome = pmsOutcomeService.resolveOptional(
        ambiance, RefObjectUtils.getOutcomeRefObject(outcomeName, AmbianceUtils.obtainCurrentSetupId(ambiance), null));
    assertThat(optionalOutcome).isNotNull();
    assertThat(optionalOutcome.getOutcome()).isEqualTo(outcomeJson);
    assertThat(optionalOutcome.isFound()).isTrue();

    // Resolve with scope
    optionalOutcome = pmsOutcomeService.resolveOptional(ambiance, RefObjectUtils.getOutcomeRefObject(outcomeName));
    assertThat(optionalOutcome).isNotNull();
    assertThat(optionalOutcome.getOutcome()).isEqualTo(outcomeJson);
    assertThat(optionalOutcome.isFound()).isTrue();
  }

  @Test
  @RealMongo
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldResolveInternalWhenOutcomeIsNotFound() {
    Ambiance ambiance = AmbianceTestUtils.buildAmbiance();
    String outcomeName = "outcome";

    OptionalOutcome optionalOutcome = pmsOutcomeService.resolveOptional(
        ambiance, RefObjectUtils.getOutcomeRefObject(outcomeName, AmbianceUtils.obtainCurrentSetupId(ambiance), null));
    assertThat(optionalOutcome).isNotNull();
    assertThat(optionalOutcome.getOutcome()).isNull();
    assertThat(optionalOutcome.isFound()).isEqualTo(false);
  }
}