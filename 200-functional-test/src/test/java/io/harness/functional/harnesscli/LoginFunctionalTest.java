/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.functional.harnesscli;

import static io.harness.generator.AccountGenerator.adminUserEmail;
import static io.harness.rule.OwnerRule.DEEPAK;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.CliFunctionalTests;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Slf4j
public class LoginFunctionalTest extends AbstractFunctionalTest {
  @Inject HarnesscliHelper harnesscliHelper;
  @Test
  @Owner(developers = DEEPAK)
  @Category(CliFunctionalTests.class)
  @Ignore("This test is skipping through maven command. Skipping for bazel")
  public void loginToLocalhost() {
    String domain = "localhost:9090", loginOutput = "";

    // Will the domain be localhost:9090 always ?
    String command = String.format("harness login -u  %s -p  admin  -d %s", adminUserEmail, domain);

    log.info("Logging to localhost");
    List<String> cliOutput = null;
    try {
      cliOutput = harnesscliHelper.executeCLICommand(command);
    } catch (Exception IOException) {
      log.info("Could not read output of terminal command");
      assertThat(false).isTrue();
    }

    if (cliOutput == null || cliOutput.size() != 1) {
      log.info("The login command output has %d lines", cliOutput.size());
      assertThat(false).isTrue();
    } else {
      loginOutput = cliOutput.get(0);
    }
    // Asserting that the output is good
    log.info("Comparing the output of login command");
    assertThat(loginOutput).isEqualTo("Logged in Successfully to " + domain);
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category(CliFunctionalTests.class)
  @Ignore("This test is skipping through maven command. Skipping for bazel")
  public void loginWithInvalidCredToLocalhost() {
    String domain, loginOutput = "";
    domain = "localhost:9090";

    // Will the domain be localhost:9090 always ?
    String command = String.format("harness login -u  %s -p  wrongPassword  -d %s", adminUserEmail, domain);

    log.info("Logging to localhost");
    List<String> cliOutput = null;
    try {
      cliOutput = harnesscliHelper.getCLICommandError(command);
    } catch (Exception IOException) {
      log.info("Could not read output of terminal command");
      assertThat(false).isTrue();
    }

    if (cliOutput == null || cliOutput.size() != 1) {
      log.info("The login command output has %d lines", cliOutput.size());
      assertThat(false).isTrue();
    } else {
      loginOutput = cliOutput.get(0);
    }
    // Asserting that the output is good
    log.info("Comparing the output of login command");
    String[] errorMessage = loginOutput.split(" ", 3);
    assertThat(errorMessage[2]).isEqualTo("Invalid Credentials ");
  }
}
