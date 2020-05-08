package io.harness.engine.services.impl;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.PRASHANT;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.OrchestrationTest;
import io.harness.ambiance.Ambiance;
import io.harness.category.element.UnitTests;
import io.harness.data.Outcome;
import io.harness.data.OutcomeInstance;
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
    String uuid = generateUuid();
    String outcomeName = "outcomeName";
    OutcomeInstance instance = OutcomeInstance.builder()
                                   .uuid(uuid)
                                   .planExecutionId(ambiance.getPlanExecutionId())
                                   .levelExecutions(ambiance.getLevelExecutions())
                                   .name(outcomeName)
                                   .outcome(DummyOutcome.builder().test("test").build())
                                   .build();
    OutcomeInstance savedInstance = outcomeService.save(instance);
    assertThat(savedInstance).isNotNull();

    DummyOutcome outcome = outcomeService.findOutcome(ambiance, outcomeName);
    assertThat(outcome).isNotNull();
    assertThat(outcome.getTest()).isEqualTo("test");
  }

  @Test
  @RealMongo
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestSaveAndFindForNull() {
    Ambiance ambiance = AmbianceTestUtils.buildAmbiance();
    String uuid = generateUuid();
    String outcomeName = "outcomeName";
    OutcomeInstance instance = OutcomeInstance.builder()
                                   .uuid(uuid)
                                   .planExecutionId(ambiance.getPlanExecutionId())
                                   .levelExecutions(ambiance.getLevelExecutions())
                                   .name(outcomeName)
                                   .build();
    OutcomeInstance savedInstance = outcomeService.save(instance);
    assertThat(savedInstance).isNotNull();

    Outcome outcome = outcomeService.findOutcome(ambiance, outcomeName);
    assertThat(outcome).isNull();
  }
}