/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.functional.servicenow;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.POOJA;

import static software.wings.beans.CanaryOrchestrationWorkflow.CanaryOrchestrationWorkflowBuilder.aCanaryOrchestrationWorkflow;
import static software.wings.beans.PhaseStep.PhaseStepBuilder.aPhaseStep;
import static software.wings.beans.PhaseStepType.POST_DEPLOYMENT;
import static software.wings.beans.PhaseStepType.PRE_DEPLOYMENT;
import static software.wings.beans.Workflow.WorkflowBuilder.aWorkflow;
import static software.wings.beans.servicenow.ServiceNowFields.DESCRIPTION;
import static software.wings.beans.servicenow.ServiceNowFields.IMPACT;
import static software.wings.beans.servicenow.ServiceNowFields.SHORT_DESCRIPTION;
import static software.wings.beans.servicenow.ServiceNowFields.STATE;
import static software.wings.beans.servicenow.ServiceNowFields.URGENCY;
import static software.wings.beans.servicenow.ServiceNowFields.WORK_NOTES;
import static software.wings.sm.StateType.SERVICENOW_CREATE_UPDATE;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.ExecutionStatus;
import io.harness.beans.WorkflowType;
import io.harness.category.element.FunctionalTests;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.generator.ApplicationGenerator;
import io.harness.generator.ApplicationGenerator.Applications;
import io.harness.generator.EnvironmentGenerator;
import io.harness.generator.EnvironmentGenerator.Environments;
import io.harness.generator.OwnerManager;
import io.harness.generator.OwnerManager.Owners;
import io.harness.generator.Randomizer.Seed;
import io.harness.generator.SettingGenerator;
import io.harness.generator.SettingGenerator.Settings;
import io.harness.rule.Owner;
import io.harness.testframework.restutils.WorkflowRestUtils;

import software.wings.api.ExecutionDataValue;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.ExecutionArgs;
import software.wings.beans.ExecutionCredential.ExecutionType;
import software.wings.beans.GraphNode;
import software.wings.beans.SSHExecutionCredential;
import software.wings.beans.SettingAttribute;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.servicenow.ServiceNowCreateUpdateParams;
import software.wings.beans.servicenow.ServiceNowFields;
import software.wings.delegatetasks.servicenow.ServiceNowAction;
import software.wings.service.intfc.WorkflowExecutionService;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(CDC)
@Slf4j
public class ServiceNowCrudTest extends AbstractFunctionalTest {
  @Inject private OwnerManager ownerManager;
  @Inject private ApplicationGenerator applicationGenerator;
  @Inject private EnvironmentGenerator environmentGenerator;
  @Inject private SettingGenerator settingGenerator;
  @Inject private WorkflowExecutionService workflowExecutionService;

  private final Seed seed = new Seed(0);
  private Application application;
  private Environment environment;
  private Owners owners;
  private SettingAttribute setting;

  @Before
  public void setUp() {
    owners = ownerManager.create();

    application = applicationGenerator.ensurePredefined(seed, owners, Applications.FUNCTIONAL_TEST);
    assertThat(application).isNotNull();
    resetCache(application.getAccountId());
    log.info("Application generated successfully");

    environment = environmentGenerator.ensurePredefined(seed, owners, Environments.FUNCTIONAL_TEST);
    assertThat(environment).isNotNull();
    resetCache(environment.getAccountId());
    log.info("Environment generated successfully");

    setting = settingGenerator.ensurePredefined(seed, owners, Settings.SERVICENOW_CONNECTOR);
    assertThat(setting).isNotNull();
    resetCache(setting.getAccountId());
    log.info("Servicenow Connector generated successfully");
  }

  @Test
  @Owner(developers = POOJA, intermittent = true)
  @Category(FunctionalTests.class)
  public void shouldCreateSnowTicket() throws Exception {
    Workflow snowFTWorkflow =
        aWorkflow()
            .name("ServiceNow Functional Test" + System.currentTimeMillis())
            .envId(environment.getUuid())
            .workflowType(WorkflowType.ORCHESTRATION)
            .orchestrationWorkflow(
                aCanaryOrchestrationWorkflow()
                    .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT).addStep(getSnowCreateNode()).build())
                    .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT).addStep(getSnowUpdateNode()).build())
                    .build())
            .build();

    Workflow savedWorkflow = WorkflowRestUtils.createWorkflow(
        bearerToken, application.getAccountId(), application.getUuid(), snowFTWorkflow);
    log.info("Workflow created successfully: {}", savedWorkflow.getUuid());

    ExecutionArgs executionArgs = new ExecutionArgs();
    executionArgs.setWorkflowType(savedWorkflow.getWorkflowType());
    executionArgs.setExecutionCredential(
        SSHExecutionCredential.Builder.aSSHExecutionCredential().withExecutionType(ExecutionType.SSH).build());
    executionArgs.setOrchestrationId(savedWorkflow.getUuid());

    WorkflowExecution workflowExecution =
        WorkflowRestUtils.startWorkflow(bearerToken, application.getUuid(), environment.getUuid(), executionArgs);
    log.info("Workflow started successfully: {}", savedWorkflow.getUuid());

    Awaitility.await().atMost(120, TimeUnit.SECONDS).pollInterval(5, TimeUnit.SECONDS).until(() -> {
      final WorkflowExecution currentWorkflowExecution =
          workflowExecutionService.getWorkflowExecution(application.getUuid(), workflowExecution.getUuid());
      log.info("Current workflow execution status: {}", currentWorkflowExecution.getStatus());
      return currentWorkflowExecution.getStatus() == ExecutionStatus.SUCCESS;
    });

    log.info("Workflow completed successfully: {}", savedWorkflow.getUuid());

    WorkflowExecution completedExecution =
        workflowExecutionService.getExecutionDetails(application.getUuid(), workflowExecution.getUuid(), false, false);

    log.info("Workflow execution details fetched successfully: {}", savedWorkflow.getUuid());

    assertThat(completedExecution).isNotNull();
    Map<String, ExecutionDataValue> executionSummary =
        (Map<String, ExecutionDataValue>) completedExecution.getExecutionNode()
            .getGroup()
            .getElements()
            .get(0)
            .getExecutionSummary();
    String issueUrl = (String) executionSummary.get("issueUrl").getValue();
    assertThat(issueUrl).isNotNull();
    URL url = new URL(issueUrl);
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setRequestMethod("GET");
    connection.connect();
    int code = connection.getResponseCode();
    assertThat(code).isEqualTo(200);
  }

  private GraphNode getSnowUpdateNode() {
    ServiceNowCreateUpdateParams params = new ServiceNowCreateUpdateParams();
    params.setAction(ServiceNowAction.UPDATE);
    params.setSnowConnectorId(setting.getUuid());
    params.setTicketType("INCIDENT");
    params.setIssueNumber("${snow.issueNumber}");
    Map<ServiceNowFields, String> fields = new HashMap<>();
    fields.put(STATE, "In Progress");
    fields.put(WORK_NOTES, "Started progress");

    params.setFields(fields);
    return GraphNode.builder()
        .id(generateUuid())
        .type(SERVICENOW_CREATE_UPDATE.name())
        .name("Update")
        .properties(ImmutableMap.<String, Object>builder().put("serviceNowCreateUpdateParams", params).build())
        .build();
  }

  private GraphNode getSnowCreateNode() {
    ServiceNowCreateUpdateParams params = new ServiceNowCreateUpdateParams();
    params.setAction(ServiceNowAction.CREATE);
    params.setSnowConnectorId(setting.getUuid());
    params.setTicketType("INCIDENT");
    Map<ServiceNowFields, String> fields = new HashMap<>();
    fields.put(URGENCY, "3");
    fields.put(IMPACT, "1");
    fields.put(SHORT_DESCRIPTION, "${workflow.name}");
    fields.put(DESCRIPTION, "${deploymentUrl}");
    params.setFields(fields);
    return GraphNode.builder()
        .id(generateUuid())
        .type(SERVICENOW_CREATE_UPDATE.name())
        .name("Create")
        .properties(ImmutableMap.<String, Object>builder()
                        .put("serviceNowCreateUpdateParams", params)
                        .put("publishAsVar", true)
                        .put("sweepingOutputName", "snow")
                        .put("sweepingOutputScope", "WORKFLOW")
                        .build())
        .build();
  }
}
