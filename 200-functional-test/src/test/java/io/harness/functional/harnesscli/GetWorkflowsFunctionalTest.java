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
import io.harness.functional.WorkflowUtils;
import io.harness.generator.ApplicationGenerator;
import io.harness.generator.ApplicationGenerator.Applications;
import io.harness.generator.OwnerManager;
import io.harness.generator.OwnerManager.Owners;
import io.harness.generator.Randomizer.Seed;
import io.harness.generator.WorkflowGenerator;
import io.harness.rule.Owner;
import io.harness.testframework.restutils.WorkflowRestUtils;

import software.wings.beans.Application;
import software.wings.beans.Workflow;

import com.google.inject.Inject;
import java.util.Iterator;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Slf4j
public class GetWorkflowsFunctionalTest extends AbstractFunctionalTest {
  @Inject private OwnerManager ownerManager;
  @Inject private ApplicationGenerator applicationGenerator;
  @Inject private WorkflowGenerator workflowGenerator;
  @Inject private WorkflowUtils workflowUtils;

  private Application application;
  private String defaultOutput = "No Workflows were found in the Application provided";
  private Workflow testWorkflow;

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
  public void getWorkflowsTest() {
    // Running harness get workflows before creating a new workflow
    String appId = application.getAppId();
    String command = String.format("harness get workflows -a %s", appId);
    log.info("Running harness get workflows");
    List<String> cliOutput = null;
    try {
      cliOutput = harnesscliHelper.executeCLICommand(command);
    } catch (Exception IOException) {
      log.info("Could not read output of terminal command");
      assertThat(false).isTrue();
    }
    assertThat(cliOutput).isNotNull();
    int outputSize = cliOutput.size();
    log.info(appId);

    // Creating a new workflow
    String workflowName = "Test Workflow harnessCli - " + System.currentTimeMillis();
    Workflow workflow = new Workflow();
    workflow.setName(workflowName);
    log.info("Creating the workflow");
    testWorkflow = WorkflowRestUtils.createWorkflow(bearerToken, getAccount().getUuid(), appId, workflow);
    assertThat(testWorkflow).isNotNull();

    // Running harness get workflows after creating a new workflow
    log.info("Running harness get workflows after creating a new workflow");
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

    boolean newWorkflowListed = false;
    Iterator<String> iterator = cliOutput.iterator();
    while (iterator.hasNext()) {
      if (iterator.next().contains(testWorkflow.getUuid())) {
        newWorkflowListed = true;
        break;
      }
    }
    assertThat(newWorkflowListed).isTrue();
    WorkflowRestUtils.deleteWorkflow(bearerToken, testWorkflow.getUuid(), appId);
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(CliFunctionalTests.class)
  @Ignore("This test is skipping through maven command. Skipping for bazel")
  public void getWorkflowsWithInvalidArgumentsTest() {
    // Running harness get workflows with invalid appId
    String command = String.format("harness get workflows -a %s", "INVALID_ID");
    log.info("Running harness get workflows with invalid app ID");
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
