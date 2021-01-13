package io.harness.registrars;

import static io.harness.rule.OwnerRule.BRIJESH;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.OrchestrationTestBase;
import io.harness.category.element.UnitTests;
import io.harness.pms.sdk.core.registries.registrar.ResolverRegistrar;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.reflections.Reflections;

public class OrchestrationResolverRegistrarTest extends OrchestrationTestBase {
  @Inject Map<String, ResolverRegistrar> resolverRegistrars;

  @Test
  @Owner(developers = BRIJESH)
  @Category(UnitTests.class)
  public void testAllRegistrarsAreRegistered()
      throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
    Set<String> resolverRegistrarClasses = new HashSet<>();

    Reflections reflections = new Reflections("io.harness");
    for (Class clazz : reflections.getSubTypesOf(ResolverRegistrar.class)) {
      resolverRegistrarClasses.add(clazz.getName());
    }
    assertThat(resolverRegistrars.keySet()).isEqualTo(resolverRegistrarClasses);
  }
}
