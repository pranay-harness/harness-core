/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.functional.harnesscli;

import static io.harness.rule.OwnerRule.SHUBHANSHU;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.CliFunctionalTests;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.generator.ApplicationGenerator;
import io.harness.generator.ApplicationGenerator.Applications;
import io.harness.generator.EnvironmentGenerator;
import io.harness.generator.OwnerManager;
import io.harness.generator.OwnerManager.Owners;
import io.harness.generator.Randomizer.Seed;
import io.harness.rule.Owner;
import io.harness.testframework.restutils.EnvironmentRestUtils;

import software.wings.beans.Application;
import software.wings.beans.Environment;

import com.google.inject.Inject;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Slf4j
public class GetEnvironmentsFunctionalTest extends AbstractFunctionalTest {
  @Inject private OwnerManager ownerManager;
  @Inject private ApplicationGenerator applicationGenerator;
  @Inject private EnvironmentGenerator environmentGenerator;

  private Application application;
  private Environment testEnvironment;
  private String defaultOutput = "No Environments were found in the Application provided";

  private final Seed seed = new Seed(0);
  private Owners owners;

  @Before
  public void setUp() {
    owners = ownerManager.create();
    application = applicationGenerator.ensurePredefined(seed, owners, Applications.GENERIC_TEST);
    assertThat(application).isNotNull();
    harnesscliHelper.loginToCLI();
  }

  @Inject HarnesscliHelper harnesscliHelper;
  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(CliFunctionalTests.class)
  @Ignore("This test is skipping through maven command. Skipping for bazel")
  public void getEnvironmentsTest() {
    // Running harness get environments before creating a new environment
    String appId = application.getAppId();
    String command = String.format("harness get environments -a %s", appId);
    log.info("Running harness get environments");
    List<String> cliOutput = null;
    try {
      cliOutput = harnesscliHelper.executeCLICommand(command);
    } catch (Exception IOException) {
      log.info("Could not read output of terminal command");
      assertThat(false).isTrue();
    }
    assertThat(cliOutput).isNotNull();
    int outputSize = cliOutput.size();

    // Creating new environment
    String environmentName = "Test environment harnessCli - " + System.currentTimeMillis();
    Environment newEnvironment = new Environment();
    newEnvironment.setName(environmentName);
    testEnvironment = EnvironmentRestUtils.createEnvironment(bearerToken, getAccount(), appId, newEnvironment);
    assertThat(testEnvironment).isNotNull();
    application.setEnvironments(Collections.singletonList(testEnvironment));

    // Running harness get environments after creating a new environment
    log.info("Running harness get environments after creating a new environment");
    cliOutput = null;
    try {
      cliOutput = harnesscliHelper.executeCLICommand(command);
    } catch (Exception IOException) {
      log.info("Could not read output of terminal command");
      assertThat(false).isTrue();
    }
    assertThat(cliOutput).isNotNull();
    int newOutputSize = cliOutput.size();
    assertThat(newOutputSize).isGreaterThanOrEqualTo(outputSize + 1);

    boolean newEnvListed = false;
    Iterator<String> iterator = cliOutput.iterator();
    while (iterator.hasNext()) {
      if (iterator.next().contains(testEnvironment.getUuid())) {
        newEnvListed = true;
        break;
      }
    }
    assertThat(newEnvListed).isTrue();
    EnvironmentRestUtils.deleteEnvironment(bearerToken, appId, getAccount().getUuid(), testEnvironment.getUuid());
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(CliFunctionalTests.class)
  @Ignore("This test is skipping through maven command. Skipping for bazel")
  public void getEnvironmentsWithInvalidArgumentsTest() {
    // Running harness get environment with invalid appId
    String command = String.format("harness get environments -a %s", "INVALID_ID");
    log.info("Running harness get environments with invalid app ID");
    List<String> cliOutput = null;
    try {
      cliOutput = harnesscliHelper.executeCLICommand(command);
    } catch (Exception IOException) {
      log.info("Could not read output of terminal command");
      assertThat(false).isTrue();
    }
    assertThat(cliOutput).isNotNull();
    assertThat(cliOutput.size()).isEqualTo(1);
    assertThat(cliOutput.get(0)).isEqualTo(defaultOutput);
  }
}
