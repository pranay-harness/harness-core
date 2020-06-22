package io.harness.functional.approval;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.generator.EnvironmentGenerator.Environments.GENERIC_TEST;
import static io.harness.rule.OwnerRule.PRABU;
import static org.assertj.core.api.Assertions.assertThat;
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

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

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
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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
                    .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT).addStep(getSnowCreateNode()).build())
                    .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT)
                                                 .addStep(getSnowApprovalNode(approvalCriteria, rejectionCriteria))
                                                 .addStep(getSnowUpdateNode(ImmutableMap.of("approval", "Approved")))
                                                 .withStepsInParallel(true)
                                                 .build())
                    .build())
            .build();
    workflowExecuteAndAssert(snowApprovalWorkflow, ExecutionStatus.SUCCESS);
  }

  @Test
  @Owner(developers = PRABU)
  @Category({FunctionalTests.class})
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
                    .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT).addStep(getSnowCreateNode()).build())
                    .withPostDeploymentSteps(
                        aPhaseStep(POST_DEPLOYMENT)
                            .addStep(getSnowApprovalNode(approvalCriteria, rejectionCriteria))
                            .addStep(getSnowUpdateNode(ImmutableMap.of("state", "Canceled", "approval", "Rejected")))
                            .withStepsInParallel(true)
                            .build())
                    .build())
            .build();
    workflowExecuteAndAssert(snowApprovalWorkflow, ExecutionStatus.REJECTED);
  }

  @Test
  @Owner(developers = PRABU)
  @Category({FunctionalTests.class})
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
                    .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT).addStep(getSnowCreateNode()).build())
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
                            .addStep(getSnowUpdateNode(ImmutableMap.of("state", "Canceled", "approval", "Approved")))
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
                    .pipelineStageElements(
                        Collections.singletonList(PipelineStageElement.builder()
                                                      .type("APPROVAL")
                                                      .name("STAGE 2")
                                                      .properties(ImmutableMap.of("approvalStateParams",
                                                          getApprovalParams(approvalCriteria, rejectionCriteria),
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
                    .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT).addStep(getSnowCreateNode()).build())
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
                    .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT)
                                                 .addStep(getSnowUpdateNode(ImmutableMap.of("state", "Canceled")))
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
                    .pipelineStageElements(
                        Collections.singletonList(PipelineStageElement.builder()
                                                      .type("APPROVAL")
                                                      .name("STAGE 2")
                                                      .properties(ImmutableMap.of("approvalStateParams",
                                                          getApprovalParams(approvalCriteria, rejectionCriteria),
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

  private GraphNode getSnowApprovalNode(Criteria approval, Criteria rejection) {
    HashMap<String, Object> approvalStateParams = getApprovalParams(approval, rejection);

    return GraphNode.builder()
        .id(generateUuid())
        .type(APPROVAL.name())
        .name("Approval Snow")
        .properties(ImmutableMap.<String, Object>builder()
                        .put("approvalStateParams", approvalStateParams)
                        .put("approvalStateType", "SERVICENOW")
                        .put("timeoutMillis", 1800000)
                        .build())
        .build();
  }

  @NotNull
  private HashMap<String, Object> getApprovalParams(Criteria approval, Criteria rejection) {
    HashMap<String, Object> serviceNowApprovalParams = new HashMap<>();
    serviceNowApprovalParams.put("approval", approval);
    serviceNowApprovalParams.put("issueNumber", "${snowIssue.issueNumber}");
    serviceNowApprovalParams.put("rejection", rejection);
    serviceNowApprovalParams.put("ticketType", "CHANGE_REQUEST");
    serviceNowApprovalParams.put("snowConnectorId", snowSetting.getUuid());

    HashMap<String, Object> approvalStateParams = new HashMap<>();
    approvalStateParams.put("serviceNowApprovalParams", serviceNowApprovalParams);
    return approvalStateParams;
  }

  private GraphNode getSnowUpdateNode(Map<String, String> updateValues) {
    ServiceNowCreateUpdateParams params = new ServiceNowCreateUpdateParams();
    params.setAction(ServiceNowAction.UPDATE);
    params.setSnowConnectorId(snowSetting.getUuid());
    params.setTicketType("CHANGE_REQUEST");
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

  private GraphNode getSnowCreateNode() {
    ServiceNowCreateUpdateParams params = new ServiceNowCreateUpdateParams();
    params.setAction(ServiceNowAction.CREATE);
    params.setSnowConnectorId(snowSetting.getUuid());
    params.setTicketType("CHANGE_REQUEST");
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

  private void workflowExecuteAndAssert(Workflow workflow, ExecutionStatus status) {
    ExecutionArgs executionArgs = saveWorkflowAndGetExecutionArgs(workflow);

    WorkflowExecution workflowExecution =
        WorkflowRestUtils.startWorkflow(bearerToken, environment.getAppId(), environment.getUuid(), executionArgs);

    logger.info("Workflow Execution started");

    Awaitility.await().atMost(600, TimeUnit.SECONDS).pollInterval(20, TimeUnit.SECONDS).until(() -> {
      ExecutionStatus executionStatus =
          workflowExecutionService.getWorkflowExecution(environment.getAppId(), workflowExecution.getUuid())
              .getStatus();
      logger.info("Current workflow execution status: {}", executionStatus.name());
      return executionStatus == status;
    });

    logger.info("Workflow Execution completed successfully");
    WorkflowExecution completedExecution =
        workflowExecutionService.getExecutionDetails(environment.getAppId(), workflowExecution.getUuid(), false);

    assertThat(completedExecution).isNotNull();
  }

  @NotNull
  private ExecutionArgs saveWorkflowAndGetExecutionArgs(Workflow workflow) {
    Workflow savedWorkflow =
        WorkflowRestUtils.createWorkflow(bearerToken, environment.getAccountId(), environment.getAppId(), workflow);
    logger.info("Workflow created successfully");
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
