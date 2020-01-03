package io.harness;

import static io.harness.rule.OwnerRule.GEORGE;

import com.google.common.collect.ImmutableSet;

import io.harness.category.element.UnitTests;
import io.harness.morphia.MorphiaModule;
import io.harness.rule.Owner;
import io.harness.serializer.morphia.DelegateMorphiaRegistrar;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class DelegateMorphiaClassesTest extends CategoryTest {
  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testDelegateClassesModule() {
    new DelegateMorphiaRegistrar().testClassesModule();
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testDelegateSearchAndList() {
    new MorphiaModule().testAutomaticSearch(ImmutableSet.<Class>builder().build());
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testDelegateImplementationClassesModule() {
    new DelegateMorphiaRegistrar().testImplementationClassesModule();
  }
}