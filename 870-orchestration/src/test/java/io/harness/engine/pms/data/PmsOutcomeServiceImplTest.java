package io.harness.engine.pms.data;

import static io.harness.rule.OwnerRule.ALEXEI;
import static io.harness.rule.OwnerRule.PRASHANT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.OrchestrationTestBase;
import io.harness.category.element.UnitTests;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.sdk.core.data.Outcome;
import io.harness.pms.sdk.core.resolver.RefObjectUtil;
import io.harness.pms.serializer.persistence.DocumentOrchestrationUtils;
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

public class PmsOutcomeServiceImplTest extends OrchestrationTestBase {
  @Inject private PmsOutcomeService pmsOutcomeService;

  @Test
  @RealMongo
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestSaveAndFind() {
    Ambiance ambiance = AmbianceTestUtils.buildAmbiance();
    String outcomeName = "outcomeName";
    Outcome outcome = DummyOutcome.builder().test("test").build();
    pmsOutcomeService.consume(
        ambiance, outcomeName, DocumentOrchestrationUtils.convertToDocumentJson(outcome), "PHASE");

    // Resolve with producer id
    DummyOutcome savedOutcome = DocumentOrchestrationUtils.convertFromDocumentJson(pmsOutcomeService.resolve(
        ambiance, RefObjectUtil.getOutcomeRefObject(outcomeName, AmbianceUtils.obtainCurrentSetupId(ambiance), null)));
    assertThat(savedOutcome).isNotNull();
    assertThat(savedOutcome.getTest()).isEqualTo("test");

    // Resolve with scope
    savedOutcome = DocumentOrchestrationUtils.convertFromDocumentJson(
        pmsOutcomeService.resolve(ambiance, RefObjectUtil.getOutcomeRefObject(outcomeName)));
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
    pmsOutcomeService.consume(ambiance, outcomeName, null, null);

    Outcome outcome = DocumentOrchestrationUtils.convertFromDocumentJson(
        pmsOutcomeService.resolve(ambiance, RefObjectUtil.getOutcomeRefObject(outcomeName)));
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
        DocumentOrchestrationUtils.convertToDocumentJson(DummyOutcome.builder().test("test").build()), null);
    pmsOutcomeService.consume(ambiance1, outcomeName1,
        DocumentOrchestrationUtils.convertToDocumentJson(DummyOutcome.builder().test("test1").build()), null);

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
        DocumentOrchestrationUtils.convertToDocumentJson(DummyOutcome.builder().test("test1").build()), null);
    String instanceId2 = pmsOutcomeService.consume(ambiance1, outcomeName1,
        DocumentOrchestrationUtils.convertToDocumentJson(DummyOutcome.builder().test("test2").build()), null);

    List<String> outcomes = pmsOutcomeService.fetchOutcomes(Arrays.asList(instanceId1, instanceId2));
    assertThat(outcomes.size()).isEqualTo(2);
    assertThat(outcomes.stream()
                   .map(oc -> ((DummyOutcome) DocumentOrchestrationUtils.convertFromDocumentJson(oc)).getTest())
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
        DocumentOrchestrationUtils.convertToDocumentJson(DummyOutcome.builder().test("test").build()), null);

    String outcomeJson = pmsOutcomeService.fetchOutcome(instanceId);
    Outcome outcome = DocumentOrchestrationUtils.convertFromDocumentJson(outcomeJson);
    assertThat(outcome).isInstanceOf(DummyOutcome.class);
    assertThat(((DummyOutcome) outcome).getTest()).isEqualTo("test");
  }
}