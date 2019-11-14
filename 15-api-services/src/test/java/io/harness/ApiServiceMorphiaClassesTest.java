package io.harness;

import static io.harness.rule.OwnerRule.UNKNOWN;

import com.google.common.collect.ImmutableSet;

import io.harness.category.element.UnitTests;
import io.harness.morphia.MorphiaModule;
import io.harness.rule.OwnerRule.Owner;
import io.harness.serializer.morphia.ApiServiceMorphiaRegistrar;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class ApiServiceMorphiaClassesTest extends CategoryTest {
  @Test
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  public void testApiServiceClassesModule() {
    new ApiServiceMorphiaRegistrar().testClassesModule();
  }

  @Test
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  public void testApiServiceSearchAndList() {
    new MorphiaModule().testAutomaticSearch(ImmutableSet.<Class>builder().build());
  }

  @Test
  @Owner(developers = UNKNOWN)
  @Category(UnitTests.class)
  public void testApiServiceImplementationClassesModule() {
    new ApiServiceMorphiaRegistrar().testImplementationClassesModule();
  }
}
