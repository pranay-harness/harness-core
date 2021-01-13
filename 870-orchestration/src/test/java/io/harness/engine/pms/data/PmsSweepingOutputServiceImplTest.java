package io.harness.engine.pms.data;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.PRASHANT;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.OrchestrationTestBase;
import io.harness.category.element.UnitTests;
import io.harness.engine.outputs.SweepingOutputException;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.ambiance.Level;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.sdk.core.resolver.GroupNotFoundException;
import io.harness.pms.sdk.core.resolver.RefObjectUtils;
import io.harness.pms.sdk.core.resolver.ResolverUtils;
import io.harness.rule.Owner;
import io.harness.utils.AmbianceTestUtils;
import io.harness.utils.DummySweepingOutput;

import com.google.inject.Inject;
import org.bson.Document;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class PmsSweepingOutputServiceImplTest extends OrchestrationTestBase {
  private static final String STEP_RUNTIME_ID = generateUuid();
  private static final String STEP_SETUP_ID = generateUuid();

  @Inject private PmsSweepingOutputService pmsSweepingOutputService;

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void testConsumeAndFind() {
    Ambiance ambianceSection = AmbianceTestUtils.buildAmbiance();
    Ambiance ambiancePhase = AmbianceUtils.cloneForFinish(ambianceSection);
    Ambiance ambianceStep = prepareStepAmbiance(ambianceSection);

    String outputName = "outputName";
    String testValueSection = "testSection";
    String testValueStep = "testStep";

    pmsSweepingOutputService.consume(
        ambianceSection, outputName, DummySweepingOutput.builder().test(testValueSection).build().toJson(), null);
    validateResult(resolve(ambianceSection, outputName), testValueSection);
    validateResult(resolve(ambianceStep, outputName), testValueSection);
    assertThatThrownBy(() -> resolve(ambiancePhase, outputName)).isInstanceOf(SweepingOutputException.class);

    pmsSweepingOutputService.consume(
        ambianceStep, outputName, DummySweepingOutput.builder().test(testValueStep).build().toJson(), null);
    validateResult(resolve(ambianceSection, outputName), testValueSection);
    validateResult(resolve(ambianceStep, outputName), testValueStep);
    assertThatThrownBy(() -> resolve(ambiancePhase, outputName)).isInstanceOf(SweepingOutputException.class);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void testSaveWithLevelsToKeepAndFind() {
    Ambiance ambianceSection = AmbianceTestUtils.buildAmbiance();
    Ambiance ambiancePhase = AmbianceUtils.cloneForFinish(ambianceSection);
    Ambiance ambianceStep = prepareStepAmbiance(ambianceSection);

    String outputName = "outputName";
    String testValueSection = "testSection";
    String testValueStep = "testStep";

    pmsSweepingOutputService.consumeInternal(
        ambianceSection, outputName, DummySweepingOutput.builder().test(testValueSection).build().toJson(), 2);
    validateResult(resolve(ambianceSection, outputName), testValueSection);
    validateResult(resolve(ambianceStep, outputName), testValueSection);
    assertThatThrownBy(() -> resolve(ambiancePhase, outputName)).isInstanceOf(SweepingOutputException.class);

    pmsSweepingOutputService.consumeInternal(
        ambianceStep, outputName, DummySweepingOutput.builder().test(testValueStep).build().toJson(), 0);
    validateResult(resolve(ambiancePhase, outputName), testValueStep);
    validateResult(resolve(ambianceSection, outputName), testValueSection);
    validateResult(resolve(ambianceStep, outputName), testValueSection);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void testSaveAtScopeAndFind() {
    Ambiance ambianceSection = AmbianceTestUtils.buildAmbiance();
    Ambiance ambiancePhase = AmbianceUtils.cloneForFinish(ambianceSection);
    Ambiance ambianceStep = prepareStepAmbiance(ambianceSection);

    String outputName = "outputName";
    String testValueSection = "testSection";
    String testValueStep = "testStep";

    pmsSweepingOutputService.consume(
        ambianceSection, outputName, DummySweepingOutput.builder().test(testValueSection).build().toJson(), "SECTION");
    validateResult(resolve(ambianceSection, outputName), testValueSection);
    validateResult(resolve(ambianceStep, outputName), testValueSection);
    assertThatThrownBy(() -> resolve(ambiancePhase, outputName)).isInstanceOf(SweepingOutputException.class);

    pmsSweepingOutputService.consume(ambianceStep, outputName,
        DummySweepingOutput.builder().test(testValueStep).build().toJson(), ResolverUtils.GLOBAL_GROUP_SCOPE);
    validateResult(resolve(ambiancePhase, outputName), testValueStep);
    validateResult(resolve(ambianceSection, outputName), testValueSection);
    validateResult(resolve(ambianceStep, outputName), testValueSection);

    assertThatThrownBy(()
                           -> pmsSweepingOutputService.consume(ambianceSection, "randomOutputName",
                               DummySweepingOutput.builder().test("randomTestValue").build().toJson(), "RANDOM"))
        .isInstanceOf(GroupNotFoundException.class);
  }

  private void validateResult(Document foundOutput, String testValue) {
    assertThat(foundOutput).isNotNull();
    assertThat(foundOutput.getString("test")).isEqualTo(testValue);
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
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestSaveAndFindForNull() {
    Ambiance ambiance = AmbianceTestUtils.buildAmbiance();
    String outputName = "outcomeName";
    pmsSweepingOutputService.consume(ambiance, outputName, null, null);

    Document output = resolve(ambiance, outputName);
    assertThat(output).isNull();
  }

  private Document resolve(Ambiance ambiance, String outputName) {
    String resolvedVal =
        pmsSweepingOutputService.resolve(ambiance, RefObjectUtils.getSweepingOutputRefObject(outputName));
    if (resolvedVal == null) {
      return null;
    }
    return Document.parse(resolvedVal);
  }
}