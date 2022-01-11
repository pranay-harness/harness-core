/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.functional.harnesscli;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.generator.AccountGenerator.adminUserEmail;
import static io.harness.generator.EnvironmentGenerator.Environments.GENERIC_TEST;

import static software.wings.sm.StateType.APPROVAL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import io.harness.functional.WorkflowUtils;
import io.harness.generator.ApplicationGenerator;
import io.harness.generator.ApplicationGenerator.Applications;
import io.harness.generator.EnvironmentGenerator;
import io.harness.generator.OwnerManager;
import io.harness.generator.OwnerManager.Owners;
import io.harness.generator.Randomizer.Seed;
import io.harness.testframework.restutils.UserGroupRestUtils;
import io.harness.testframework.restutils.WorkflowRestUtils;

import software.wings.beans.Account;
import software.wings.beans.Application;
import software.wings.beans.CanaryOrchestrationWorkflow;
import software.wings.beans.Environment;
import software.wings.beans.GraphNode;
import software.wings.beans.Pipeline;
import software.wings.beans.Workflow;
import software.wings.beans.security.UserGroup;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Duration;
import org.hamcrest.CoreMatchers;

@Slf4j
public class HarnesscliHelper {
  @Inject private EnvironmentGenerator environmentGenerator;
  @Inject private ApplicationGenerator applicationGenerator;
  @Inject private WorkflowUtils workflowUtils;
  @Inject private OwnerManager ownerManager;

  private Owners owners;
  private final Seed seed = new Seed(0);

  public List<String> executeCLICommand(String command) throws IOException {
    String s;
    Process processFinal = Runtime.getRuntime().exec(command);
    List<String> cliOutput = new ArrayList<>();
    InputStream inputStream = null;
    try {
      processFinal.waitFor();
    } catch (java.lang.InterruptedException e) {
      log.info("Could not wait for the completion of the process");
    }
    inputStream = processFinal.getInputStream();
    BufferedReader processStdErr = new BufferedReader(new InputStreamReader(inputStream));
    while ((s = processStdErr.readLine()) != null) {
      cliOutput.add(s);
    }
    return cliOutput;
  }

  public List<String> getCLICommandError(String command) throws IOException {
    String s;
    Process processFinal = Runtime.getRuntime().exec(command);
    List<String> cliOutput = new ArrayList<>();
    InputStream inputStream = null;
    inputStream = processFinal.getErrorStream();
    BufferedReader processStdErr = new BufferedReader(new InputStreamReader(inputStream));
    while ((s = processStdErr.readLine()) != null) {
      cliOutput.add(s);
    }
    return cliOutput;
  }

  private GraphNode getApprovalNode(String userGroupId) {
    return GraphNode.builder()
        .id(generateUuid())
        .type(APPROVAL.name())
        .name("Test Approval")
        .properties(ImmutableMap.<String, Object>builder()
                        .put("approvalStateType", "USER_GROUP")
                        .put("timeoutMillis", 1800000)
                        .put("userGroups", Collections.singletonList(userGroupId))
                        .build())
        .build();
  }

  public void loginToCLI() {
    String domain = "localhost:9090";

    // Will the domain be localhost:9090 always ?
    String command = String.format("harness login -u  %s -p  admin  -d %s", adminUserEmail, domain);

    log.info("Logging to localhost");
    List<String> cliOutput = null;
    try {
      cliOutput = executeCLICommand(command);
    } catch (Exception IOException) {
      log.info("Could not read output of terminal command");
      assertThat(false).isTrue();
    }

    if (cliOutput == null || cliOutput.size() != 1) {
      log.info("The login command output has %d lines", cliOutput.size());
      assertThat(false).isTrue();
    }
  }

  public Workflow createApprovalWorkflow(
      Account account, String bearerToken, String workflowName, Application application) {
    Environment environment = environmentGenerator.ensurePredefined(seed, owners, GENERIC_TEST);
    assertThat(environment).isNotNull();
    log.info("Creating the workflow");

    log.info("Fetching User Group Id");
    List<UserGroup> userGroupLists = UserGroupRestUtils.getUserGroups(account, bearerToken);
    String userGroupId = userGroupLists.get(0).getUuid();

    Workflow uiWorkflow = workflowUtils.buildCanaryWorkflowPostDeploymentStep(
        workflowName, environment.getUuid(), getApprovalNode(userGroupId));

    Workflow savedWorkflow =
        WorkflowRestUtils.createWorkflow(bearerToken, application.getAccountId(), application.getUuid(), uiWorkflow);
    assertThat(savedWorkflow).isNotNull();

    log.info("Asserting that the Phase step of approval is added");
    String phaseName = ((CanaryOrchestrationWorkflow) savedWorkflow.getOrchestrationWorkflow())
                           .getPostDeploymentSteps()
                           .getSteps()
                           .get(0)
                           .getName();
    assertThat(phaseName).isEqualToIgnoringCase("Test Approval");
    return savedWorkflow;
  }

  public void deployWorkflow(Workflow savedWorkflow, Application application) {
    String command = String.format("harness deploy -w %s -a %s", savedWorkflow.getUuid(), application.getUuid());
    log.info("Running command {}", command);
    List<String> cliOutput = null;
    try {
      cliOutput = executeCLICommand(command);
    } catch (Exception IOException) {
      log.info("Could not read output of terminal command");
      assertThat(false).isTrue();
    }

    if (cliOutput == null || cliOutput.size() != 2) {
      log.info("The deploy command output has " + cliOutput.size() + " lines");
      log.info("Deploy command failed");
      assertThat(false).isTrue();
    }
  }

  public void deployPipeline(Pipeline approvalPipeline, Application application) {
    String command = String.format("harness deploy -p %s -a %s", approvalPipeline.getUuid(), application.getUuid());
    log.info("Running command {}", command);
    List<String> cliOutput = null;
    try {
      cliOutput = executeCLICommand(command);
    } catch (Exception IOException) {
      log.info("Could not read output of terminal command");
      assertThat(false).isTrue();
    }

    if (cliOutput == null || cliOutput.size() != 2) {
      log.info("The deploy command output has " + cliOutput.size() + " lines");
      log.info("Deploy command failed");
      assertThat(false).isTrue();
    }
  }

  public String getApproval(String executionName) {
    String command = "harness get approvals";
    log.info("Running command {}", command);
    List<String> cliOutput = null;
    try {
      cliOutput = executeCLICommand(command);
    } catch (IOException e) {
      log.info("Could not read output of terminal command");
      assertThat(false).isTrue();
    }

    if (cliOutput == null) {
      log.info("get approvals command failed");
      assertThat(false).isTrue();
    }

    for (String element : cliOutput) {
      if (element.contains(executionName)) {
        String[] approvalDetail = element.split("\\t", 0);
        return approvalDetail[approvalDetail.length - 1];
      }
    }
    return "";
  }

  public String getApprovalID(String executionName) {
    String approvalId;
    await().atMost(Duration.FIVE_SECONDS).until(() -> { return getApproval(executionName); }, CoreMatchers.not(""));
    approvalId = getApproval(executionName);
    return approvalId;
  }

  public void deleteWorkflow(String bearerToken, Workflow savedWorkflow, Application application) {
    log.info("Deleting the workflow");
    WorkflowRestUtils.deleteWorkflow(bearerToken, savedWorkflow.getUuid(), application.getAppId());
  }

  public Application getApplication() {
    owners = ownerManager.create();
    Application application = applicationGenerator.ensurePredefined(seed, owners, Applications.GENERIC_TEST);
    assertThat(application).isNotNull();
    return application;
  }
}
