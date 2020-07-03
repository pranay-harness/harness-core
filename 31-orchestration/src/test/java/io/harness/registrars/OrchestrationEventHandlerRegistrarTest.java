package io.harness.registrars;

import static io.harness.rule.OwnerRule.BRIJESH;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.OrchestrationTest;
import io.harness.category.element.UnitTests;
import io.harness.registries.registrar.OrchestrationEventHandlerRegistrar;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.reflections.Reflections;

import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class OrchestrationEventHandlerRegistrarTest extends OrchestrationTest {
  @Inject Map<String, OrchestrationEventHandlerRegistrar> orchestrationEventHandlerRegistrars;

  @Test
  @Owner(developers = BRIJESH)
  @Category(UnitTests.class)
  public void testAllRegistrarsAreRegistered()
      throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
    Set<String> orchestrationEventHandlerRegistrarsClasses = new HashSet<>();

    Reflections reflections = new Reflections("io.harness.registrars");
    for (Class clazz : reflections.getSubTypesOf(OrchestrationEventHandlerRegistrar.class)) {
      orchestrationEventHandlerRegistrarsClasses.add(clazz.getName());
    }
    assertThat(orchestrationEventHandlerRegistrars.keySet()).isEqualTo(orchestrationEventHandlerRegistrarsClasses);
  }
}