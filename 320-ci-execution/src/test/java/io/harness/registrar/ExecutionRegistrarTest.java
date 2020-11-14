package io.harness.registrar;

import static io.harness.rule.OwnerRule.HARSH;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.executionplan.CIExecutionTest;
import io.harness.registrars.ExecutionRegistrar;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class ExecutionRegistrarTest extends CIExecutionTest {
  @Inject private ExecutionRegistrar executionRegistrar;

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  public void shouldTestRegister() {
    executionRegistrar.testClassesModule();
  }
}