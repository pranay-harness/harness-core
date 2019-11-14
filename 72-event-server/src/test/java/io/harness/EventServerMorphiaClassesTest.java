package io.harness;

import static io.harness.rule.OwnerRule.UNKNOWN;

import com.google.common.collect.ImmutableSet;

import io.harness.category.element.UnitTests;
import io.harness.morphia.MorphiaModule;
import io.harness.rule.OwnerRule.Owner;
import io.harness.serializer.morphia.EventServerMorphiaRegistrar;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class EventServerMorphiaClassesTest extends CategoryTest {
  @Test
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  public void testEventServerClassesModule() {
    new EventServerMorphiaRegistrar().testClassesModule();
  }

  @Test
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  public void testEventServerSearchAndList() {
    new MorphiaModule().testAutomaticSearch(ImmutableSet.<Class>builder().build());
  }

  @Test
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  public void testEventImplementationClassesModule() {
    new EventServerMorphiaRegistrar().testImplementationClassesModule();
  }
}