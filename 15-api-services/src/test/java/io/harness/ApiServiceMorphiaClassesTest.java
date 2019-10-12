package io.harness;

import com.google.common.collect.ImmutableSet;

import io.harness.category.element.UnitTests;
import io.harness.morphia.MorphiaModule;
import io.harness.serializer.morphia.ApiServiceMorphiaRegistrar;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class ApiServiceMorphiaClassesTest extends CategoryTest {
  @Test
  @Category(UnitTests.class)
  public void testApiServiceClassesModule() {
    new ApiServiceMorphiaRegistrar().testClassesModule();
  }

  @Test
  @Category(UnitTests.class)
  public void testApiServiceSearchAndList() {
    new MorphiaModule().testAutomaticSearch(ImmutableSet.<Class>builder().build());
  }

  @Test
  @Category(UnitTests.class)
  public void testApiServiceImplementationClassesModule() {
    new ApiServiceMorphiaRegistrar().testImplementationClassesModule();
  }
}
