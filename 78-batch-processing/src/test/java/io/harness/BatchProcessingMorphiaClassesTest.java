package io.harness;

import static io.harness.rule.OwnerRule.GEORGE;
import static io.harness.rule.OwnerRule.VIKAS;

import com.google.common.collect.ImmutableSet;

import io.harness.category.element.UnitTests;
import io.harness.morphia.MorphiaModule;
import io.harness.rule.Owner;
import io.harness.serializer.morphia.BatchProcessingMorphiaRegistrar;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class BatchProcessingMorphiaClassesTest extends CategoryTest {
  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testBatchProcessingClassesModule() {
    new BatchProcessingMorphiaRegistrar().testClassesModule();
  }

  @Test
  @Owner(developers = VIKAS)
  @Category(UnitTests.class)
  @Ignore("fix this later, the setup for this test project is incomplete")
  public void testEventSearchAndList() {
    new MorphiaModule().testAutomaticSearch(ImmutableSet.<Class>builder().build());
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testBatchProcessingImplementationClassesModule() {
    new BatchProcessingMorphiaRegistrar().testImplementationClassesModule();
  }
}