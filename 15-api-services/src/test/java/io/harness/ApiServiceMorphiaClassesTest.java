package io.harness;

import static io.harness.rule.OwnerRule.GEORGE;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.serializer.morphia.ApiServiceMorphiaRegistrar;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class ApiServiceMorphiaClassesTest extends CategoryTest {
  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testApiServiceClassesModule() {
    new ApiServiceMorphiaRegistrar().testClassesModule();
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testApiServiceImplementationClassesModule() {
    new ApiServiceMorphiaRegistrar().testImplementationClassesModule();
  }
}
