package io.harness.walktree.registries.visitorfield;

import static io.harness.rule.OwnerRule.ARCHIT;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.WalkTreeBaseTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.walktree.registries.registrars.VisitableFieldRegistrar;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.reflections.Reflections;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class VisitorFieldRegistrarTest extends WalkTreeBaseTest {
  @Inject Map<String, VisitableFieldRegistrar> visitableFieldRegistrarMap;

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testAllRegistrarsAreRegistered() {
    Set<String> fieldRegistrarClasses = new HashSet<>();

    Reflections reflections = new Reflections("io.harness.registrars");
    for (Class clazz : reflections.getSubTypesOf(VisitableFieldRegistrar.class)) {
      fieldRegistrarClasses.add(clazz.getName());
    }
    assertThat(visitableFieldRegistrarMap.keySet()).isEqualTo(fieldRegistrarClasses);
  }
}
