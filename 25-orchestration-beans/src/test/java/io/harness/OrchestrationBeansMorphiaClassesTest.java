package io.harness;

import static io.harness.rule.OwnerRule.PRASHANT;

import com.google.common.collect.ImmutableSet;

import io.harness.category.element.UnitTests;
import io.harness.morphia.MorphiaModule;
import io.harness.rule.Owner;
import io.harness.serializer.morphia.OrchestrationBeansMorphiaRegistrar;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class OrchestrationBeansMorphiaClassesTest extends CategoryTest {
  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void testPackage() {
    new OrchestrationBeansMorphiaRegistrar().testClassesModule();
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void testOrchestrationSearchAndList() {
    new MorphiaModule().testAutomaticSearch(ImmutableSet.<Class>builder().build());
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void testOrchestrationImplementationClassesModule() {
    new OrchestrationBeansMorphiaRegistrar().testImplementationClassesModule();
  }
}
