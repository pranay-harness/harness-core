package io.harness;

import com.google.common.collect.ImmutableSet;

import io.harness.category.element.UnitTests;
import io.harness.morphia.MorphiaModule;
import io.harness.serializer.morphia.EventServerMorphiaRegistrar;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class EventServerMorphiaClassesTest extends CategoryTest {
  @Test
  @Category(UnitTests.class)
  public void testEventServerClassesModule() {
    new EventServerMorphiaRegistrar().testClassesModule();
  }

  @Test
  @Category(UnitTests.class)
  public void testEventServerSearchAndList() {
    new MorphiaModule().testAutomaticSearch(ImmutableSet.<Class>builder().build());
  }

  @Test
  @Category(UnitTests.class)
  public void testEventImplementationClassesModule() {
    new EventServerMorphiaRegistrar().testImplementationClassesModule();
  }
}