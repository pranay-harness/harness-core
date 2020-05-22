package io.harness.serializer.morphia;

import static io.harness.rule.OwnerRule.KAMAL;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class CVNextGenCommonMorphiaRegisterTest extends CategoryTest {
  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testCVNextGenCommonModule() {
    new CVNextGenCommonMorphiaRegister().testClassesModule();
  }
}