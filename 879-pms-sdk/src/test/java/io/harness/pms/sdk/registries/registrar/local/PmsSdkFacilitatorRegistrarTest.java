package io.harness.pms.sdk.registries.registrar.local;

import static io.harness.rule.OwnerRule.BRIJESH;
import static io.harness.rule.OwnerRule.PRASHANT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.pms.sdk.PmsSdkTestBase;
import io.harness.pms.sdk.registries.registrar.FacilitatorRegistrar;
import io.harness.pms.sdk.registries.registrar.local.PmsSdkFacilitatorRegistrar;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.reflections.Reflections;

public class PmsSdkFacilitatorRegistrarTest extends PmsSdkTestBase {
  @Inject Map<String, FacilitatorRegistrar> facilitatorRegistrars;

  @Test
  @Owner(developers = BRIJESH)
  @Category(UnitTests.class)
  public void testAllRegistrarsAreRegistered() {
    Set<String> facilitatorRegistrarClasses = new HashSet<>();

    Reflections reflections = new Reflections("io.harness");
    for (Class clazz : reflections.getSubTypesOf(FacilitatorRegistrar.class)) {
      facilitatorRegistrarClasses.add(clazz.getName());
    }
    assertThat(facilitatorRegistrars.keySet()).isEqualTo(facilitatorRegistrarClasses);
  }
}
