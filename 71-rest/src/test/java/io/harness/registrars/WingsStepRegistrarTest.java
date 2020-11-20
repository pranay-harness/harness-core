package io.harness.registrars;

import static io.harness.rule.OwnerRule.PRASHANT;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;

public class WingsStepRegistrarTest extends WingsBaseTest {
  @Inject WingsStepRegistrar wingsStepRegistrar;

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestRegister() {
    wingsStepRegistrar.testClassesModule();
  }
}
