package io.harness;

import io.harness.category.element.UnitTests;
import io.harness.serializer.morphia.CommonMorphiaRegistrar;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class CommonMorphiaClassesTest extends CategoryTest {
  @Test
  @Category(UnitTests.class)
  public void testCommonClassesModule() {
    new CommonMorphiaRegistrar().testClassesModule();
  }

  @Test
  @Category(UnitTests.class)
  public void testCommonImplementationClassesModule() {
    new CommonMorphiaRegistrar().testImplementationClassesModule();
  }
}
