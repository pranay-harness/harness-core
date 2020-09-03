package io.harness.functional.harnesscli;

import static io.harness.generator.EnvironmentGenerator.Environments.GENERIC_TEST;
import static io.harness.rule.OwnerRule.SHUBHANSHU;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.category.element.CliFunctionalTests;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.functional.WorkflowUtils;
import io.harness.generator.ApplicationGenerator;
import io.harness.generator.ApplicationGenerator.Applications;
import io.harness.generator.EnvironmentGenerator;
import io.harness.generator.OwnerManager;
import io.harness.generator.OwnerManager.Owners;
import io.harness.generator.Randomizer.Seed;
import io.harness.rule.Owner;
import io.harness.testframework.restutils.WorkflowRestUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.artifact.Artifact;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

@Slf4j
public class GetDeploymentsFunctionalTests extends AbstractFunctionalTest {
  @Inject private OwnerManager ownerManager;
  @Inject private ApplicationGenerator applicationGenerator;
  @Inject private EnvironmentGenerator environmentGenerator;
  @Inject private WorkflowUtils workflowUtils;
  @Inject HarnesscliHelper harnesscliHelper;

  private Application application;
  private final Seed seed = new Seed(0);
  private Owners owners;
  private Workflow testWorkflow;

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
  public void getDeploymentsTest() {
    // Running harness get deployments before deploying a workflow
    String appId = application.getAppId();
    String command = String.format("harness get deployments");
    logger.info("Running harness get deployments");
    List<String> cliOutput = null;
    try {
      cliOutput = harnesscliHelper.executeCLICommand(command);
    } catch (Exception IOException) {
      logger.info("Could not read output of terminal command");
      assertThat(false).isTrue();
    }
    assertThat(cliOutput).isNotNull();

    // Creating and deploying a workflow
    logger.info("Creating the workflow");
    String workflowName = "Test Workflow harnessCli - " + System.currentTimeMillis();
    Environment environment = environmentGenerator.ensurePredefined(seed, owners, GENERIC_TEST);
    assertThat(environment).isNotNull();
    Workflow sampleWorkflow = workflowUtils.buildCanaryWorkflowPostDeploymentStep(workflowName, environment.getUuid());
    testWorkflow = WorkflowRestUtils.createWorkflow(bearerToken, getAccount().getUuid(), appId, sampleWorkflow);
    assertThat(testWorkflow).isNotNull();
    // Test running the workflow
    WorkflowExecution workflowExecution = runWorkflow(bearerToken, application.getUuid(), environment.getUuid(),
        testWorkflow.getUuid(), Collections.<Artifact>emptyList());
    assertThat(workflowExecution).isNotNull();

    // Running harness get deployments after deploying a workflow
    logger.info("Running harness get deployments after creating a new workflow");
    cliOutput = null;
    try {
      cliOutput = harnesscliHelper.executeCLICommand(command);
    } catch (Exception IOException) {
      logger.info("Could not read output of terminal command");
      assertThat(false).isTrue();
    }
    assertThat(cliOutput).isNotNull();

    boolean newDeploymentListed = false;
    Iterator<String> iterator = cliOutput.iterator();
    while (iterator.hasNext()) {
      if (iterator.next().contains(workflowName)) {
        newDeploymentListed = true;
        break;
      }
    }
    assertThat(newDeploymentListed).isTrue();
    WorkflowRestUtils.deleteWorkflow(bearerToken, testWorkflow.getUuid(), appId);
  }
}