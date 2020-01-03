package io.harness;

import static io.harness.rule.OwnerRule.GEORGE;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.serializer.morphia.CommonMorphiaRegistrar;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class CommonMorphiaClassesTest extends CategoryTest {
  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testCommonClassesModule() {
    new CommonMorphiaRegistrar().testClassesModule();
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testCommonImplementationClassesModule() {
    new CommonMorphiaRegistrar().testImplementationClassesModule();
  }
}
