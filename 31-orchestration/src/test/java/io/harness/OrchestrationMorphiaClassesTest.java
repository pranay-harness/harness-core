package io.harness;

import com.google.common.collect.ImmutableSet;

import io.harness.category.element.UnitTests;
import io.harness.morphia.MorphiaModule;
import io.harness.serializer.morphia.OrchestrationMorphiaRegistrar;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class OrchestrationMorphiaClassesTest extends CategoryTest {
  @Test
  @Category(UnitTests.class)
  public void testPackage() {
    new OrchestrationMorphiaRegistrar().testClassesModule();
  }

  @Test
  @Category(UnitTests.class)
  public void testOrchestrationSearchAndList() {
    new MorphiaModule().testAutomaticSearch(ImmutableSet.<Class>builder().build());
  }

  @Test
  @Category(UnitTests.class)
  public void testOrchestrationImplementationClassesModule() {
    new OrchestrationMorphiaRegistrar().testImplementationClassesModule();
  }
}
