package io.harness.registrars;

import static io.harness.rule.OwnerRule.BRIJESH;
import static io.harness.rule.OwnerRule.GARVIT;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.OrchestrationTestBase;
import io.harness.category.element.UnitTests;
import io.harness.registries.registrar.ResolverRegistrar;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.reflections.Reflections;

import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class OrchestrationResolverRegistrarTest extends OrchestrationTestBase {
  @Inject OrchestrationResolverRegistrar orchestrationResolverRegistrar;
  @Inject Map<String, ResolverRegistrar> resolverRegistrars;

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void shouldTestRegister() {
    orchestrationResolverRegistrar.testClassesModule();
  }

  @Test
  @Owner(developers = BRIJESH)
  @Category(UnitTests.class)
  public void testAllRegistrarsAreRegistered()
      throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
    Set<String> resolverRegistrarClasses = new HashSet<>();

    Reflections reflections = new Reflections("io.harness.registrars");
    for (Class clazz : reflections.getSubTypesOf(ResolverRegistrar.class)) {
      resolverRegistrarClasses.add(clazz.getName());
    }
    assertThat(resolverRegistrars.keySet()).isEqualTo(resolverRegistrarClasses);
  }
}
