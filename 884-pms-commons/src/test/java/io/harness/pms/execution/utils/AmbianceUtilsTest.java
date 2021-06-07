package io.harness.pms.execution.utils;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.PRASHANT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.ambiance.Level;
import io.harness.pms.contracts.steps.StepType;
import io.harness.rule.Owner;

import com.google.common.collect.ImmutableMap;
import com.google.protobuf.InvalidProtocolBufferException;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class AmbianceUtilsTest extends CategoryTest {
  private static final String ACCOUNT_ID = generateUuid();
  private static final String APP_ID = generateUuid();
  private static final String PLAN_EXECUTION_ID = generateUuid();
  private static final String PLAN_ID = generateUuid();
  private static final String PHASE_RUNTIME_ID = generateUuid();
  private static final String PHASE_SETUP_ID = generateUuid();
  private static final String SECTION_RUNTIME_ID = generateUuid();
  private static final String SECTION_SETUP_ID = generateUuid();

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void cloneForFinish() {
    Ambiance ambiance = buildAmbiance();
    assertThat(ambiance.getLevelsList()).hasSize(2);

    Ambiance clonedAmbiance = AmbianceUtils.cloneForFinish(ambiance);
    assertThat(clonedAmbiance).isNotNull();
    assertThat(clonedAmbiance.getLevelsList()).hasSize(1);
    assertThat(clonedAmbiance.getPlanExecutionId()).isEqualTo(PLAN_EXECUTION_ID);
    assertThat(clonedAmbiance.getPlanId()).isEqualTo(PLAN_ID);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void cloneForChild() {
    Ambiance ambiance = buildAmbiance();
    assertThat(ambiance.getLevelsList()).hasSize(2);

    Ambiance clonedAmbiance = AmbianceUtils.cloneForChild(ambiance);
    assertThat(clonedAmbiance).isNotNull();
    assertThat(clonedAmbiance.getLevelsList()).hasSize(2);
    assertThat(clonedAmbiance.getPlanExecutionId()).isEqualTo(PLAN_EXECUTION_ID);
    assertThat(clonedAmbiance.getPlanId()).isEqualTo(PLAN_ID);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestClone() {
    Ambiance ambiance = buildAmbiance();
    assertThat(ambiance.getLevelsList()).hasSize(2);

    Ambiance clonedAmbiance = AmbianceUtils.clone(ambiance, 0);
    assertThat(clonedAmbiance).isNotNull();
    assertThat(clonedAmbiance.getLevelsList()).hasSize(0);
    assertThat(clonedAmbiance.getPlanExecutionId()).isEqualTo(PLAN_EXECUTION_ID);
    assertThat(clonedAmbiance.getPlanId()).isEqualTo(PLAN_ID);

    clonedAmbiance = AmbianceUtils.clone(ambiance, 1);
    assertThat(clonedAmbiance).isNotNull();
    assertThat(clonedAmbiance.getLevelsList()).hasSize(1);
    assertThat(clonedAmbiance.getPlanExecutionId()).isEqualTo(PLAN_EXECUTION_ID);
    assertThat(clonedAmbiance.getPlanId()).isEqualTo(PLAN_ID);

    clonedAmbiance = AmbianceUtils.clone(ambiance, 2);
    assertThat(clonedAmbiance).isNotNull();
    assertThat(clonedAmbiance.getLevelsList()).hasSize(2);
    assertThat(clonedAmbiance.getPlanExecutionId()).isEqualTo(PLAN_EXECUTION_ID);
    assertThat(clonedAmbiance.getPlanId()).isEqualTo(PLAN_ID);

    clonedAmbiance = AmbianceUtils.clone(ambiance, 3);
    assertThat(clonedAmbiance).isNotNull();
    assertThat(clonedAmbiance.getLevelsList()).hasSize(2);
    assertThat(clonedAmbiance.getPlanExecutionId()).isEqualTo(PLAN_EXECUTION_ID);
    assertThat(clonedAmbiance.getPlanId()).isEqualTo(PLAN_ID);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestDeepCopy() throws InvalidProtocolBufferException {
    Ambiance ambiance = buildAmbiance();
    Ambiance copy = AmbianceUtils.deepCopy(ambiance);

    assertThat(copy).isNotNull();
    assertThat(System.identityHashCode(copy.getLevelsList()))
        .isNotEqualTo(System.identityHashCode(ambiance.getLevelsList()));
    assertThat(copy.getSetupAbstractionsMap()).isEqualTo(ambiance.getSetupAbstractionsMap());
    assertThat(copy.getLevelsList()).isEqualTo(ambiance.getLevelsList());
  }

  private Ambiance buildAmbiance() {
    Level phaseLevel = Level.newBuilder()
                           .setRuntimeId(PHASE_RUNTIME_ID)
                           .setSetupId(PHASE_SETUP_ID)
                           .setStepType(StepType.newBuilder().setType("PHASE").build())
                           .build();
    Level sectionLevel = Level.newBuilder()
                             .setRuntimeId(SECTION_RUNTIME_ID)
                             .setSetupId(SECTION_SETUP_ID)
                             .setStepType(StepType.newBuilder().setType("SECTION").build())
                             .build();
    List<Level> levels = new ArrayList<>();
    levels.add(phaseLevel);
    levels.add(sectionLevel);
    return Ambiance.newBuilder()
        .setPlanExecutionId(PLAN_EXECUTION_ID)
        .setPlanId(PLAN_ID)
        .putAllSetupAbstractions(ImmutableMap.of("accountId", ACCOUNT_ID, "appId", APP_ID))
        .addAllLevels(levels)
        .build();
  }
}
