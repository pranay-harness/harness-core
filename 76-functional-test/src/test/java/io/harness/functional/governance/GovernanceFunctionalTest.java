package io.harness.functional.governance;

import static io.harness.beans.WorkflowType.ORCHESTRATION;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.generator.EnvironmentGenerator.Environments.GENERIC_TEST;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.CanaryOrchestrationWorkflow.CanaryOrchestrationWorkflowBuilder.aCanaryOrchestrationWorkflow;
import static software.wings.beans.PhaseStep.PhaseStepBuilder.aPhaseStep;
import static software.wings.beans.PhaseStepType.POST_DEPLOYMENT;
import static software.wings.beans.PhaseStepType.PRE_DEPLOYMENT;
import static software.wings.beans.Workflow.WorkflowBuilder.aWorkflow;
import static software.wings.sm.StateType.HTTP;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import io.harness.RestUtils.WorkflowRestUtil;
import io.harness.beans.ExecutionStatus;
import io.harness.beans.WorkflowType;
import io.harness.category.element.FunctionalTests;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.generator.ApplicationGenerator;
import io.harness.generator.ApplicationGenerator.Applications;
import io.harness.generator.EnvironmentGenerator;
import io.harness.generator.OwnerManager;
import io.harness.generator.OwnerManager.Owners;
import io.harness.generator.Randomizer.Seed;
import io.harness.rest.RestResponse;
import io.restassured.http.ContentType;
import io.restassured.mapper.ObjectMapperType;
import org.awaitility.Awaitility;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.ExecutionArgs;
import software.wings.beans.ExecutionCredential.ExecutionType;
import software.wings.beans.GraphNode;
import software.wings.beans.SSHExecutionCredential;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.governance.GovernanceConfig;
import software.wings.common.Constants;
import software.wings.service.intfc.WorkflowExecutionService;
import software.wings.sm.states.HttpState;

import java.util.concurrent.TimeUnit;
import javax.ws.rs.core.GenericType;

/**
 * @author rktummala on 02/18/19
 */
public class GovernanceFunctionalTest extends AbstractFunctionalTest {
  @Inject private OwnerManager ownerManager;
  @Inject private ApplicationGenerator applicationGenerator;
  @Inject private EnvironmentGenerator environmentGenerator;
  @Inject private WorkflowExecutionService workflowExecutionService;
  @Inject private WorkflowRestUtil workflowRestUtil;

  Application application;
  final Seed seed = new Seed(0);
  Owners owners;

  @Before
  public void setUp() {
    owners = ownerManager.create();
    application = applicationGenerator.ensurePredefined(seed, owners, Applications.GENERIC_TEST);
    assertThat(application).isNotNull();
  }

  @Test
  @Category(FunctionalTests.class)
  @Ignore
  public void shouldBlockDeploymentsWhenFreezeIsEnabled() throws Exception {
    Environment environment = environmentGenerator.ensurePredefined(seed, owners, GENERIC_TEST);
    assertThat(environment).isNotNull();
    Workflow workflow =
        aWorkflow()
            .name("HTTP Workflow")
            .workflowType(WorkflowType.ORCHESTRATION)
            .envId(environment.getUuid())
            .orchestrationWorkflow(
                aCanaryOrchestrationWorkflow()
                    .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT, Constants.PRE_DEPLOYMENT).build())
                    .withPostDeploymentSteps(
                        aPhaseStep(POST_DEPLOYMENT, Constants.POST_DEPLOYMENT).addStep(getHTTPNode()).build())
                    .build())
            .build();

    // Test creating a workflow
    Workflow savedWorkflow =
        workflowRestUtil.createWorkflow(application.getAccountId(), application.getUuid(), workflow);
    assertThat(savedWorkflow).isNotNull();
    assertThat(savedWorkflow.getUuid()).isNotEmpty();
    assertThat(savedWorkflow.getWorkflowType()).isEqualTo(ORCHESTRATION);

    // Test running the workflow

    ExecutionArgs executionArgs = new ExecutionArgs();
    executionArgs.setWorkflowType(savedWorkflow.getWorkflowType());
    executionArgs.setExecutionCredential(
        SSHExecutionCredential.Builder.aSSHExecutionCredential().withExecutionType(ExecutionType.SSH).build());
    executionArgs.setOrchestrationId(savedWorkflow.getUuid());

    WorkflowExecution workflowExecution =
        workflowRestUtil.runWorkflow(application.getUuid(), environment.getUuid(), executionArgs);
    assertThat(workflowExecution).isNotNull();

    String executionUuid = workflowExecution.getUuid();
    Awaitility.await()
        .atMost(120, TimeUnit.SECONDS)
        .pollInterval(5, TimeUnit.SECONDS)
        .until(()
                   -> workflowExecutionService.getWorkflowExecution(application.getUuid(), executionUuid)
                          .getStatus()
                          .equals(ExecutionStatus.SUCCESS));

    setDeploymentFreeze(application.getAccountId(), true);

    workflowExecution = workflowRestUtil.runWorkflow(application.getUuid(), environment.getUuid(), executionArgs);
    assertThat(workflowExecution).isNull();

    setDeploymentFreeze(application.getAccountId(), false);

    workflowExecution = workflowRestUtil.runWorkflow(application.getUuid(), environment.getUuid(), executionArgs);
    assertThat(workflowExecution).isNotNull();

    String newExecutionUuid = workflowExecution.getUuid();
    Awaitility.await()
        .atMost(120, TimeUnit.SECONDS)
        .pollInterval(5, TimeUnit.SECONDS)
        .until(()
                   -> workflowExecutionService.getWorkflowExecution(application.getUuid(), newExecutionUuid)
                          .getStatus()
                          .equals(ExecutionStatus.SUCCESS));
  }

  private GraphNode getHTTPNode() {
    return GraphNode.builder()
        .id(generateUuid())
        .type(HTTP.name())
        .name("HTTP")
        .properties(ImmutableMap.<String, Object>builder()
                        .put(HttpState.URL_KEY, "http://www.google.com")
                        .put(HttpState.METHOD_KEY, "GET")
                        .build())
        .build();
  }

  private void setDeploymentFreeze(String accountId, boolean freeze) {
    GenericType<RestResponse<GovernanceConfig>> returnType = new GenericType<RestResponse<GovernanceConfig>>() {};
    GovernanceConfig governanceConfig =
        GovernanceConfig.builder().accountId(accountId).deploymentFreeze(freeze).build();
    given()
        .auth()
        .oauth2(bearerToken)
        .queryParam("accountId", accountId)
        .contentType(ContentType.JSON)
        .body(governanceConfig, ObjectMapperType.GSON)
        .put("/compliance-config/" + accountId)
        .as(returnType.getType());
  }
}
