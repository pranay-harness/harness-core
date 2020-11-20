package io.harness.registrars;

import static io.harness.rule.OwnerRule.BRIJESH;
import static io.harness.rule.OwnerRule.PRASHANT;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.OrchestrationTestBase;
import io.harness.category.element.UnitTests;
import io.harness.registries.registrar.FacilitatorRegistrar;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.reflections.Reflections;

import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class OrchestrationFacilitatorRegistrarTest extends OrchestrationTestBase {
  @Inject OrchestrationFacilitatorRegistrar orchestrationFacilitatorRegistrar;
  @Inject Map<String, FacilitatorRegistrar> facilitatorRegistrars;

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestRegister() {
    orchestrationFacilitatorRegistrar.testClassesModule();
  }

  @Test
  @Owner(developers = BRIJESH)
  @Category(UnitTests.class)
  public void testAllRegistrarsAreRegistered()
      throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
    Set<String> facilitatorRegistrarClasses = new HashSet<>();

    Reflections reflections = new Reflections("io.harness.registrars");
    for (Class clazz : reflections.getSubTypesOf(FacilitatorRegistrar.class)) {
      facilitatorRegistrarClasses.add(clazz.getName());
    }
    assertThat(facilitatorRegistrars.keySet()).isEqualTo(facilitatorRegistrarClasses);
  }
}
