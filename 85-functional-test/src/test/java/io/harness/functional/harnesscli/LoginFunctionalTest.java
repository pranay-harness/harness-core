package io.harness.functional.harnesscli;

import static io.harness.generator.AccountGenerator.adminUserEmail;
import static io.harness.rule.OwnerRule.DEEPAK;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.category.element.CliFunctionalTests;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.rule.Owner;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.List;

@Slf4j
public class LoginFunctionalTest extends AbstractFunctionalTest {
  @Inject HarnesscliHelper harnesscliHelper;
  @Test
  @Owner(developers = DEEPAK)
  @Category(CliFunctionalTests.class)
  public void loginToLocalhost() {
    String domain = "localhost:9090", loginOutput = "";

    // Will the domain be localhost:9090 always ?
    String command = String.format("harness login -u  %s -p  admin  -d %s", adminUserEmail, domain);

    logger.info("Logging to localhost");
    List<String> cliOutput = null;
    try {
      cliOutput = harnesscliHelper.executeCLICommand(command);
    } catch (Exception IOException) {
      logger.info("Could not read output of terminal command");
      assertThat(false).isTrue();
    }

    if (cliOutput == null || cliOutput.size() != 1) {
      logger.info("The login command output has %d lines", cliOutput.size());
      assertThat(false).isTrue();
    } else {
      loginOutput = cliOutput.get(0);
    }
    // Asserting that the output is good
    logger.info("Comparing the output of login command");
    assertThat(loginOutput).isEqualTo("Logged in Successfully to " + domain);
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category(CliFunctionalTests.class)
  public void loginWithInvalidCredToLocalhost() {
    String domain, loginOutput = "";
    domain = "localhost:9090";

    // Will the domain be localhost:9090 always ?
    String command = String.format("harness login -u  %s -p  wrongPassword  -d %s", adminUserEmail, domain);

    logger.info("Logging to localhost");
    List<String> cliOutput = null;
    try {
      cliOutput = harnesscliHelper.getCLICommandError(command);
    } catch (Exception IOException) {
      logger.info("Could not read output of terminal command");
      assertThat(false).isTrue();
    }

    if (cliOutput == null || cliOutput.size() != 1) {
      logger.info("The login command output has %d lines", cliOutput.size());
      assertThat(false).isTrue();
    } else {
      loginOutput = cliOutput.get(0);
    }
    // Asserting that the output is good
    logger.info("Comparing the output of login command");
    String[] errorMessage = loginOutput.split(" ", 3);
    assertThat(errorMessage[2]).isEqualTo("Invalid Credentials ");
  }
}
