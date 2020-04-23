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
    assertThat(System.identityHashCode(copy.getLevels())).isNotEqualTo(System.identityHashCode(ambiance.getLevels()));
    assertThat(copy.getSetupAbstractions()).isEqualTo(ambiance.getSetupAbstractions());
    assertThat(copy.getLevels()).isEqualTo(ambiance.getLevels());
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestAddLevel() {
    String setupId = generateUuid();
    String runtimeId = generateUuid();
    Level phaseLevel = Level.builder().levelKey("PHASE").runtimeId(runtimeId).setupId(setupId).build();
    Ambiance ambiance = buildAmbiance();
    assertThat(ambiance.getLevels()).hasSize(2);
    ambiance.addLevel(phaseLevel);
    assertThat(ambiance.getLevels()).hasSize(1);
    assertThat(ambiance.getLevels().get(0)).isEqualTo(phaseLevel);
  }

  private Ambiance buildAmbiance() {
    Level phaseLevel = Level.builder().levelKey("PHASE").runtimeId(generateUuid()).setupId(generateUuid()).build();
    Level sectionLevel = Level.builder().levelKey("SECTION").runtimeId(generateUuid()).setupId(generateUuid()).build();
    List<Level> levels = new ArrayList<>();
    levels.add(phaseLevel);
    levels.add(sectionLevel);
    return Ambiance.builder()
        .executionInstanceId(generateUuid())
        .setupAbstractions(ImmutableMap.<String, String>builder()
                               .put("accountId", generateUuid())
                               .put("appId", generateUuid())
                               .build())
        .levels(levels)
        .build();
  }
}