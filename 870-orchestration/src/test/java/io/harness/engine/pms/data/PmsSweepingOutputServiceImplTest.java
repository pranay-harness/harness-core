package io.harness.engine.pms.data;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.PRASHANT;
import static io.harness.rule.OwnerRule.PRASHANTSHARMA;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.OrchestrationTestBase;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.data.ExecutionSweepingOutputInstance;
import io.harness.engine.outputs.SweepingOutputException;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.ambiance.Level;
import io.harness.pms.contracts.steps.StepCategory;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.sdk.core.resolver.GroupNotFoundException;
import io.harness.pms.sdk.core.resolver.RefObjectUtils;
import io.harness.pms.sdk.core.resolver.ResolverUtils;
import io.harness.pms.serializer.recaster.RecastOrchestrationUtils;
import io.harness.rule.Owner;
import io.harness.utils.AmbianceTestUtils;
import io.harness.utils.DummySweepingOutput;

import com.google.inject.Inject;
import java.util.List;
import org.bson.Document;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(HarnessTeam.PIPELINE)
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

    pmsSweepingOutputService.consume(ambianceSection, outputName,
        RecastOrchestrationUtils.toJson(DummySweepingOutput.builder().test(testValueSection).build()), null);
    validateResult(resolve(ambianceSection, outputName), testValueSection);
    validateResult(resolve(ambianceStep, outputName), testValueSection);
    assertThatThrownBy(() -> resolve(ambiancePhase, outputName)).isInstanceOf(SweepingOutputException.class);

    pmsSweepingOutputService.consume(ambianceStep, outputName,
        RecastOrchestrationUtils.toJson(DummySweepingOutput.builder().test(testValueStep).build()), null);
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

    pmsSweepingOutputService.consumeInternal(ambianceSection, outputName,
        RecastOrchestrationUtils.toJson(DummySweepingOutput.builder().test(testValueSection).build()), 2, null);
    validateResult(resolve(ambianceSection, outputName), testValueSection);
    validateResult(resolve(ambianceStep, outputName), testValueSection);
    assertThatThrownBy(() -> resolve(ambiancePhase, outputName)).isInstanceOf(SweepingOutputException.class);

    pmsSweepingOutputService.consumeInternal(ambianceStep, outputName,
        RecastOrchestrationUtils.toJson(DummySweepingOutput.builder().test(testValueStep).build()), 0, null);
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

    pmsSweepingOutputService.consume(ambianceSection, outputName,
        RecastOrchestrationUtils.toJson(DummySweepingOutput.builder().test(testValueSection).build()), "SECTION");
    validateResult(resolve(ambianceSection, outputName), testValueSection);
    validateResult(resolve(ambianceStep, outputName), testValueSection);
    assertThatThrownBy(() -> resolve(ambiancePhase, outputName)).isInstanceOf(SweepingOutputException.class);

    pmsSweepingOutputService.consume(ambianceStep, outputName,
        RecastOrchestrationUtils.toJson(DummySweepingOutput.builder().test(testValueStep).build()),
        ResolverUtils.GLOBAL_GROUP_SCOPE);
    validateResult(resolve(ambiancePhase, outputName), testValueStep);
    validateResult(resolve(ambianceSection, outputName), testValueSection);
    validateResult(resolve(ambianceStep, outputName), testValueSection);

    assertThatThrownBy(
        ()
            -> pmsSweepingOutputService.consume(ambianceSection, "randomOutputName",
                RecastOrchestrationUtils.toJson(DummySweepingOutput.builder().test("randomTestValue").build()),
                "RANDOM"))
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
            .setStepType(StepType.newBuilder().setType("SHELL_SCRIPT").setStepCategory(StepCategory.STEP).build())
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

  @Test
  @Owner(developers = PRASHANTSHARMA)
  @Category(UnitTests.class)
  public void testFetchOutcomeInstanceByRuntimeId() {
    List<ExecutionSweepingOutputInstance> result = pmsSweepingOutputService.fetchOutcomeInstanceByRuntimeId("abc");
    assertThat(result.size()).isEqualTo(0);
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