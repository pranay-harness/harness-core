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
public class VerificationMorphiaClassesTest extends WingsBaseTest {
  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testVerificationModule() {
    new VerificationMorphiaRegistrar().testClassesModule();
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testVerificationSearchAndList() {
    new MorphiaModule().testAutomaticSearch();
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testVerificationImplementationClassesModule() {
    new VerificationMorphiaRegistrar().testImplementationClassesModule();
  }
}
