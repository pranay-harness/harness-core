package io.harness;

import static io.harness.rule.OwnerRule.GEORGE;

import io.harness.category.element.UnitTests;
import io.harness.morphia.MorphiaModule;
import io.harness.rule.Owner;
import io.harness.serializer.morphia.EventServerMorphiaRegistrar;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class EventServerMorphiaClassesTest extends CategoryTest {
  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testEventServerClassesModule() {
    new EventServerMorphiaRegistrar().testClassesModule();
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testEventServerSearchAndList() {
    new MorphiaModule().testAutomaticSearch();
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testEventImplementationClassesModule() {
    new EventServerMorphiaRegistrar().testImplementationClassesModule();
  }
}