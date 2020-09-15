package io.harness.registrars;

import static io.harness.rule.OwnerRule.GARVIT;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.OrchestrationTest;
import io.harness.category.element.UnitTests;
import io.harness.registries.registrar.OrchestrationFieldRegistrar;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.reflections.Reflections;

import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class OrchestrationFieldRegistrarTest extends OrchestrationTest {
  @Inject Map<String, OrchestrationFieldRegistrar> orchestrationFieldRegistrarMap;

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void shouldTestRegister() {
    new OrchestrationAdviserRegistrar().testClassesModule();
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testAllRegistrarsAreRegistered()
      throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
    Set<String> fieldRegistrarClasses = new HashSet<>();

    Reflections reflections = new Reflections("io.harness.registrars");
    for (Class clazz : reflections.getSubTypesOf(OrchestrationFieldRegistrar.class)) {
      fieldRegistrarClasses.add(clazz.getName());
    }
    assertThat(orchestrationFieldRegistrarMap.keySet()).isEqualTo(fieldRegistrarClasses);
  }
}
