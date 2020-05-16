package io.harness.ambiance;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.PRASHANT;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;

import io.harness.OrchestrationBeansTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.ArrayList;
import java.util.List;

public class AmbianceTest extends OrchestrationBeansTest {
  private static final String ACCOUNT_ID = generateUuid();
  private static final String APP_ID = generateUuid();
  private static final String EXECUTION_INSTANCE_ID = generateUuid();
  private static final String PHASE_RUNTIME_ID = generateUuid();
  private static final String PHASE_SETUP_ID = generateUuid();
  private static final String SECTION_RUNTIME_ID = generateUuid();
  private static final String SECTION_SETUP_ID = generateUuid();

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestDeepCopy() {
    Ambiance ambiance = buildAmbiance();
    Ambiance copy = ambiance.deepCopy();

    assertThat(copy).isNotNull();
    assertThat(System.identityHashCode(copy.getSetupAbstractions()))
        .isNotEqualTo(System.identityHashCode(ambiance.getSetupAbstractions()));
    assertThat(System.identityHashCode(copy.getLevelExecutions()))
        .isNotEqualTo(System.identityHashCode(ambiance.getLevelExecutions()));
    assertThat(copy.getSetupAbstractions()).isEqualTo(ambiance.getSetupAbstractions());
    assertThat(copy.getLevelExecutions()).isEqualTo(ambiance.getLevelExecutions());
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestAddLevelExecution() {
    String setupId = generateUuid();
    String runtimeId = generateUuid();
    LevelExecution stepLevelExecution = LevelExecution.builder().runtimeId(runtimeId).setupId(setupId).build();
    Ambiance ambiance = buildAmbiance();
    assertThat(ambiance.getLevelExecutions()).hasSize(2);
    ambiance.addLevelExecution(stepLevelExecution);
    assertThat(ambiance.getLevelExecutions()).hasSize(3);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestCloneForNext() {
    Ambiance ambiance = buildAmbiance();
    assertThat(ambiance.getLevelExecutions()).hasSize(2);

    Ambiance clonedAmbiance = ambiance.cloneForFinish();
    assertThat(clonedAmbiance).isNotNull();
    assertThat(clonedAmbiance.getLevelExecutions()).hasSize(1);
    assertThat(clonedAmbiance.getPlanExecutionId()).isEqualTo(EXECUTION_INSTANCE_ID);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestCloneForChild() {
    Ambiance ambiance = buildAmbiance();
    assertThat(ambiance.getLevelExecutions()).hasSize(2);

    Ambiance clonedAmbiance = ambiance.cloneForChild();
    assertThat(clonedAmbiance).isNotNull();
    assertThat(clonedAmbiance.getLevelExecutions()).hasSize(2);
    assertThat(clonedAmbiance.getPlanExecutionId()).isEqualTo(EXECUTION_INSTANCE_ID);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestObtainCurrentRuntimeId() {
    Ambiance ambiance = buildAmbiance();
    assertThat(ambiance.obtainCurrentRuntimeId()).isEqualTo(SECTION_RUNTIME_ID);
  }

  private Ambiance buildAmbiance() {
    LevelExecution phaseLevelExecution =
        LevelExecution.builder().runtimeId(PHASE_RUNTIME_ID).setupId(PHASE_SETUP_ID).build();
    LevelExecution sectionLevelExecution =
        LevelExecution.builder().runtimeId(SECTION_RUNTIME_ID).setupId(SECTION_SETUP_ID).build();
    List<LevelExecution> levelExecutions = new ArrayList<>();
    levelExecutions.add(phaseLevelExecution);
    levelExecutions.add(sectionLevelExecution);
    return Ambiance.builder()
        .planExecutionId(EXECUTION_INSTANCE_ID)
        .setupAbstractions(
            ImmutableMap.<String, String>builder().put("accountId", ACCOUNT_ID).put("appId", APP_ID).build())
        .levelExecutions(levelExecutions)
        .build();
  }
}