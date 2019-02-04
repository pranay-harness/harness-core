package io.harness.functional.jira;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.generator.EnvironmentGenerator.Environments.GENERIC_TEST;
import static io.harness.generator.SettingGenerator.Settings.HARNESS_JIRA;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.CanaryOrchestrationWorkflow.CanaryOrchestrationWorkflowBuilder.aCanaryOrchestrationWorkflow;
import static software.wings.beans.PhaseStep.PhaseStepBuilder.aPhaseStep;
import static software.wings.beans.PhaseStepType.POST_DEPLOYMENT;
import static software.wings.beans.PhaseStepType.PRE_DEPLOYMENT;
import static software.wings.beans.Workflow.WorkflowBuilder.aWorkflow;
import static software.wings.beans.WorkflowType.ORCHESTRATION;
import static software.wings.sm.StateType.JIRA_CREATE_UPDATE;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import io.harness.beans.ExecutionStatus;
import io.harness.category.element.FunctionalTests;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.generator.AccountGenerator;
import io.harness.generator.ApplicationGenerator;
import io.harness.generator.ApplicationGenerator.Applications;
import io.harness.generator.EnvironmentGenerator;
import io.harness.generator.OwnerManager;
import io.harness.generator.OwnerManager.Owners;
import io.harness.generator.Randomizer.Seed;
import io.harness.generator.ServiceGenerator;
import io.harness.generator.SettingGenerator;
import io.harness.generator.WorkflowGenerator;
import io.restassured.http.ContentType;
import io.restassured.mapper.ObjectMapperType;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.ExecutionArgs;
import software.wings.beans.ExecutionCredential.ExecutionType;
import software.wings.beans.GraphNode;
import software.wings.beans.RestResponse;
import software.wings.beans.SSHExecutionCredential;
import software.wings.beans.SettingAttribute;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.WorkflowType;
import software.wings.common.Constants;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.WorkflowService;

import java.util.Collections;
import javax.ws.rs.core.GenericType;

public class JiraCrudTest extends AbstractFunctionalTest {
  @Inject private OwnerManager ownerManager;
  @Inject private ApplicationGenerator applicationGenerator;
  @Inject private EnvironmentGenerator environmentGenerator;
  @Inject private WorkflowGenerator workflowGenerator;
  @Inject private ServiceGenerator serviceGenerator;
  @Inject private SettingGenerator settingGenerator;
  @Inject private AccountGenerator accountGenerator;
  @Inject private WorkflowService workflowService;
  @Inject private SettingsService settingsService;

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
  public void shouldCreateJiraStepinWorkflow() {
    Environment environment = environmentGenerator.ensurePredefined(seed, owners, GENERIC_TEST);
    assertThat(environment).isNotNull();
    Workflow jiraWorkflow =
        aWorkflow()
            .withName("Create Jira")
            .withEnvId(environment.getUuid())
            .withWorkflowType(WorkflowType.ORCHESTRATION)
            .withOrchestrationWorkflow(
                aCanaryOrchestrationWorkflow()
                    .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT, Constants.PRE_DEPLOYMENT).build())
                    .withPostDeploymentSteps(
                        aPhaseStep(POST_DEPLOYMENT, Constants.POST_DEPLOYMENT).addStep(getJiraCreateNode()).build())
                    .build())
            .build();

    // REST API.
    GenericType<RestResponse<Workflow>> workflowType = new GenericType<RestResponse<Workflow>>() {};

    RestResponse<Workflow> savedWorkflowResponse = given()
                                                       .auth()
                                                       .oauth2(bearerToken)
                                                       .queryParam("accountId", application.getAccountId())
                                                       .queryParam("appId", application.getUuid())
                                                       .body(jiraWorkflow, ObjectMapperType.GSON)
                                                       .contentType(ContentType.JSON)
                                                       .post("/workflows")
                                                       .as(workflowType.getType());

    Workflow savedWorkflow = savedWorkflowResponse.getResource();
    assertThat(savedWorkflow).isNotNull();
    assertThat(savedWorkflow.getUuid()).isNotEmpty();
    assertThat(savedWorkflow.getWorkflowType()).isEqualTo(ORCHESTRATION);

    // Test running the workflow

    GenericType<RestResponse<WorkflowExecution>> workflowExecutionType =
        new GenericType<RestResponse<WorkflowExecution>>() {};

    ExecutionArgs executionArgs = new ExecutionArgs();
    executionArgs.setWorkflowType(savedWorkflow.getWorkflowType());
    executionArgs.setExecutionCredential(
        SSHExecutionCredential.Builder.aSSHExecutionCredential().withExecutionType(ExecutionType.SSH).build());
    executionArgs.setOrchestrationId(savedWorkflow.getUuid());

    RestResponse<WorkflowExecution> savedWorkflowExecutionResponse = given()
                                                                         .auth()
                                                                         .oauth2(bearerToken)
                                                                         .queryParam("appId", application.getUuid())
                                                                         .queryParam("envId", environment.getUuid())
                                                                         .contentType(ContentType.JSON)
                                                                         .body(executionArgs, ObjectMapperType.GSON)
                                                                         .post("/executions")
                                                                         .as(workflowExecutionType.getType());

    WorkflowExecution workflowExecution = savedWorkflowExecutionResponse.getResource();
    assertThat(workflowExecution).isNotNull();

    // Check output of the workflow Execution
    WorkflowExecution workflowExecutionDetails;
    do {
      RestResponse<WorkflowExecution> workflowExecutionRestResponse =
          given()
              .auth()
              .oauth2(bearerToken)
              .queryParam("appId", application.getUuid())
              .queryParam("envId", environment.getUuid())
              .get("/executions/" + workflowExecution.getUuid())
              .as(workflowExecutionType.getType());

      workflowExecutionDetails = workflowExecutionRestResponse.getResource();
    } while (workflowExecutionDetails.getStatus() != ExecutionStatus.SUCCESS);

    assertThat(workflowExecutionDetails.getStatus()).isEqualTo(ExecutionStatus.SUCCESS);
  }

  private GraphNode getJiraCreateNode() {
    SettingAttribute jiraSetting = settingGenerator.ensurePredefined(seed, owners, HARNESS_JIRA);
    assertThat(jiraSetting).isNotNull();
    return GraphNode.builder()
        .id(generateUuid())
        .type(JIRA_CREATE_UPDATE.name())
        .name("Create Jira")
        .properties(ImmutableMap.<String, Object>builder()
                        .put("description", "test123")
                        .put("issueType", "Story")
                        .put("jiraAction", "CREATE_TICKET")
                        .put("jiraConnectorId", jiraSetting.getUuid())
                        .put("priority", "P1")
                        .put("project", "TJI")
                        .put("publishAsVar", true)
                        .put("summary", "test")
                        .put("sweepingOutputName", "Jiravar")
                        .put("sweepingOutputScope", "PIPELINE")
                        .put("labels", Collections.singletonList("demo"))
                        .build())
        .build();
  }
}
