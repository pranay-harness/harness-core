package io.harness.mongo;

import static io.harness.rule.OwnerRule.GEORGE;

import com.google.common.collect.ImmutableSet;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.morphia.MorphiaModule;
import io.harness.rule.Owner;
import io.harness.serializer.morphia.PersistenceMorphiaRegistrar;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class PersistenceMorphiaClassesTest extends CategoryTest {
  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testPersistenceModule() {
    new PersistenceMorphiaRegistrar().testClassesModule();
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testPersistenceSearchAndList() {
    new MorphiaModule().testAutomaticSearch(ImmutableSet.<Class>builder().build());
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testPersistencImplementationClassesModule() {
    new PersistenceMorphiaRegistrar().testImplementationClassesModule();
  }
}