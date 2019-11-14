package io.harness;

import static io.harness.rule.OwnerRule.UNKNOWN;

import com.google.common.collect.ImmutableSet;

import io.harness.category.element.UnitTests;
import io.harness.morphia.MorphiaModule;
import io.harness.rule.OwnerRule.Owner;
import io.harness.serializer.morphia.OrchestrationMorphiaRegistrar;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class OrchestrationMorphiaClassesTest extends CategoryTest {
  @Test
  @Owner(emails = UNKNOWN)
  @Category(UnitTests.class)
  public void testPackage() {
    new OrchestrationMorphiaRegistrar().testClassesModule();
  }

  @Test
  @Owner(emails = UNKNOWN)
  @Category(UnitTests.class)
  public void testOrchestrationSearchAndList() {
    new MorphiaModule().testAutomaticSearch(ImmutableSet.<Class>builder().build());
  }

  @Test
  @Owner(emails = UNKNOWN)
  @Category(UnitTests.class)
  public void testOrchestrationImplementationClassesModule() {
    new OrchestrationMorphiaRegistrar().testImplementationClassesModule();
  }
}
