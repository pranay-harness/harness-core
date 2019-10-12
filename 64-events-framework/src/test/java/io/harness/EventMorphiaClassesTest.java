package io.harness;

import com.google.common.collect.ImmutableSet;

import io.harness.category.element.UnitTests;
import io.harness.morphia.MorphiaModule;
import io.harness.serializer.morphia.EventMorphiaRegistrar;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class EventMorphiaClassesTest extends CategoryTest {
  @Test
  @Category(UnitTests.class)
  public void testEventClassesModule() {
    new EventMorphiaRegistrar().testClassesModule();
  }

  @Test
  @Category(UnitTests.class)
  public void testEventSearchAndList() {
    new MorphiaModule().testAutomaticSearch(ImmutableSet.<Class>builder().build());
  }

  @Test
  @Category(UnitTests.class)
  public void testEventImplementationClassesModule() {
    new EventMorphiaRegistrar().testImplementationClassesModule();
  }
}