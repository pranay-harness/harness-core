package io.harness.functional.harnesscli;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.generator.AccountGenerator.adminUserEmail;
import static io.harness.generator.EnvironmentGenerator.Environments.GENERIC_TEST;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.sm.StateType.APPROVAL;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import io.harness.functional.WorkflowUtils;
import io.harness.generator.ApplicationGenerator;
import io.harness.generator.ApplicationGenerator.Applications;
import io.harness.generator.EnvironmentGenerator;
import io.harness.generator.OwnerManager;
import io.harness.generator.OwnerManager.Owners;
import io.harness.generator.Randomizer.Seed;
import io.harness.testframework.restutils.UserGroupRestUtils;
import io.harness.testframework.restutils.WorkflowRestUtils;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.Account;
import software.wings.beans.Application;
import software.wings.beans.CanaryOrchestrationWorkflow;
import software.wings.beans.Environment;
import software.wings.beans.GraphNode;
import software.wings.beans.Pipeline;
import software.wings.beans.Workflow;
import software.wings.beans.security.UserGroup;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
      logger.info("Could not wait for the completion of the process");
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

    logger.info("Logging to localhost");
    List<String> cliOutput = null;
    try {
      cliOutput = executeCLICommand(command);
    } catch (Exception IOException) {
      logger.info("Could not read output of terminal command");
      assertThat(false).isTrue();
    }

    if (cliOutput == null || cliOutput.size() != 1) {
      logger.info("The login command output has %d lines", cliOutput.size());
      assertThat(false).isTrue();
    }
  }

  public Workflow createApprovalWorkflow(
      Account account, String bearerToken, String workflowName, Application application) {
    Environment environment = environmentGenerator.ensurePredefined(seed, owners, GENERIC_TEST);
    assertThat(environment).isNotNull();
    logger.info("Creating the workflow");

    logger.info("Fetching User Group Id");
    List<UserGroup> userGroupLists = UserGroupRestUtils.getUserGroups(account, bearerToken);
    String userGroupId = userGroupLists.get(0).getUuid();

    Workflow uiWorkflow = workflowUtils.buildCanaryWorkflowPostDeploymentStep(
        workflowName, environment.getUuid(), getApprovalNode(userGroupId));

    Workflow savedWorkflow =
        WorkflowRestUtils.createWorkflow(bearerToken, application.getAccountId(), application.getUuid(), uiWorkflow);
    assertThat(savedWorkflow).isNotNull();

    logger.info("Asserting that the Phase step of approval is added");
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
    logger.info("Running command {}", command);
    List<String> cliOutput = null;
    try {
      cliOutput = executeCLICommand(command);
    } catch (Exception IOException) {
      logger.info("Could not read output of terminal command");
      assertThat(false).isTrue();
    }

    if (cliOutput == null || cliOutput.size() != 2) {
      logger.info("The deploy command output has " + cliOutput.size() + " lines");
      logger.info("Deploy command failed");
      assertThat(false).isTrue();
    }
  }

  public void deployPipeline(Pipeline approvalPipeline, Application application) {
    String command = String.format("harness deploy -p %s -a %s", approvalPipeline.getUuid(), application.getUuid());
    logger.info("Running command {}", command);
    List<String> cliOutput = null;
    try {
      cliOutput = executeCLICommand(command);
    } catch (Exception IOException) {
      logger.info("Could not read output of terminal command");
      assertThat(false).isTrue();
    }

    if (cliOutput == null || cliOutput.size() != 2) {
      logger.info("The deploy command output has " + cliOutput.size() + " lines");
      logger.info("Deploy command failed");
      assertThat(false).isTrue();
    }
  }

  public String getApprovalID(String executionName) {
    try {
      Thread.sleep(2000);
    } catch (InterruptedException e) {
      logger.info("Got an interrupt while sleeping for get approvals");
    }
    String command = "harness get approvals";
    logger.info("Running command {}", command);
    List<String> cliOutput = null;
    try {
      cliOutput = executeCLICommand(command);
    } catch (IOException e) {
      logger.info("Could not read output of terminal command");
      assertThat(false).isTrue();
    }

    if (cliOutput == null) {
      logger.info("get approvals command failed");
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

  public void deleteWorkflow(String bearerToken, Workflow savedWorkflow, Application application) {
    logger.info("Deleting the workflow");
    WorkflowRestUtils.deleteWorkflow(bearerToken, savedWorkflow.getUuid(), application.getAppId());
  }

  public Application getApplication() {
    owners = ownerManager.create();
    Application application = applicationGenerator.ensurePredefined(seed, owners, Applications.GENERIC_TEST);
    assertThat(application).isNotNull();
    return application;
  }
}
