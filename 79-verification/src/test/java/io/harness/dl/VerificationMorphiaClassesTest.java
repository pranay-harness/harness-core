package io.harness.dl;

import com.google.common.collect.ImmutableSet;

import io.harness.category.element.UnitTests;
import io.harness.morphia.MorphiaModule;
import io.harness.serializer.morphia.VerificationMorphiaRegistrar;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;
import software.wings.integration.common.MongoDBTest.MongoEntity;
import software.wings.integration.dl.PageRequestTest.Dummy;

@Slf4j
public class VerificationMorphiaClassesTest extends WingsBaseTest {
  @Test
  @Category(UnitTests.class)
  public void testVerificationModule() {
    new VerificationMorphiaRegistrar().testClassesModule();
  }

  @Test
  @Category(UnitTests.class)
  public void testVerificationSearchAndList() {
    new MorphiaModule().testAutomaticSearch(
        ImmutableSet.<Class>builder().add(MongoEntity.class).add(Dummy.class).build());
  }

  @Test
  @Category(UnitTests.class)
  public void testVerificationImplementationClassesModule() {
    new VerificationMorphiaRegistrar().testImplementationClassesModule();
  }
}
