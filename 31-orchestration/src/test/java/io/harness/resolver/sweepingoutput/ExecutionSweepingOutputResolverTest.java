package io.harness.resolver.sweepingoutput;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.GARVIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.inject.Inject;

import io.harness.OrchestrationTest;
import io.harness.ambiance.Ambiance;
import io.harness.ambiance.Level;
import io.harness.beans.SweepingOutput;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.references.SweepingOutputRefObject;
import io.harness.rule.Owner;
import io.harness.testlib.RealMongo;
import io.harness.utils.AmbianceTestUtils;
import io.harness.utils.DummySweepingOutput;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class ExecutionSweepingOutputResolverTest extends OrchestrationTest {
  private static final String STEP_RUNTIME_ID = generateUuid();
  private static final String STEP_SETUP_ID = generateUuid();

  @Inject private ExecutionSweepingOutputResolver executionSweepingOutputResolver;

  @Test
  @RealMongo
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testSaveAndFind() {
    Ambiance ambianceSection = AmbianceTestUtils.buildAmbiance();
    Ambiance ambiancePhase = ambianceSection.cloneForFinish();
    Ambiance ambianceStep = ambianceSection.cloneForChild();
    ambianceStep.addLevel(Level.builder().runtimeId(STEP_RUNTIME_ID).setupId(STEP_SETUP_ID).build());

    String outputName = "outputName";
    String testValueSection = "testSection";
    String testValueStep = "testStep";

    executionSweepingOutputResolver.consume(
        ambianceSection, outputName, DummySweepingOutput.builder().test(testValueSection).build());
    validateResult(resolve(ambianceSection, outputName), testValueSection);
    validateResult(resolve(ambianceStep, outputName), testValueSection);
    assertThatThrownBy(() -> resolve(ambiancePhase, outputName)).isInstanceOf(InvalidRequestException.class);

    executionSweepingOutputResolver.consume(
        ambianceStep, outputName, DummySweepingOutput.builder().test(testValueStep).build());
    validateResult(resolve(ambianceSection, outputName), testValueSection);
    validateResult(resolve(ambianceStep, outputName), testValueStep);
    assertThatThrownBy(() -> resolve(ambiancePhase, outputName)).isInstanceOf(InvalidRequestException.class);
  }

  private void validateResult(SweepingOutput foundOutput, String testValue) {
    assertThat(foundOutput).isNotNull();
    assertThat(foundOutput).isInstanceOf(DummySweepingOutput.class);

    DummySweepingOutput dummySweepingOutput = (DummySweepingOutput) foundOutput;
    assertThat(dummySweepingOutput.getTest()).isEqualTo(testValue);
  }

  @Test
  @RealMongo
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void shouldTestSaveAndFindForNull() {
    Ambiance ambiance = AmbianceTestUtils.buildAmbiance();
    String outputName = "outcomeName";
    executionSweepingOutputResolver.consume(ambiance, outputName, null);

    SweepingOutput output = resolve(ambiance, outputName);
    assertThat(output).isNull();
  }

  private SweepingOutput resolve(Ambiance ambiance, String outputName) {
    return executionSweepingOutputResolver.resolve(
        ambiance, SweepingOutputRefObject.builder().name(outputName).producerId("producer_id").build());
  }
}
