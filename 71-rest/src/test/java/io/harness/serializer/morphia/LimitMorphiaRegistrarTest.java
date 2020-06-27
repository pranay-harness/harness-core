package io.harness.serializer.morphia;

import static io.harness.rule.OwnerRule.GEORGE;

import io.harness.category.element.UnitTests;
import io.harness.morphia.MorphiaModule;
import io.harness.rule.Owner;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;

@Slf4j
public class LimitMorphiaRegistrarTest extends WingsBaseTest {
  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testLimitsModule() {
    new LimitsMorphiaRegistrar().testClassesModule();
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testLimitsSearchAndList() {
    new MorphiaModule().testAutomaticSearch();
    // ImmutableSet.<Class>builder().add(Dummy.class).add(MongoEntity.class).build()
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testLimitsImplementationClassesModule() {
    new LimitsMorphiaRegistrar().testImplementationClassesModule();
  }
}
