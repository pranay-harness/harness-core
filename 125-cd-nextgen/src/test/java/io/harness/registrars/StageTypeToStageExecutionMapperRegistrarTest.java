package io.harness.registrars;

import static io.harness.rule.OwnerRule.SAHIL;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.cdng.CDNGBaseTest;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.reflections.Reflections;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class StageTypeToStageExecutionMapperRegistrarTest extends CDNGBaseTest {
  @Inject Map<String, StageTypeToStageExecutionMapperRegistrar> stageTypeToStageExecutionMapperRegistrarMap;

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testAllRegistrarsAreRegistered() {
    Set<String> fieldRegistrarClasses = new HashSet<>();

    Reflections reflections = new Reflections("io.harness.registrars");
    for (Class clazz : reflections.getSubTypesOf(StageTypeToStageExecutionMapperRegistrar.class)) {
      fieldRegistrarClasses.add(clazz.getName());
    }
    assertThat(stageTypeToStageExecutionMapperRegistrarMap.keySet()).isEqualTo(fieldRegistrarClasses);
  }
}
