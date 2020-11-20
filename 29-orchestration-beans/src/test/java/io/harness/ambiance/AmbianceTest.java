package io.harness.ambiance;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.PRASHANT;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;

import io.harness.OrchestrationBeansTestBase;
import io.harness.category.element.UnitTests;
import io.harness.pms.ambiance.Level;
import io.harness.pms.steps.StepType;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.ArrayList;
import java.util.List;

public class AmbianceTest extends OrchestrationBeansTestBase {
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
  public void shouldTestAddLevelExecution() {
    String setupId = generateUuid();
    String runtimeId = generateUuid();
    Level stepLevel = Level.newBuilder().setRuntimeId(runtimeId).setSetupId(setupId).build();
    Ambiance ambiance = buildAmbiance();
    assertThat(ambiance.getLevels()).hasSize(2);
    ambiance.addLevel(stepLevel);
    assertThat(ambiance.getLevels()).hasSize(3);
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestObtainCurrentRuntimeId() {
    Ambiance ambiance = buildAmbiance();
    assertThat(ambiance.obtainCurrentRuntimeId()).isEqualTo(SECTION_RUNTIME_ID);
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
    return Ambiance.builder()
        .planExecutionId(EXECUTION_INSTANCE_ID)
        .setupAbstractions(ImmutableMap.of("accountId", ACCOUNT_ID, "appId", APP_ID))
        .levels(levels)
        .build();
  }
}
