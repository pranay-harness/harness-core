package io.harness.functional.harnesscli;

import static io.harness.rule.OwnerRule.DEEPAK;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.category.element.CliFunctionalTests;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.rule.OwnerRule.Owner;
import io.harness.testframework.framework.utils.PipelineUtils;
import io.harness.testframework.restutils.PipelineRestUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.Application;
import software.wings.beans.Pipeline;
import software.wings.beans.Workflow;

import java.util.List;

@Slf4j
public class RejectFunctionalTest extends AbstractFunctionalTest {
  @Inject HarnesscliHelper harnesscliHelper;

  private Application application;
  public Workflow approvalWorkflow;
  public String workflowName;
  public Pipeline approvalPipeline;
  public String pipelineName;

  @Before
  public void setUp() {
    application = harnesscliHelper.getApplication();
    harnesscliHelper.loginToCLI();
  }

  @Test
  @Owner(emails = DEEPAK)
  @Category(CliFunctionalTests.class)
  public void rejectTheWorkflow() {
    workflowName = "CLI-Test-Approval-" + System.currentTimeMillis();
    approvalWorkflow = harnesscliHelper.createApprovalWorkflow(getAccount(), bearerToken, workflowName, application);
    harnesscliHelper.deployWorkflow(approvalWorkflow, application);
    String approvalId = harnesscliHelper.getApprovalID(workflowName);
    if (approvalId == null) {
      logger.info("No approval Exists for the given ID");
      assertThat(false).isTrue();
    }
    String command = String.format("harness reject -i %s", approvalId);
    logger.info("Running command {}", command);

    List<String> cliOutput = null;
    try {
      cliOutput = harnesscliHelper.executeCLICommand(command);
    } catch (Exception IOException) {
      logger.info("Could not read output of terminal command");
      assertThat(false).isTrue();
    }

    if (cliOutput == null || cliOutput.size() != 3) {
      logger.info("The reject command output has " + cliOutput.size() + " lines");
      assertThat(false).isTrue();
    }
    verifyReject(cliOutput);
    harnesscliHelper.deleteWorkflow(bearerToken, approvalWorkflow, application);
  }

  @Test
  @Owner(emails = DEEPAK)
  @Category(CliFunctionalTests.class)
  public void rejectWithoutFlag() {
    String rejectOutput = "";
    String command = String.format("harness reject");
    logger.info("Running command {}", command);
    logger.info("Running command on localhost");
    List<String> cliOutput = null;
    try {
      cliOutput = harnesscliHelper.getCLICommandError(command);
    } catch (Exception IOException) {
      logger.info("Could not read output of terminal command");
      assertThat(false).isTrue();
    }
    rejectOutput = cliOutput.get(0);
    // Asserting that the output is good
    logger.info("Comparing the output of reject command");
    assertThat(rejectOutput).isEqualTo("Error: required flag(s) \"approval-id\" not set");
  }

  @Test
  @Owner(emails = DEEPAK)
  @Category(CliFunctionalTests.class)
  public void rejectThePipeline() {
    pipelineName = "CLI-PIPELINE-" + System.currentTimeMillis();
    logger.info("Generated unique pipeline name : " + pipelineName);
    approvalPipeline =
        PipelineUtils.createApprovalPipeline(pipelineName, getAccount(), bearerToken, application.getAppId());
    assertThat(approvalPipeline).isNotNull();
    harnesscliHelper.deployPipeline(approvalPipeline, application);
    String approvalId = harnesscliHelper.getApprovalID(approvalPipeline.getName());
    if (approvalId == null) {
      logger.info("No approval Exists for the given ID");
      assertThat(false).isTrue();
    }
    String command = String.format("harness reject -i %s", approvalId);
    logger.info("Running command {}", command);

    List<String> cliOutput = null;
    try {
      cliOutput = harnesscliHelper.executeCLICommand(command);
    } catch (Exception IOException) {
      logger.info("Could not read output of terminal command");
      assertThat(false).isTrue();
    }

    if (cliOutput == null || cliOutput.size() != 3) {
      logger.info("The reject command output has " + cliOutput.size() + " lines");
      assertThat(false).isTrue();
    }
    verifyReject(cliOutput);
    PipelineRestUtils.deletePipeline(application.getAppId(), approvalPipeline.getUuid(), bearerToken);
  }

  public void verifyReject(List<String> cliOutput) {
    assertThat(cliOutput.get(0).contains("Execution status: REJECTED")).isTrue();
    assertThat(cliOutput.get(1).contains("Execution ID:")).isTrue();
    assertThat(cliOutput.get(2).contains("Execution Link:")).isTrue();
  }
}
