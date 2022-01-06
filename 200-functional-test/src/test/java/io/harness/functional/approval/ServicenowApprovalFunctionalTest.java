/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.functional.approval;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.generator.EnvironmentGenerator.Environments.GENERIC_TEST;
import static io.harness.rule.OwnerRule.PRABU;

import static software.wings.beans.CanaryOrchestrationWorkflow.CanaryOrchestrationWorkflowBuilder.aCanaryOrchestrationWorkflow;
import static software.wings.beans.PhaseStep.PhaseStepBuilder.aPhaseStep;
import static software.wings.beans.PhaseStepType.POST_DEPLOYMENT;
import static software.wings.beans.PhaseStepType.PRE_DEPLOYMENT;
import static software.wings.beans.Workflow.WorkflowBuilder.aWorkflow;
import static software.wings.beans.servicenow.ServiceNowFields.DESCRIPTION;
import static software.wings.beans.servicenow.ServiceNowFields.IMPACT;
import static software.wings.beans.servicenow.ServiceNowFields.SHORT_DESCRIPTION;
import static software.wings.beans.servicenow.ServiceNowFields.URGENCY;
import static software.wings.beans.servicenow.ServiceNowFields.WORK_NOTES;
import static software.wings.sm.StateType.APPROVAL;
import static software.wings.sm.StateType.SERVICENOW_CREATE_UPDATE;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.ExecutionStatus;
import io.harness.beans.WorkflowType;
import io.harness.category.element.FunctionalTests;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.generator.ApplicationGenerator;
import io.harness.generator.EnvironmentGenerator;
import io.harness.generator.OwnerManager;
import io.harness.generator.Randomizer;
import io.harness.generator.SettingGenerator;
import io.harness.generator.SettingGenerator.Settings;
import io.harness.rule.Owner;
import io.harness.testframework.restutils.PipelineRestUtils;
import io.harness.testframework.restutils.WorkflowRestUtils;

import software.wings.api.ExecutionDataValue;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.ExecutionArgs;
import software.wings.beans.ExecutionCredential;
import software.wings.beans.GraphNode;
import software.wings.beans.Pipeline;
import software.wings.beans.PipelineStage;
import software.wings.beans.PipelineStage.PipelineStageElement;
import software.wings.beans.SSHExecutionCredential;
import software.wings.beans.SettingAttribute;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.approval.ConditionalOperator;
import software.wings.beans.approval.Criteria;
import software.wings.beans.servicenow.ServiceNowCreateUpdateParams;
import software.wings.beans.servicenow.ServiceNowFields;
import software.wings.delegatetasks.servicenow.ServiceNowAction;
import software.wings.service.impl.servicenow.ServiceNowDelegateServiceImpl;
import software.wings.service.intfc.WorkflowExecutionService;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(CDC)
@Slf4j
public class ServicenowApprovalFunctionalTest extends AbstractFunctionalTest {
  @Inject private OwnerManager ownerManager;
  @Inject private SettingGenerator settingGenerator;
  @Inject private EnvironmentGenerator environmentGenerator;
  @Inject private ApplicationGenerator applicationGenerator;
  @Inject private WorkflowExecutionService workflowExecutionService;
  @Inject private ServiceNowDelegateServiceImpl serviceNowDelegateService;

  private Environment environment;
  private OwnerManager.Owners owners;
  private SettingAttribute snowSetting;
  private Application application;
  private final Randomizer.Seed seed = new Randomizer.Seed(0);

  @Before
  public void setUp() {
    owners = ownerManager.create();
    environment = environmentGenerator.ensurePredefined(seed, owners, GENERIC_TEST);
    assertThat(environment).isNotNull();
    snowSetting = settingGenerator.ensurePredefined(seed, owners, Settings.SERVICENOW_CONNECTOR);
    assertThat(snowSetting).isNotNull();
    application = applicationGenerator.ensurePredefined(seed, owners, ApplicationGenerator.Applications.GENERIC_TEST);
    assertThat(application).isNotNull();
  }

  @Test
  @Owner(developers = PRABU)
  @Category({FunctionalTests.class})
  @Ignore("Functional Flakiness fixing. Needs to be fixed")
  public void ExecuteServiceNowApprovalForMultipleORConditions() {
    Criteria rejectionCriteria = new Criteria();
    rejectionCriteria.setConditions(
        ImmutableMap.of("state", Arrays.asList("Cancelled"), "approval", Arrays.asList("Approved", "Rejected")));

    Criteria approvalCriteria = new Criteria();
    approvalCriteria.setConditions(ImmutableMap.of(
        "state", Arrays.asList("Closed", "Cancelled"), "approval", Arrays.asList("Approved", "Requested")));
    approvalCriteria.setOperator(ConditionalOperator.OR);

    Workflow snowApprovalWorkflow =
        aWorkflow()
            .name("ServiceNow Approval Functional Test" + System.currentTimeMillis())
            .envId(environment.getUuid())
            .workflowType(WorkflowType.ORCHESTRATION)
            .orchestrationWorkflow(
                aCanaryOrchestrationWorkflow()
                    .withPreDeploymentSteps(
                        aPhaseStep(PRE_DEPLOYMENT).addStep(getSnowCreateNode("CHANGE_REQUEST")).build())
                    .withPostDeploymentSteps(
                        aPhaseStep(POST_DEPLOYMENT)
                            .addStep(getSnowApprovalNode(
                                approvalCriteria, rejectionCriteria, 1800000, null, null, "CHANGE_REQUEST"))
                            .addStep(getSnowUpdateNode(ImmutableMap.of("approval", "Approved"), "CHANGE_REQUEST"))
                            .withStepsInParallel(true)
                            .build())
                    .build())
            .build();
    workflowExecuteAndAssert(snowApprovalWorkflow, ExecutionStatus.SUCCESS);
  }

  @Test
  @Owner(developers = PRABU)
  @Category({FunctionalTests.class})
  @Ignore("Functional Flakiness fixing. Needs to be fixed")
  public void ExecuteServiceNowApprovalWithChangeWindowTimeout() {
    Criteria rejectionCriteria = new Criteria();
    rejectionCriteria.setConditions(
        ImmutableMap.of("state", Arrays.asList("Cancelled"), "approval", Arrays.asList("Approved", "Rejected")));

    Criteria approvalCriteria = new Criteria();
    approvalCriteria.setConditions(ImmutableMap.of(
        "state", Arrays.asList("Closed", "Cancelled"), "approval", Arrays.asList("Approved", "Requested")));
    approvalCriteria.setOperator(ConditionalOperator.OR);

    Date startDate = new Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1));
    Date endDate = new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1));
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

    Workflow snowApprovalWorkflow =
        aWorkflow()
            .name("ServiceNow Approval Functional Test" + System.currentTimeMillis())
            .envId(environment.getUuid())
            .workflowType(WorkflowType.ORCHESTRATION)
            .orchestrationWorkflow(
                aCanaryOrchestrationWorkflow()
                    .withPreDeploymentSteps(
                        aPhaseStep(PRE_DEPLOYMENT).addStep(getSnowCreateNode("CHANGE_REQUEST")).build())
                    .withPostDeploymentSteps(
                        aPhaseStep(POST_DEPLOYMENT)
                            .addStep(getSnowUpdateNode(
                                ImmutableMap.of("approval", "Approved", "start_date", dateFormat.format(startDate),
                                    "end_date", dateFormat.format(endDate)),
                                "CHANGE_REQUEST"))
                            .addStep(getSnowApprovalNode(
                                approvalCriteria, rejectionCriteria, 10000, "start_date", "end_date", "CHANGE_REQUEST"))
                            .build())
                    .build())
            .build();
    WorkflowExecution completedExecution = workflowExecuteAndAssert(snowApprovalWorkflow, ExecutionStatus.SUCCESS);
    GraphNode snowApprovalNode =
        completedExecution.getExecutionNode().getNext().getGroup().getElements().get(0).getNext();
    Map<String, Object> executionDetails = (Map<String, Object>) snowApprovalNode.getExecutionDetails();
    assertThat(executionDetails.get("currentStatus"))
        .isEqualTo(ExecutionDataValue.builder()
                       .displayName("Current value")
                       .value("Approval is Approved,\nState is New")
                       .build());
    assertThat(executionDetails.get("approvalCriteria"))
        .isEqualTo(
            ExecutionDataValue.builder()
                .displayName("Approval Criteria")
                .value("State should be any of Closed/Cancelled or\nApproval should be any of Approved/Requested")
                .build());
    assertThat(executionDetails.get("rejectionCriteria"))
        .isEqualTo(ExecutionDataValue.builder()
                       .displayName("Rejection Criteria")
                       .value("State should be Cancelled and\nApproval should be any of Approved/Rejected")
                       .build());
  }

  @Test
  @Owner(developers = PRABU)
  @Category({FunctionalTests.class})
  @Ignore("Functional Flakiness fixing. Needs to be fixed")
  public void ExecuteServiceNowApprovalWithChangingTimeWindowValuesForChange() {
    Criteria rejectionCriteria = new Criteria();
    rejectionCriteria.setConditions(
        ImmutableMap.of("state", Arrays.asList("Cancelled"), "approval", Arrays.asList("Approved", "Rejected")));

    Criteria approvalCriteria = new Criteria();
    approvalCriteria.setConditions(ImmutableMap.of(
        "state", Arrays.asList("Closed", "Cancelled"), "approval", Arrays.asList("Approved", "Requested")));
    approvalCriteria.setOperator(ConditionalOperator.OR);

    Date startDate = new Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1));
    Date endDate = new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1));
    Date endDate2 = new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(2));
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

    Workflow snowApprovalWorkflow =
        aWorkflow()
            .name("ServiceNow Approval Functional Test" + System.currentTimeMillis())
            .envId(environment.getUuid())
            .workflowType(WorkflowType.ORCHESTRATION)
            .orchestrationWorkflow(
                aCanaryOrchestrationWorkflow()
                    .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT)
                                                .addStep(getSnowCreateNode("CHANGE_REQUEST"))
                                                .addStep(getSnowUpdateNode(ImmutableMap.of("approval", "Approved",
                                                                               "start_date", dateFormat.format(endDate),
                                                                               "end_date", dateFormat.format(endDate2)),
                                                    "CHANGE_REQUEST"))
                                                .build())
                    .withPostDeploymentSteps(
                        aPhaseStep(POST_DEPLOYMENT)
                            .addStep(getSnowUpdateNode(
                                ImmutableMap.of("approval", "Approved", "start_date", dateFormat.format(startDate),
                                    "end_date", dateFormat.format(endDate)),
                                "CHANGE_REQUEST"))
                            .addStep(getSnowApprovalNode(approvalCriteria, rejectionCriteria, 6000000, "start_date",
                                "end_date", "CHANGE_REQUEST"))
                            .withStepsInParallel(true)
                            .build())
                    .build())
            .build();
    workflowExecuteAndAssert(snowApprovalWorkflow, ExecutionStatus.SUCCESS);
  }

  @Test
  @Owner(developers = PRABU)
  @Category({FunctionalTests.class})
  @Ignore("Functional Flakiness fixing. Needs to be fixed")
  public void ThrowExceptionForInvalidChangeWindowValues() {
    Criteria rejectionCriteria = new Criteria();
    rejectionCriteria.setConditions(
        ImmutableMap.of("state", Arrays.asList("Cancelled"), "approval", Arrays.asList("Approved", "Rejected")));

    Criteria approvalCriteria = new Criteria();
    approvalCriteria.setConditions(ImmutableMap.of(
        "state", Arrays.asList("Closed", "Cancelled"), "approval", Arrays.asList("Approved", "Requested")));
    approvalCriteria.setOperator(ConditionalOperator.OR);

    Date startDate = new Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1));
    Date endDate = new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1));
    Date endDate2 = new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(2));
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

    Workflow snowApprovalWorkflow =
        aWorkflow()
            .name("ServiceNow Approval Functional Test" + System.currentTimeMillis())
            .envId(environment.getUuid())
            .workflowType(WorkflowType.ORCHESTRATION)
            .orchestrationWorkflow(
                aCanaryOrchestrationWorkflow()
                    .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT)
                                                .addStep(getSnowCreateNode("CHANGE_REQUEST"))
                                                .addStep(getSnowUpdateNode(ImmutableMap.of("approval", "Approved",
                                                                               "start_date", dateFormat.format(endDate),
                                                                               "end_date", dateFormat.format(endDate2)),
                                                    "CHANGE_REQUEST"))
                                                .build())
                    .withPostDeploymentSteps(
                        aPhaseStep(POST_DEPLOYMENT)
                            .addStep(getSnowUpdateNode(
                                ImmutableMap.of("approval", "Approved", "start_date", dateFormat.format(startDate),
                                    "end_date", dateFormat.format(endDate)),
                                "CHANGE_REQUEST"))
                            .addStep(getSnowApprovalNode(
                                approvalCriteria, rejectionCriteria, 10000, "start_day", "end_date", "CHANGE_REQUEST"))
                            .withStepsInParallel(true)
                            .build())
                    .build())
            .build();

    WorkflowExecution completedExecution = workflowExecuteAndAssert(snowApprovalWorkflow, ExecutionStatus.FAILED);
    GraphNode snowApprovalNode =
        completedExecution.getExecutionNode().getNext().getGroup().getElements().get(0).getGroup().getElements().get(1);
    Map<String, Object> executionDetails = (Map<String, Object>) snowApprovalNode.getExecutionDetails();
    assertThat(executionDetails.get("errorMsg"))
        .isEqualTo(
            ExecutionDataValue.builder().displayName("Message").value("Time Window fields given are invalid").build());
  }

  @Test
  @Owner(developers = PRABU)
  @Category({FunctionalTests.class})
  @Ignore("Functional Flakiness fixing. Needs to be fixed")
  public void ExecuteServiceNowApprovalForExpiredButWaitingForTimeWindow() {
    Criteria rejectionCriteria = new Criteria();
    rejectionCriteria.setConditions(
        ImmutableMap.of("state", Arrays.asList("Cancelled"), "approval", Arrays.asList("Approved", "Rejected")));

    Criteria approvalCriteria = new Criteria();
    approvalCriteria.setConditions(ImmutableMap.of(
        "state", Arrays.asList("Closed", "Cancelled"), "approval", Arrays.asList("Approved", "Requested")));
    approvalCriteria.setOperator(ConditionalOperator.OR);

    Date startDate = new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1));
    Date endDate = new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(2));
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

    Workflow snowApprovalWorkflow =
        aWorkflow()
            .name("ServiceNow Approval Functional Test" + System.currentTimeMillis())
            .envId(environment.getUuid())
            .workflowType(WorkflowType.ORCHESTRATION)
            .orchestrationWorkflow(
                aCanaryOrchestrationWorkflow()
                    .withPreDeploymentSteps(
                        aPhaseStep(PRE_DEPLOYMENT).addStep(getSnowCreateNode("CHANGE_REQUEST")).build())
                    .withPostDeploymentSteps(
                        aPhaseStep(POST_DEPLOYMENT)
                            .addStep(getSnowUpdateNode(
                                ImmutableMap.of("approval", "Approved", "start_date", dateFormat.format(startDate),
                                    "end_date", dateFormat.format(endDate)),
                                "CHANGE_REQUEST"))
                            .addStep(getSnowApprovalNode(
                                approvalCriteria, rejectionCriteria, 10000, "start_date", "end_date", "CHANGE_REQUEST"))
                            .build())
                    .build())
            .build();

    ExecutionArgs executionArgs = saveWorkflowAndGetExecutionArgs(snowApprovalWorkflow);

    WorkflowExecution workflowExecution =
        WorkflowRestUtils.startWorkflow(bearerToken, environment.getAppId(), environment.getUuid(), executionArgs);

    log.info("Workflow Execution started");

    Awaitility.await().atMost(600, TimeUnit.SECONDS).pollInterval(60, TimeUnit.SECONDS).until(() -> {
      ExecutionStatus executionStatus =
          workflowExecutionService.getWorkflowExecution(environment.getAppId(), workflowExecution.getUuid())
              .getStatus();
      log.info("Current workflow execution status: {}", executionStatus.name());
      return executionStatus == ExecutionStatus.PAUSED;
    });
  }

  @Test
  @Owner(developers = PRABU)
  @Category({FunctionalTests.class})
  @Ignore("Functional Flakiness fixing. Needs to be fixed")
  public void ExecuteServiceNowApprovalWithChangingTimeWindowValuesOtherTicketTypes() {
    Criteria rejectionCriteria = new Criteria();
    rejectionCriteria.setConditions(
        ImmutableMap.of("state", Arrays.asList("Cancelled"), "approval", Arrays.asList("Approved", "Rejected")));

    Criteria approvalCriteria = new Criteria();
    approvalCriteria.setConditions(ImmutableMap.of(
        "state", Arrays.asList("Closed", "Cancelled"), "approval", Arrays.asList("Approved", "Requested")));
    approvalCriteria.setOperator(ConditionalOperator.OR);

    Date startDate = new Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1));
    Date endDate = new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1));
    Date endDate2 = new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(2));
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

    String ticketType = "INCIDENT";
    Workflow snowApprovalWorkflow =
        aWorkflow()
            .name("ServiceNow Approval Functional Test" + System.currentTimeMillis())
            .envId(environment.getUuid())
            .workflowType(WorkflowType.ORCHESTRATION)
            .orchestrationWorkflow(
                aCanaryOrchestrationWorkflow()
                    .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT)
                                                .addStep(getSnowCreateNode(ticketType))
                                                .addStep(getSnowUpdateNode(ImmutableMap.of("approval", "Approved",
                                                                               "work_start", dateFormat.format(endDate),
                                                                               "work_end", dateFormat.format(endDate2)),
                                                    ticketType))
                                                .build())
                    .withPostDeploymentSteps(
                        aPhaseStep(POST_DEPLOYMENT)
                            .addStep(getSnowUpdateNode(
                                ImmutableMap.of("approval", "Approved", "work_start", dateFormat.format(startDate),
                                    "work_end", dateFormat.format(endDate)),
                                ticketType))
                            .addStep(getSnowApprovalNode(
                                approvalCriteria, rejectionCriteria, 10000, "work_start", "work_end", ticketType))
                            .withStepsInParallel(true)
                            .build())
                    .build())
            .build();
    workflowExecuteAndAssert(snowApprovalWorkflow, ExecutionStatus.SUCCESS);

    ticketType = "PROBLEM";
    Workflow snowApprovalWorkflow2 =
        aWorkflow()
            .name("ServiceNow Approval Functional Test" + System.currentTimeMillis())
            .envId(environment.getUuid())
            .workflowType(WorkflowType.ORCHESTRATION)
            .orchestrationWorkflow(
                aCanaryOrchestrationWorkflow()
                    .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT)
                                                .addStep(getSnowCreateNode(ticketType))
                                                .addStep(getSnowUpdateNode(ImmutableMap.of("approval", "Approved",
                                                                               "work_start", dateFormat.format(endDate),
                                                                               "work_end", dateFormat.format(endDate2)),
                                                    ticketType))
                                                .build())
                    .withPostDeploymentSteps(
                        aPhaseStep(POST_DEPLOYMENT)
                            .addStep(getSnowUpdateNode(
                                ImmutableMap.of("approval", "Approved", "work_start", dateFormat.format(startDate),
                                    "work_end", dateFormat.format(endDate)),
                                ticketType))
                            .addStep(getSnowApprovalNode(
                                approvalCriteria, rejectionCriteria, 10000, "work_start", "work_end", ticketType))
                            .withStepsInParallel(true)
                            .build())
                    .build())
            .build();
    workflowExecuteAndAssert(snowApprovalWorkflow2, ExecutionStatus.SUCCESS);
  }

  @Test
  @Owner(developers = PRABU)
  @Category({FunctionalTests.class})
  @Ignore("After migration to bazel this test seems to be slower and intermittenly timesout")
  public void ExecuteServiceNowRejectionForMultipleANDConditions() {
    Criteria rejectionCriteria = new Criteria();
    rejectionCriteria.setConditions(
        ImmutableMap.of("state", Arrays.asList("Canceled", "Assess"), "approval", Arrays.asList("Rejected")));

    Criteria approvalCriteria = new Criteria();
    approvalCriteria.setConditions(
        ImmutableMap.of("state", Arrays.asList("Closed", "Canceled"), "approval", Arrays.asList("Approved")));

    Workflow snowApprovalWorkflow =
        aWorkflow()
            .name("ServiceNow Rejection Functional Test" + System.currentTimeMillis())
            .envId(environment.getUuid())
            .workflowType(WorkflowType.ORCHESTRATION)
            .orchestrationWorkflow(
                aCanaryOrchestrationWorkflow()
                    .withPreDeploymentSteps(
                        aPhaseStep(PRE_DEPLOYMENT).addStep(getSnowCreateNode("CHANGE_REQUEST")).build())
                    .withPostDeploymentSteps(
                        aPhaseStep(POST_DEPLOYMENT)
                            .addStep(getSnowApprovalNode(
                                approvalCriteria, rejectionCriteria, 1800000, null, null, "CHANGE_REQUEST"))
                            .addStep(getSnowUpdateNode(
                                ImmutableMap.of("state", "Canceled", "approval", "Rejected"), "CHANGE_REQUEST"))
                            .withStepsInParallel(true)
                            .build())
                    .build())
            .build();
    workflowExecuteAndAssert(snowApprovalWorkflow, ExecutionStatus.REJECTED);
  }

  @Test
  @Owner(developers = PRABU)
  @Category({FunctionalTests.class})
  @Ignore("Functional Flakiness fixing. Needs to be fixed")
  public void ExecuteServiceNowApprovalPipelineForMultipleANDConditions() {
    Criteria rejectionCriteria = new Criteria();
    rejectionCriteria.setConditions(
        ImmutableMap.of("state", Arrays.asList("Canceled", "Assess"), "approval", Arrays.asList("Rejected")));

    Criteria approvalCriteria = new Criteria();
    approvalCriteria.setConditions(
        ImmutableMap.of("state", Arrays.asList("Closed", "Canceled"), "approval", Arrays.asList("Approved")));

    Workflow snowCreateWorkflow =
        aWorkflow()
            .name("ServiceNow Approval Create Workflow" + System.currentTimeMillis())
            .uuid("WORKFLOW_ID1" + System.currentTimeMillis())
            .envId(environment.getUuid())
            .workflowType(WorkflowType.ORCHESTRATION)
            .orchestrationWorkflow(
                aCanaryOrchestrationWorkflow()
                    .withPreDeploymentSteps(
                        aPhaseStep(PRE_DEPLOYMENT).addStep(getSnowCreateNode("CHANGE_REQUEST")).build())
                    .build())
            .build();

    Workflow snowUpdateWorkflow =
        aWorkflow()
            .name("ServiceNow Approval Update workflow" + System.currentTimeMillis())
            .uuid("WORKFLOW_ID2" + System.currentTimeMillis())
            .envId(environment.getUuid())
            .workflowType(WorkflowType.ORCHESTRATION)
            .orchestrationWorkflow(
                aCanaryOrchestrationWorkflow()
                    .withPostDeploymentSteps(
                        aPhaseStep(POST_DEPLOYMENT)
                            .addStep(getSnowUpdateNode(
                                ImmutableMap.of("state", "Canceled", "approval", "Approved"), "CHANGE_REQUEST"))
                            .build())
                    .build())
            .build();

    saveWorkflowAndGetExecutionArgs(snowCreateWorkflow);
    saveWorkflowAndGetExecutionArgs(snowUpdateWorkflow);

    Pipeline snowApprovalPipeline =
        Pipeline.builder()
            .uuid("PIPELINE_ID")
            .name("ServiceNow Approval Pipeline " + System.currentTimeMillis())
            .pipelineStages(Arrays.asList(
                PipelineStage.builder()
                    .pipelineStageElements(Collections.singletonList(
                        PipelineStageElement.builder()
                            .name("STAGE 1")
                            .type("ENV_STATE")
                            .properties(Collections.singletonMap("workflowId", snowCreateWorkflow.getUuid()))
                            .build()))
                    .build(),
                PipelineStage.builder()
                    .pipelineStageElements(Collections.singletonList(
                        PipelineStageElement.builder()
                            .type("APPROVAL")
                            .name("STAGE 2")
                            .properties(ImmutableMap.of("approvalStateParams",
                                getApprovalParams(approvalCriteria, rejectionCriteria, "CHANGE_REQUEST", null, null),
                                "approvalStateType", "SERVICENOW"))
                            .build()))
                    .build(),
                PipelineStage.builder()
                    .parallel(true)
                    .pipelineStageElements(Collections.singletonList(
                        PipelineStageElement.builder()
                            .name("STAGE 3")
                            .type("ENV_STATE")
                            .properties(Collections.singletonMap("workflowId", snowUpdateWorkflow.getUuid()))
                            .build()))
                    .build()))
            .build();

    Pipeline savedPipeline = PipelineRestUtils.createPipeline(
        application.getUuid(), snowApprovalPipeline, application.getAccountId(), bearerToken);

    ExecutionArgs executionArgs = new ExecutionArgs();
    executionArgs.setPipelineId(savedPipeline.getUuid());
    executionArgs.setWorkflowType(WorkflowType.PIPELINE);

    WorkflowExecution workflowExecution =
        runPipeline(bearerToken, application.getUuid(), environment.getUuid(), executionArgs);
    assertThat(workflowExecution.getStatus()).isEqualTo(ExecutionStatus.SUCCESS);
  }

  @Test
  @Owner(developers = PRABU)
  @Category({FunctionalTests.class})
  @Ignore("Functional Flakiness fixing. Needs to be fixed")
  public void ExecuteServiceNowRejectionPipelineForMultipleORConditions() {
    Criteria rejectionCriteria = new Criteria();
    rejectionCriteria.setConditions(
        ImmutableMap.of("state", Arrays.asList("Canceled", "Assess"), "approval", Arrays.asList("Rejected")));
    rejectionCriteria.setOperator(ConditionalOperator.OR);

    Criteria approvalCriteria = new Criteria();
    approvalCriteria.setConditions(
        ImmutableMap.of("state", Arrays.asList("Closed", "Canceled"), "approval", Arrays.asList("Approved")));

    Workflow snowCreateWorkflow =
        aWorkflow()
            .name("ServiceNow Approval Create Workflow" + System.currentTimeMillis())
            .uuid("WORKFLOW_ID1" + System.currentTimeMillis())
            .envId(environment.getUuid())
            .workflowType(WorkflowType.ORCHESTRATION)
            .orchestrationWorkflow(
                aCanaryOrchestrationWorkflow()
                    .withPreDeploymentSteps(
                        aPhaseStep(PRE_DEPLOYMENT).addStep(getSnowCreateNode("CHANGE_REQUEST")).build())
                    .build())
            .build();

    Workflow snowUpdateWorkflow =
        aWorkflow()
            .name("ServiceNow Approval Update workflow" + System.currentTimeMillis())
            .uuid("WORKFLOW_ID2" + System.currentTimeMillis())
            .envId(environment.getUuid())
            .workflowType(WorkflowType.ORCHESTRATION)
            .orchestrationWorkflow(
                aCanaryOrchestrationWorkflow()
                    .withPostDeploymentSteps(
                        aPhaseStep(POST_DEPLOYMENT)
                            .addStep(getSnowUpdateNode(ImmutableMap.of("state", "Canceled"), "CHANGE_REQUEST"))
                            .build())
                    .build())
            .build();

    saveWorkflowAndGetExecutionArgs(snowCreateWorkflow);
    saveWorkflowAndGetExecutionArgs(snowUpdateWorkflow);

    Pipeline snowApprovalPipeline =
        Pipeline.builder()
            .uuid("PIPELINE_ID")
            .name("ServiceNow Approval Pipeline " + System.currentTimeMillis())
            .pipelineStages(Arrays.asList(
                PipelineStage.builder()
                    .pipelineStageElements(Collections.singletonList(
                        PipelineStageElement.builder()
                            .name("STAGE 1")
                            .type("ENV_STATE")
                            .properties(Collections.singletonMap("workflowId", snowCreateWorkflow.getUuid()))
                            .build()))
                    .build(),
                PipelineStage.builder()
                    .pipelineStageElements(Collections.singletonList(
                        PipelineStageElement.builder()
                            .type("APPROVAL")
                            .name("STAGE 2")
                            .properties(ImmutableMap.of("approvalStateParams",
                                getApprovalParams(approvalCriteria, rejectionCriteria, "CHANGE_REQUEST", null, null),
                                "approvalStateType", "SERVICENOW"))
                            .build()))
                    .build(),
                PipelineStage.builder()
                    .parallel(true)
                    .pipelineStageElements(Collections.singletonList(
                        PipelineStageElement.builder()
                            .name("STAGE 3")
                            .type("ENV_STATE")
                            .properties(Collections.singletonMap("workflowId", snowUpdateWorkflow.getUuid()))
                            .build()))
                    .build()))
            .build();

    Pipeline savedPipeline = PipelineRestUtils.createPipeline(
        application.getUuid(), snowApprovalPipeline, application.getAccountId(), bearerToken);

    ExecutionArgs executionArgs = new ExecutionArgs();
    executionArgs.setPipelineId(savedPipeline.getUuid());
    executionArgs.setWorkflowType(WorkflowType.PIPELINE);

    WorkflowExecution workflowExecution =
        runPipeline(bearerToken, application.getUuid(), environment.getUuid(), executionArgs);
    assertThat(workflowExecution.getStatus()).isEqualTo(ExecutionStatus.REJECTED);
  }

  private GraphNode getSnowApprovalNode(
      Criteria approval, Criteria rejection, int timeoutMillis, String start, String end, String ticketType) {
    HashMap<String, Object> approvalStateParams = getApprovalParams(approval, rejection, ticketType, start, end);

    Map<String, Object> properties = new HashMap<>();
    properties.put("approvalStateParams", approvalStateParams);
    properties.put("approvalStateType", "SERVICENOW");
    properties.put("timeoutMillis", timeoutMillis);

    return GraphNode.builder()
        .id(generateUuid())
        .type(APPROVAL.name())
        .name("Approval Snow")
        .properties(properties)
        .build();
  }

  @NotNull
  private HashMap<String, Object> getApprovalParams(
      Criteria approval, Criteria rejection, String ticketType, String start, String end) {
    HashMap<String, Object> serviceNowApprovalParams = new HashMap<>();
    serviceNowApprovalParams.put("approval", approval);
    serviceNowApprovalParams.put("issueNumber", "${snowIssue.issueNumber}");
    serviceNowApprovalParams.put("rejection", rejection);
    serviceNowApprovalParams.put("ticketType", ticketType);
    serviceNowApprovalParams.put("snowConnectorId", snowSetting.getUuid());

    if (start != null) {
      serviceNowApprovalParams.put("changeWindowPresent", true);
      serviceNowApprovalParams.put("changeWindowStartField", start);
      serviceNowApprovalParams.put("changeWindowEndField", end);
    }

    HashMap<String, Object> approvalStateParams = new HashMap<>();
    approvalStateParams.put("serviceNowApprovalParams", serviceNowApprovalParams);
    return approvalStateParams;
  }

  private GraphNode getSnowUpdateNode(Map<String, String> updateValues, String ticketType) {
    ServiceNowCreateUpdateParams params = new ServiceNowCreateUpdateParams();
    params.setAction(ServiceNowAction.UPDATE);
    params.setSnowConnectorId(snowSetting.getUuid());
    params.setTicketType(ticketType);
    params.setIssueNumber("${snowIssue.issueNumber}");
    Map<ServiceNowFields, String> fields = new HashMap<>();
    Map<String, String> additionalFields = new HashMap<>();
    for (Map.Entry<String, String> updateEntry : updateValues.entrySet()) {
      try {
        fields.put(ServiceNowFields.valueOf(updateEntry.getKey()), updateEntry.getValue());
        fields.put(WORK_NOTES, "Started progress");
      } catch (IllegalArgumentException e) {
        additionalFields.put(updateEntry.getKey(), updateEntry.getValue());
      }
    }
    params.setFields(fields);
    params.setAdditionalFields(additionalFields);
    return GraphNode.builder()
        .id(generateUuid())
        .type(SERVICENOW_CREATE_UPDATE.name())
        .name("Update")
        .properties(ImmutableMap.<String, Object>builder().put("serviceNowCreateUpdateParams", params).build())
        .build();
  }

  private GraphNode getSnowCreateNode(String ticketType) {
    ServiceNowCreateUpdateParams params = new ServiceNowCreateUpdateParams();
    params.setAction(ServiceNowAction.CREATE);
    params.setSnowConnectorId(snowSetting.getUuid());
    params.setTicketType(ticketType);
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
                        .put("sweepingOutputName", "snowIssue")
                        .put("sweepingOutputScope", "PIPELINE")
                        .build())
        .build();
  }

  private WorkflowExecution workflowExecuteAndAssert(Workflow workflow, ExecutionStatus status) {
    ExecutionArgs executionArgs = saveWorkflowAndGetExecutionArgs(workflow);

    WorkflowExecution workflowExecution =
        WorkflowRestUtils.startWorkflow(bearerToken, environment.getAppId(), environment.getUuid(), executionArgs);

    log.info("Workflow Execution started");

    Awaitility.await().atMost(600, TimeUnit.SECONDS).pollInterval(20, TimeUnit.SECONDS).until(() -> {
      ExecutionStatus executionStatus =
          workflowExecutionService.getWorkflowExecution(environment.getAppId(), workflowExecution.getUuid())
              .getStatus();
      log.info("Current workflow execution status: {}", executionStatus.name());
      return executionStatus == status || ExecutionStatus.isFinalStatus(executionStatus);
    });

    log.info("Workflow Execution completed successfully");
    WorkflowExecution completedExecution =
        workflowExecutionService.getExecutionDetails(environment.getAppId(), workflowExecution.getUuid(), false, false);

    assertThat(completedExecution.getStatus()).isEqualTo(status);

    assertThat(completedExecution).isNotNull();
    return completedExecution;
  }

  @NotNull
  private ExecutionArgs saveWorkflowAndGetExecutionArgs(Workflow workflow) {
    Workflow savedWorkflow =
        WorkflowRestUtils.createWorkflow(bearerToken, environment.getAccountId(), environment.getAppId(), workflow);
    log.info("Workflow created successfully");
    assertThat(savedWorkflow).isNotNull();

    ExecutionArgs executionArgs = new ExecutionArgs();
    executionArgs.setWorkflowType(savedWorkflow.getWorkflowType());
    executionArgs.setExecutionCredential(SSHExecutionCredential.Builder.aSSHExecutionCredential()
                                             .withExecutionType(ExecutionCredential.ExecutionType.SSH)
                                             .build());
    executionArgs.setOrchestrationId(savedWorkflow.getUuid());
    return executionArgs;
  }
}
