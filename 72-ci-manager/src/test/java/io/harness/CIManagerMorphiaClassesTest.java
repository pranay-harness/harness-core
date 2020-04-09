package io.harness;

import static io.harness.rule.OwnerRule.HARSH;

import com.google.common.collect.ImmutableSet;

import io.harness.app.morphia.CIManagerMorphiaRegistrar;
import io.harness.category.element.UnitTests;
import io.harness.morphia.MorphiaModule;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class CIManagerMorphiaClassesTest extends CategoryTest {
  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  public void testEventServerClassesModule() {
    new CIManagerMorphiaRegistrar().testClassesModule();
  }

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  public void testEventServerSearchAndList() {
    new MorphiaModule().testAutomaticSearch(ImmutableSet.<Class>builder().build());
  }

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  public void testEventImplementationClassesModule() {
    new CIManagerMorphiaRegistrar().testImplementationClassesModule();
  }
}