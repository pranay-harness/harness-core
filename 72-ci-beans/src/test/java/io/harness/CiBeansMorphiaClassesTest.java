package io.harness;

import static io.harness.rule.OwnerRule.HARSH;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.serializer.morphia.CIBeansMorphiaRegistrar;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class CiBeansMorphiaClassesTest extends CategoryTest {
  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  public void testCIClassesModule() {
    new CIBeansMorphiaRegistrar().testClassesModule();
  }

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  public void testCIImplementationClassesModule() {
    new CIBeansMorphiaRegistrar().testImplementationClassesModule();
  }
}