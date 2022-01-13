/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.engine.pms.data;

import static io.harness.rule.OwnerRule.ALEXEI;
import static io.harness.rule.OwnerRule.PRASHANT;
import static io.harness.rule.OwnerRule.PRASHANTSHARMA;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anySet;
import static org.mockito.Mockito.when;

import io.harness.OrchestrationTestBase;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.data.OutcomeInstance;
import io.harness.engine.expressions.ExpressionEvaluatorProvider;
import io.harness.expression.EngineExpressionEvaluator;
import io.harness.expression.VariableResolverTracker;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.data.PmsOutcome;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.sdk.core.data.Outcome;
import io.harness.pms.sdk.core.resolver.RefObjectUtils;
import io.harness.pms.serializer.recaster.RecastOrchestrationUtils;
import io.harness.rule.Owner;
import io.harness.utils.AmbianceTestUtils;
import io.harness.utils.DummyOrchestrationOutcome;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.data.mongodb.core.MongoTemplate;

@OwnedBy(HarnessTeam.PIPELINE)
public class PmsOutcomeServiceImplTest extends OrchestrationTestBase {
  @Inject private MongoTemplate mongoTemplate;
  @Mock private ExpressionEvaluatorProvider expressionEvaluatorProvider;
  @Inject @InjectMocks @Spy private PmsOutcomeServiceImpl pmsOutcomeService;

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestSaveAndFind() {
    Ambiance ambiance = AmbianceTestUtils.buildAmbiance();
    String outcomeName = "outcomeName";
    Outcome outcome = DummyOrchestrationOutcome.builder().test("test").build();
    pmsOutcomeService.consume(ambiance, outcomeName, RecastOrchestrationUtils.toJson(outcome), "PHASE");

    // Resolve with producer id
    DummyOrchestrationOutcome savedOutcome = RecastOrchestrationUtils.fromJson(
        pmsOutcomeService.resolve(ambiance,
            RefObjectUtils.getOutcomeRefObject(outcomeName, AmbianceUtils.obtainCurrentSetupId(ambiance), null)),
        DummyOrchestrationOutcome.class);
    assertThat(savedOutcome).isNotNull();
    assertThat(savedOutcome.getTest()).isEqualTo("test");

    // Resolve with scope
    savedOutcome = RecastOrchestrationUtils.fromJson(
        pmsOutcomeService.resolve(ambiance, RefObjectUtils.getOutcomeRefObject(outcomeName)),
        DummyOrchestrationOutcome.class);
    assertThat(savedOutcome).isNotNull();
    assertThat(savedOutcome.getTest()).isEqualTo("test");
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestOutcomeInstancePopulation() {
    Ambiance ambiance = AmbianceTestUtils.buildAmbiance();
    String outcomeName = "outcomeName";
    Outcome outcome = DummyOrchestrationOutcome.builder().test("test").build();
    String outComeInstanceId =
        pmsOutcomeService.consume(ambiance, outcomeName, RecastOrchestrationUtils.toJson(outcome), "PHASE");

    assertThat(outComeInstanceId).isNotNull();
    OutcomeInstance instance = mongoTemplate.findById(outComeInstanceId, OutcomeInstance.class);
    assertThat(instance).isNotNull();
    assertThat(instance.getProducedBy()).isEqualTo(AmbianceUtils.obtainCurrentLevel(ambiance));
    assertThat(instance.getPlanExecutionId()).isEqualTo(AmbianceTestUtils.PLAN_EXECUTION_ID);
    assertThat(instance.getName()).isEqualTo(outcomeName);
    assertThat(instance.getGroupName()).isEqualTo("PHASE");
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestSaveAndFindForNull() {
    Ambiance ambiance = AmbianceTestUtils.buildAmbiance();
    String outcomeName = "outcomeName";
    pmsOutcomeService.consume(ambiance, outcomeName, null, null);

    Outcome outcome = RecastOrchestrationUtils.fromJson(
        pmsOutcomeService.resolve(ambiance, RefObjectUtils.getOutcomeRefObject(outcomeName)), Outcome.class);
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

    pmsOutcomeService.consume(ambiance, outcomeName,
        RecastOrchestrationUtils.toJson(DummyOrchestrationOutcome.builder().test("test").build()), null);
    pmsOutcomeService.consume(ambiance1, outcomeName1,
        RecastOrchestrationUtils.toJson(DummyOrchestrationOutcome.builder().test("test1").build()), null);

    List<String> outcomes = pmsOutcomeService.findAllByRuntimeId(
        ambiance.getPlanExecutionId(), AmbianceUtils.obtainCurrentRuntimeId(ambiance));
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

    String instanceId1 = pmsOutcomeService.consume(ambiance, outcomeName,
        RecastOrchestrationUtils.toJson(DummyOrchestrationOutcome.builder().test("test1").build()), null);
    String instanceId2 = pmsOutcomeService.consume(ambiance1, outcomeName1,
        RecastOrchestrationUtils.toJson(DummyOrchestrationOutcome.builder().test("test2").build()), null);

    List<String> outcomes = pmsOutcomeService.fetchOutcomes(Arrays.asList(instanceId1, instanceId2));
    assertThat(outcomes.size()).isEqualTo(2);
    assertThat(outcomes.stream()
                   .map(oc -> (RecastOrchestrationUtils.fromJson(oc, DummyOrchestrationOutcome.class)).getTest())
                   .collect(Collectors.toList()))
        .containsExactlyInAnyOrder("test1", "test2");
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldFetchOutcome() {
    Ambiance ambiance = AmbianceTestUtils.buildAmbiance();
    String outcomeName = "outcome";

    String instanceId = pmsOutcomeService.consume(ambiance, outcomeName,
        RecastOrchestrationUtils.toJson(DummyOrchestrationOutcome.builder().test("test").build()), null);

    String outcomeJson = pmsOutcomeService.fetchOutcome(instanceId);
    Outcome outcome = RecastOrchestrationUtils.fromJson(outcomeJson, Outcome.class);
    assertThat(outcome).isInstanceOf(DummyOrchestrationOutcome.class);
    assertThat(((DummyOrchestrationOutcome) outcome).getTest()).isEqualTo("test");
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldResolveOptional() {
    Ambiance ambiance = AmbianceTestUtils.buildAmbiance();
    String outcomeName = "outcome";

    String outcomeJson = RecastOrchestrationUtils.toJson(DummyOrchestrationOutcome.builder().test("test").build());
    pmsOutcomeService.consume(ambiance, outcomeName, outcomeJson, null);

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

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldResolveOptionalWithDots() {
    Ambiance ambiance = AmbianceTestUtils.buildAmbiance();
    String outcomeName = "outcome.name";

    String outcomeJson = RecastOrchestrationUtils.toJson(DummyOrchestrationOutcome.builder().test("test").build());
    pmsOutcomeService.consume(ambiance, outcomeName, outcomeJson, null);

    when(expressionEvaluatorProvider.get(
             any(VariableResolverTracker.class), any(Ambiance.class), anySet(), anyBoolean()))
        .thenReturn(prepareEngineExpressionEvaluator(
            ImmutableMap.of(outcomeName, DummyOrchestrationOutcome.builder().test("test").build())));

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

  public static class SampleEngineExpressionEvaluator extends EngineExpressionEvaluator {
    private final Map<String, Object> contextMap;

    public SampleEngineExpressionEvaluator(Map<String, Object> contextMap) {
      super(null);
      this.contextMap = contextMap;
    }

    @Override
    protected void initialize() {
      super.initialize();
      contextMap.forEach(this::addToContext);
    }
  }

  private static EngineExpressionEvaluator prepareEngineExpressionEvaluator(Map<String, Object> contextMap) {
    return new SampleEngineExpressionEvaluator(contextMap);
  }

  @Test
  @Owner(developers = PRASHANTSHARMA)
  @Category(UnitTests.class)
  public void testFetchOutcomeInstanceByRuntimeId() {
    List<OutcomeInstance> result = pmsOutcomeService.fetchOutcomeInstanceByRuntimeId("abc");
    assertThat(result.size()).isEqualTo(0);
  }

  @Test
  @Owner(developers = PRASHANTSHARMA)
  @Category(UnitTests.class)
  public void testCloneForRetryExecution() {
    Ambiance ambiance = AmbianceTestUtils.buildAmbiance();
    String outcomeName = "outcome.name";

    String outcomeJson = RecastOrchestrationUtils.toJson(DummyOrchestrationOutcome.builder().test("test").build());
    pmsOutcomeService.consume(ambiance, outcomeName, outcomeJson, null);

    when(pmsOutcomeService.fetchOutcomeInstanceByRuntimeId(any()))
        .thenReturn(Collections.singletonList(OutcomeInstance.builder()
                                                  .planExecutionId("plan")
                                                  .name(outcomeName)
                                                  .outcomeValue(PmsOutcome.parse(outcomeJson))
                                                  .build()));
    assertThat(pmsOutcomeService.cloneForRetryExecution(ambiance, "node").size()).isEqualTo(1);
  }
}
