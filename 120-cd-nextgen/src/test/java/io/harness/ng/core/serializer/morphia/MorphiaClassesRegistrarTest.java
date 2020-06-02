package io.harness.ng.core.serializer.morphia;

import static io.harness.rule.OwnerRule.ANKIT;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class MorphiaClassesRegistrarTest extends CategoryTest {
  @Test
  @Owner(developers = ANKIT)
  @Category(UnitTests.class)
  public void test() {
    new MorphiaClassesRegistrar().testClassesModule();
  }
}