package io.harness;

import static io.harness.rule.OwnerRule.HARSH;

import com.google.common.collect.ImmutableSet;

import io.harness.category.element.UnitTests;
import io.harness.morphia.MorphiaModule;
import io.harness.rule.Owner;
import io.harness.serializer.morphia.CIBeansMorphiaRegistrar;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class CIManagerMorphiaClassesTest extends CategoryTest {
  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  public void testCIClassesModule() {
    new CIBeansMorphiaRegistrar().testClassesModule();
  }

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  @Ignore("TODO: have to register classes during implementation")
  public void testCISearchAndList() {
    new MorphiaModule().testAutomaticSearch(ImmutableSet.<Class>builder().build());
  }

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  public void testCIImplementationClassesModule() {
    new CIBeansMorphiaRegistrar().testImplementationClassesModule();
  }
}