package io.harness.registrars;

import static io.harness.rule.OwnerRule.BRIJESH;
import static io.harness.rule.OwnerRule.PRASHANT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.OrchestrationTestBase;
import io.harness.category.element.UnitTests;
import io.harness.pms.sdk.registries.registrar.AdviserRegistrar;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.reflections.Reflections;

public class OrchestrationAdviserRegistrarTest extends OrchestrationTestBase {
  @Inject Map<String, AdviserRegistrar> adviserRegistrars;

  @Test
  @Owner(developers = BRIJESH)
  @Category(UnitTests.class)
  public void testAllRegistrarsAreRegistered() {
    Set<String> adviserRegistrarClasses = new HashSet<>();

    Reflections reflections = new Reflections("io.harness");
    for (Class clazz : reflections.getSubTypesOf(AdviserRegistrar.class)) {
      adviserRegistrarClasses.add(clazz.getName());
    }
    assertThat(adviserRegistrars.keySet()).isEqualTo(adviserRegistrarClasses);
  }
}
