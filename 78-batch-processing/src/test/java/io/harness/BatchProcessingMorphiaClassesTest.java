package io.harness;

import static io.harness.rule.OwnerRule.GEORGE;

import com.google.common.collect.ImmutableSet;

import io.harness.category.element.UnitTests;
import io.harness.morphia.MorphiaModule;
import io.harness.rule.Owner;
import io.harness.serializer.morphia.BatchProcessingMorphiaRegistrar;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.integration.common.MongoDBTest;
import software.wings.integration.dl.PageRequestTest;

public class BatchProcessingMorphiaClassesTest extends CategoryTest {
  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testBatchProcessingClassesModule() {
    new BatchProcessingMorphiaRegistrar().testClassesModule();
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testEventSearchAndList() {
    new MorphiaModule().testAutomaticSearch(
        ImmutableSet.<Class>builder().add(MongoDBTest.MongoEntity.class).add(PageRequestTest.Dummy.class).build());
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testBatchProcessingImplementationClassesModule() {
    new BatchProcessingMorphiaRegistrar().testImplementationClassesModule();
  }
}