package io.harness.registrars;

import static io.harness.rule.OwnerRule.PRASHANT;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;

public class WingsStateRegistrarTest extends WingsBaseTest {
  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestRegister() {
    new WingsStateRegistrar().testClassesModule();
  }
}