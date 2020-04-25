package io.harness.state.io.ambiance;

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
  public void shouldTestAddLevel() {
    String setupId = generateUuid();
    String runtimeId = generateUuid();
    LevelExecution phaseLevelExecution =
        LevelExecution.builder().levelKey("PHASE").runtimeId(runtimeId).setupId(setupId).build();
    Ambiance ambiance = buildAmbiance();
    assertThat(ambiance.getLevelExecutions()).hasSize(2);
    ambiance.addLevel(phaseLevelExecution);
    assertThat(ambiance.getLevelExecutions()).hasSize(1);
    assertThat(ambiance.getLevelExecutions().get(0)).isEqualTo(phaseLevelExecution);
  }

  private Ambiance buildAmbiance() {
    LevelExecution phaseLevelExecution =
        LevelExecution.builder().levelKey("PHASE").runtimeId(generateUuid()).setupId(generateUuid()).build();
    LevelExecution sectionLevelExecution =
        LevelExecution.builder().levelKey("SECTION").runtimeId(generateUuid()).setupId(generateUuid()).build();
    List<LevelExecution> levelExecutions = new ArrayList<>();
    levelExecutions.add(phaseLevelExecution);
    levelExecutions.add(sectionLevelExecution);
    return Ambiance.builder()
        .executionInstanceId(generateUuid())
        .setupAbstractions(ImmutableMap.<String, String>builder()
                               .put("accountId", generateUuid())
                               .put("appId", generateUuid())
                               .build())
        .levelExecutions(levelExecutions)
        .build();
  }
}