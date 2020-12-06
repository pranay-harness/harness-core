package io.harness.registrars;

import static io.harness.rule.OwnerRule.PRASHANT;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;

import com.google.inject.Inject;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class WingsStepRegistrarTest extends WingsBaseTest {
  @Inject WingsStepRegistrar wingsStepRegistrar;

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void shouldTestRegister() {
    wingsStepRegistrar.testClassesModule();
  }
}
