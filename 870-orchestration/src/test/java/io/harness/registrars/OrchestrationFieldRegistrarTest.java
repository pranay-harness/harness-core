package io.harness.registrars;

import static io.harness.rule.OwnerRule.GARVIT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.OrchestrationTestBase;
import io.harness.category.element.UnitTests;
import io.harness.pms.sdk.registries.registrar.OrchestrationFieldRegistrar;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.reflections.Reflections;

public class OrchestrationFieldRegistrarTest extends OrchestrationTestBase {
  @Inject Map<String, OrchestrationFieldRegistrar> orchestrationFieldRegistrarMap;

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testAllRegistrarsAreRegistered()
      throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
    Set<String> fieldRegistrarClasses = new HashSet<>();

    Reflections reflections = new Reflections("io.harness");
    for (Class clazz : reflections.getSubTypesOf(OrchestrationFieldRegistrar.class)) {
      fieldRegistrarClasses.add(clazz.getName());
    }
    assertThat(orchestrationFieldRegistrarMap.keySet()).isEqualTo(fieldRegistrarClasses);
  }
}
