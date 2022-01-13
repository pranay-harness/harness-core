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
import io.harness.generator.OwnerManager;
import io.harness.generator.OwnerManager.Owners;
import io.harness.generator.Randomizer.Seed;
import io.harness.rule.Owner;
import io.harness.testframework.framework.utils.PipelineUtils;
import io.harness.testframework.restutils.PipelineRestUtils;

import software.wings.beans.Application;
import software.wings.beans.Pipeline;

import com.google.inject.Inject;
import java.util.Iterator;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Slf4j
public class GetApprovalsFunctionalTest extends AbstractFunctionalTest {
  @Inject private OwnerManager ownerManager;
  @Inject private ApplicationGenerator applicationGenerator;
  @Inject HarnesscliHelper harnesscliHelper;

  private Application application;
  private final Seed seed = new Seed(0);
  private Owners owners;
  private Pipeline testPipeline;

  @Before
  public void setUp() {
    owners = ownerManager.create();
    application = applicationGenerator.ensurePredefined(seed, owners, Applications.GENERIC_TEST);
    assertThat(application).isNotNull();
    harnesscliHelper.loginToCLI();
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(CliFunctionalTests.class)
  @Ignore("This test is skipping through maven command. Skipping for bazel")
  public void getApprovalsTest() {
    // Running harness get approvals before executing a pipeline with approval step
    String command = String.format("harness get approvals");
    log.info("Running command " + command);
    List<String> cliOutput = null;
    try {
      cliOutput = harnesscliHelper.executeCLICommand(command);
    } catch (Exception IOException) {
      log.info("Could not read output of terminal command");
      assertThat(false).isTrue();
    }
    int outputSize = cliOutput.size();

    // Creating and deploying pipeline with approval step
    String pipelineName = "Test Pipeline harnessCli - " + System.currentTimeMillis();
    testPipeline =
        PipelineUtils.createApprovalPipeline(pipelineName, getAccount(), bearerToken, application.getAppId());
    assertThat(testPipeline).isNotNull();
    harnesscliHelper.deployPipeline(testPipeline, application);
    String approvalId = harnesscliHelper.getApprovalID(pipelineName);
    log.info(approvalId);
    if (approvalId == null) {
      log.info("No approval Exists for the given ID");
      assertThat(false).isTrue();
    }

    // Running harness get approvals after executing a pipeline with approval step
    log.info("Running command " + command);
    cliOutput = null;
    try {
      cliOutput = harnesscliHelper.executeCLICommand(command);
    } catch (Exception IOException) {
      log.info("Could not read output of terminal command");
      assertThat(false).isTrue();
    }
    int newOutputSize = cliOutput.size();
    assertThat(newOutputSize).isGreaterThan(outputSize);

    boolean newApprovalListed = false;
    Iterator<String> iterator = cliOutput.iterator();
    while (iterator.hasNext()) {
      if (iterator.next().contains(approvalId)) {
        newApprovalListed = true;
        break;
      }
    }
    assertThat(newApprovalListed).isTrue();

    // Approving the pending approval
    command = String.format("harness approve -i %s", approvalId);
    cliOutput = null;
    try {
      cliOutput = harnesscliHelper.executeCLICommand(command);
    } catch (Exception IOException) {
      log.info("Could not read output of terminal command");
      assertThat(false).isTrue();
    }
    if (cliOutput == null || cliOutput.size() != 3) {
      log.info("The approve command output has " + cliOutput.size() + " lines");
      assertThat(false).isTrue();
    }
    assertThat(cliOutput.get(0).contains("Execution status: SUCCESS")).isTrue();
    assertThat(cliOutput.get(1).contains("Execution ID:")).isTrue();
    assertThat(cliOutput.get(2).contains("Execution Link:")).isTrue();
    PipelineRestUtils.deletePipeline(application.getAppId(), testPipeline.getUuid(), bearerToken);
  }
}
