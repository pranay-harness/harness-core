package io.harness;

import com.google.common.collect.ImmutableSet;

import io.harness.category.element.UnitTests;
import io.harness.morphia.MorphiaModule;
import io.harness.serializer.morphia.DelegateMorphiaRegistrar;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class DelegateMorphiaClassesTest extends CategoryTest {
  @Test
  @Category(UnitTests.class)
  public void testDelegateClassesModule() {
    new DelegateMorphiaRegistrar().testClassesModule();
  }

  @Test
  @Category(UnitTests.class)
  public void testDelegateSearchAndList() {
    new MorphiaModule().testAutomaticSearch(ImmutableSet.<Class>builder().build());
  }

  @Test
  @Category(UnitTests.class)
  public void testDelegateImplementationClassesModule() {
    new DelegateMorphiaRegistrar().testImplementationClassesModule();
  }
}