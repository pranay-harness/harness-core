package io.harness.engine.outputs;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.GARVIT;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.OrchestrationTestBase;
import io.harness.category.element.UnitTests;
import io.harness.pms.ambiance.Ambiance;
import io.harness.pms.ambiance.Level;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.sdk.core.data.SweepingOutput;
import io.harness.pms.sdk.core.resolver.GroupNotFoundException;
import io.harness.pms.sdk.core.resolver.RefObjectUtil;
import io.harness.pms.sdk.core.resolver.ResolverUtils;
import io.harness.pms.steps.StepType;
import io.harness.rule.Owner;
import io.harness.testlib.RealMongo;
import io.harness.utils.AmbianceTestUtils;
import io.harness.utils.DummySweepingOutput;

import com.google.inject.Inject;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class ExecutionSweepingOutputServiceImplTest extends OrchestrationTestBase {
  private static final String STEP_RUNTIME_ID = generateUuid();
  private static final String STEP_SETUP_ID = generateUuid();

  @Inject private ExecutionSweepingOutputService executionSweepingOutputService;

  @Test
  @RealMongo
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testConsumeAndFind() {
    Ambiance ambianceSection = AmbianceTestUtils.buildAmbiance();
    Ambiance ambiancePhase = AmbianceUtils.cloneForFinish(ambianceSection);
    Ambiance ambianceStep = prepareStepAmbiance(ambianceSection);

    String outputName = "outputName";
    String testValueSection = "testSection";
    String testValueStep = "testStep";

    executionSweepingOutputService.consume(
        ambianceSection, outputName, DummySweepingOutput.builder().test(testValueSection).build(), null);
    validateResult(resolve(ambianceSection, outputName), testValueSection);
    validateResult(resolve(ambianceStep, outputName), testValueSection);
    assertThatThrownBy(() -> resolve(ambiancePhase, outputName)).isInstanceOf(SweepingOutputException.class);

    executionSweepingOutputService.consume(
        ambianceStep, outputName, DummySweepingOutput.builder().test(testValueStep).build(), null);
    validateResult(resolve(ambianceSection, outputName), testValueSection);
    validateResult(resolve(ambianceStep, outputName), testValueStep);
    assertThatThrownBy(() -> resolve(ambiancePhase, outputName)).isInstanceOf(SweepingOutputException.class);
  }

  @Test
  @RealMongo
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testSaveWithLevelsToKeepAndFind() {
    Ambiance ambianceSection = AmbianceTestUtils.buildAmbiance();
    Ambiance ambiancePhase = AmbianceUtils.cloneForFinish(ambianceSection);
    Ambiance ambianceStep = prepareStepAmbiance(ambianceSection);

    String outputName = "outputName";
    String testValueSection = "testSection";
    String testValueStep = "testStep";

    executionSweepingOutputService.consumeInternal(
        ambianceSection, outputName, DummySweepingOutput.builder().test(testValueSection).build(), 2);
    validateResult(resolve(ambianceSection, outputName), testValueSection);
    validateResult(resolve(ambianceStep, outputName), testValueSection);
    assertThatThrownBy(() -> resolve(ambiancePhase, outputName)).isInstanceOf(SweepingOutputException.class);

    executionSweepingOutputService.consumeInternal(
        ambianceStep, outputName, DummySweepingOutput.builder().test(testValueStep).build(), 0);
    validateResult(resolve(ambiancePhase, outputName), testValueStep);
    validateResult(resolve(ambianceSection, outputName), testValueSection);
    validateResult(resolve(ambianceStep, outputName), testValueSection);
  }

  @Test
  @RealMongo
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testSaveAtScopeAndFind() {
    Ambiance ambianceSection = AmbianceTestUtils.buildAmbiance();
    Ambiance ambiancePhase = AmbianceUtils.cloneForFinish(ambianceSection);
    Ambiance ambianceStep = prepareStepAmbiance(ambianceSection);

    String outputName = "outputName";
    String testValueSection = "testSection";
    String testValueStep = "testStep";

    executionSweepingOutputService.consume(
        ambianceSection, outputName, DummySweepingOutput.builder().test(testValueSection).build(), "SECTION");
    validateResult(resolve(ambianceSection, outputName), testValueSection);
    validateResult(resolve(ambianceStep, outputName), testValueSection);
    assertThatThrownBy(() -> resolve(ambiancePhase, outputName)).isInstanceOf(SweepingOutputException.class);

    executionSweepingOutputService.consume(ambianceStep, outputName,
        DummySweepingOutput.builder().test(testValueStep).build(), ResolverUtils.GLOBAL_GROUP_SCOPE);
    validateResult(resolve(ambiancePhase, outputName), testValueStep);
    validateResult(resolve(ambianceSection, outputName), testValueSection);
    validateResult(resolve(ambianceStep, outputName), testValueSection);

    assertThatThrownBy(()
                           -> executionSweepingOutputService.consume(ambianceSection, "randomOutputName",
                               DummySweepingOutput.builder().test("randomTestValue").build(), "RANDOM"))
        .isInstanceOf(GroupNotFoundException.class);
  }

  private void validateResult(SweepingOutput foundOutput, String testValue) {
    assertThat(foundOutput).isNotNull();
    assertThat(foundOutput).isInstanceOf(DummySweepingOutput.class);

    DummySweepingOutput dummySweepingOutput = (DummySweepingOutput) foundOutput;
    assertThat(dummySweepingOutput.getTest()).isEqualTo(testValue);
  }

  private Ambiance prepareStepAmbiance(Ambiance ambianceSection) {
    return AmbianceUtils.cloneForChild(ambianceSection,
        Level.newBuilder()
            .setRuntimeId(STEP_RUNTIME_ID)
            .setSetupId(STEP_SETUP_ID)
            .setStepType(StepType.newBuilder().setType("SHELL_SCRIPT").build())
            .build());
  }

  @Test
  @RealMongo
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void shouldTestSaveAndFindForNull() {
    Ambiance ambiance = AmbianceTestUtils.buildAmbiance();
    String outputName = "outcomeName";
    executionSweepingOutputService.consume(ambiance, outputName, null, null);

    SweepingOutput output = resolve(ambiance, outputName);
    assertThat(output).isNull();
  }

  private SweepingOutput resolve(Ambiance ambiance, String outputName) {
    return executionSweepingOutputService.resolve(ambiance, RefObjectUtil.getSweepingOutputRefObject(outputName));
  }
}
