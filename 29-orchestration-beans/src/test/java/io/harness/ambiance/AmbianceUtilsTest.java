package io.harness.ambiance;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.PRASHANT;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import io.harness.OrchestrationBeansModuleListProvider;
import io.harness.OrchestrationBeansTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.runners.GuiceRunner;
import io.harness.runners.ModuleProvider;
import io.harness.state.StepType;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

@RunWith(GuiceRunner.class)
@ModuleProvider(OrchestrationBeansModuleListProvider.class)
public class AmbianceUtilsTest extends OrchestrationBeansTest {
  private static final String ACCOUNT_ID = generateUuid();
  private static final String APP_ID = generateUuid();
  private static final String EXECUTION_INSTANCE_ID = generateUuid();
  private static final String PHASE_RUNTIME_ID = generateUuid();
  private static final String PHASE_SETUP_ID = generateUuid();
  private static final String SECTION_RUNTIME_ID = generateUuid();
  private static final String SECTION_SETUP_ID = generateUuid();

  @Inject AmbianceUtils ambianceUtils;

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void cloneForFinish() {
    Ambiance ambiance = buildAmbiance();
    assertThat(ambiance.getLevels()).hasSize(2);

    Ambiance clonedAmbiance = ambianceUtils.cloneForFinish(ambiance);
    assertThat(clonedAmbiance).isNotNull();
    assertThat(clonedAmbiance.getLevels()).hasSize(1);
    assertThat(clonedAmbiance.getPlanExecutionId()).isEqualTo(EXECUTION_INSTANCE_ID);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void cloneForChild() {
    Ambiance ambiance = buildAmbiance();
    assertThat(ambiance.getLevels()).hasSize(2);

    Ambiance clonedAmbiance = ambianceUtils.cloneForChild(ambiance);
    assertThat(clonedAmbiance).isNotNull();
    assertThat(clonedAmbiance.getLevels()).hasSize(2);
    assertThat(clonedAmbiance.getPlanExecutionId()).isEqualTo(EXECUTION_INSTANCE_ID);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestClone() {
    Ambiance ambiance = buildAmbiance();
    assertThat(ambiance.getLevels()).hasSize(2);

    Ambiance clonedAmbiance = ambianceUtils.clone(ambiance, 0);
    assertThat(clonedAmbiance).isNotNull();
    assertThat(clonedAmbiance.getLevels()).hasSize(0);
    assertThat(clonedAmbiance.getPlanExecutionId()).isEqualTo(EXECUTION_INSTANCE_ID);

    clonedAmbiance = ambianceUtils.clone(ambiance, 1);
    assertThat(clonedAmbiance).isNotNull();
    assertThat(clonedAmbiance.getLevels()).hasSize(1);
    assertThat(clonedAmbiance.getPlanExecutionId()).isEqualTo(EXECUTION_INSTANCE_ID);

    clonedAmbiance = ambianceUtils.clone(ambiance, 2);
    assertThat(clonedAmbiance).isNotNull();
    assertThat(clonedAmbiance.getLevels()).hasSize(2);
    assertThat(clonedAmbiance.getPlanExecutionId()).isEqualTo(EXECUTION_INSTANCE_ID);

    clonedAmbiance = ambianceUtils.clone(ambiance, 3);
    assertThat(clonedAmbiance).isNotNull();
    assertThat(clonedAmbiance.getLevels()).hasSize(2);
    assertThat(clonedAmbiance.getPlanExecutionId()).isEqualTo(EXECUTION_INSTANCE_ID);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestDeepCopy() {
    Ambiance ambiance = buildAmbiance();
    Ambiance copy = ambianceUtils.deepCopy(ambiance);

    assertThat(copy).isNotNull();
    assertThat(System.identityHashCode(copy.getLevels())).isNotEqualTo(System.identityHashCode(ambiance.getLevels()));
    assertThat(copy.getSetupAbstractions()).isEqualTo(ambiance.getSetupAbstractions());
    assertThat(copy.getLevels()).isEqualTo(ambiance.getLevels());
  }

  private Ambiance buildAmbiance() {
    Level phaseLevel = Level.builder()
                           .runtimeId(PHASE_RUNTIME_ID)
                           .setupId(PHASE_SETUP_ID)
                           .stepType(StepType.builder().type("PHASE").build())
                           .build();
    Level sectionLevel = Level.builder()
                             .runtimeId(SECTION_RUNTIME_ID)
                             .setupId(SECTION_SETUP_ID)
                             .stepType(StepType.builder().type("SECTION").build())
                             .build();
    List<Level> levels = new ArrayList<>();
    levels.add(phaseLevel);
    levels.add(sectionLevel);
    return Ambiance.builder()
        .planExecutionId(EXECUTION_INSTANCE_ID)
        .setupAbstractions(ImmutableMap.of("accountId", ACCOUNT_ID, "appId", APP_ID))
        .levels(levels)
        .build();
  }
}