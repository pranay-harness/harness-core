package io.harness.functional.commands;

import static io.harness.rule.OwnerRule.GEORGE;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.FunctionalTests;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.rule.FunctionalTestRule;
import io.harness.rule.Owner;
import io.harness.testframework.framework.ManagerExecutor;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.zeroturnaround.exec.ProcessExecutor;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class InspectCommandTest extends AbstractFunctionalTest {
  @Test
  @Owner(developers = GEORGE)
  @Category(FunctionalTests.class)
  @Ignore("Needs more work to make it works")
  public void testIsnpectCommand() throws IOException, TimeoutException, InterruptedException {
    ProcessExecutor inspect = ManagerExecutor.managerProcessExecutor(
        AbstractFunctionalTest.class, "inspect", FunctionalTestRule.alpn, FunctionalTestRule.alpnJar);
    String output = inspect.readOutput(true).execute().outputUTF8();
    assertThat(output).contains("the inspection finished");
  }
}
