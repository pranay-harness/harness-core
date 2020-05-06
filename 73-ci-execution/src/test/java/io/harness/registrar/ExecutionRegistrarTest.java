package io.harness.registrar;

import static io.harness.rule.OwnerRule.HARSH;

import io.harness.category.element.UnitTests;
import io.harness.executionplan.CIExecutionTest;
import io.harness.registrars.ExecutionRegistrar;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class ExecutionRegistrarTest extends CIExecutionTest {
  @Test
  @Owner(developers = HARSH, intermittent = true)
  @Category(UnitTests.class)
  public void shouldTestRegister() {
    new ExecutionRegistrar().testClassesModule();
  }
}