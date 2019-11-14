package io.harness;

import static io.harness.rule.OwnerRule.UNKNOWN;

import io.harness.category.element.UnitTests;
import io.harness.rule.OwnerRule.Owner;
import io.harness.serializer.morphia.CommonMorphiaRegistrar;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class CommonMorphiaClassesTest extends CategoryTest {
  @Test
  @Owner(emails = UNKNOWN)
  @Category(UnitTests.class)
  public void testCommonClassesModule() {
    new CommonMorphiaRegistrar().testClassesModule();
  }

  @Test
  @Owner(emails = UNKNOWN)
  @Category(UnitTests.class)
  public void testCommonImplementationClassesModule() {
    new CommonMorphiaRegistrar().testImplementationClassesModule();
  }
}
