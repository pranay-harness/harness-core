package io.harness.serializer.morphia;

import com.google.common.collect.ImmutableSet;

import io.harness.category.element.UnitTests;
import io.harness.morphia.MorphiaModule;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;
import software.wings.integration.common.MongoDBTest.MongoEntity;
import software.wings.integration.dl.PageRequestTest.Dummy;

@Slf4j
public class LimitMorphiaRegistrarTest extends WingsBaseTest {
  @Test
  @Category(UnitTests.class)
  public void testLimitsModule() {
    new LimitsMorphiaRegistrar().testClassesModule();
  }

  @Test
  @Category(UnitTests.class)
  public void testLimitsSearchAndList() {
    new MorphiaModule().testAutomaticSearch(
        ImmutableSet.<Class>builder().add(Dummy.class).add(MongoEntity.class).build());
  }

  @Test
  @Category(UnitTests.class)
  public void testLimitsImplementationClassesModule() {
    new LimitsMorphiaRegistrar().testImplementationClassesModule();
  }
}
