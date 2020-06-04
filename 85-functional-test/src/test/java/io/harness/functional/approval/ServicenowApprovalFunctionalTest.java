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
import io.harness.generator.EnvironmentGenerator;
import io.harness.generator.OwnerManager;
import io.harness.generator.Randomizer;
import io.harness.generator.SettingGenerator;
import io.harness.generator.SettingGenerator.Settings;
import io.harness.rule.Owner;
import io.harness.testframework.restutils.WorkflowRestUtils;
import org.awaitility.Awaitility;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.Environment;
import software.wings.beans.ExecutionArgs;
import software.wings.beans.ExecutionCredential;
import software.wings.beans.GraphNode;
import software.wings.beans.SSHExecutionCredential;
import software.wings.beans.SettingAttribute;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.servicenow.ServiceNowCreateUpdateParams;
import software.wings.beans.servicenow.ServiceNowFields;
import software.wings.delegatetasks.servicenow.ServiceNowAction;
import software.wings.service.impl.servicenow.ServiceNowDelegateServiceImpl;
import software.wings.service.intfc.WorkflowExecutionService;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@OwnedBy(CDC)
public class ServicenowApprovalFunctionalTest extends AbstractFunctionalTest {
  @Inject private OwnerManager ownerManager;
  @Inject private SettingGenerator settingGenerator;
  @Inject private EnvironmentGenerator environmentGenerator;
  @Inject private WorkflowExecutionService workflowExecutionService;
  @Inject private ServiceNowDelegateServiceImpl serviceNowDelegateService;

  private Environment environment;
  private OwnerManager.Owners owners;
  private SettingAttribute snowSetting;
  private final Randomizer.Seed seed = new Randomizer.Seed(0);

  @Before
  public void setUp() {
    owners = ownerManager.create();
    environment = environmentGenerator.ensurePredefined(seed, owners, GENERIC_TEST);
    assertThat(environment).isNotNull();
    snowSetting = settingGenerator.ensurePredefined(seed, owners, Settings.SERVICENOW_CONNECTOR);
    assertThat(snowSetting).isNotNull();
  }

  @Test
  @Owner(developers = PRABU)
  @Category(FunctionalTests.class)
  public void ExecuteServiceNowApprovalForStateField() {
    Workflow snowApprovalWorkflow =
        aWorkflow()
            .name("ServiceNow Approval Functional Test" + System.currentTimeMillis())
            .envId(environment.getUuid())
            .workflowType(WorkflowType.ORCHESTRATION)
            .orchestrationWorkflow(
                aCanaryOrchestrationWorkflow()
                    .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT).addStep(getSnowCreateNode()).build())
                    .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT)
                                                 .addStep(getSnowApprovalNode("state", "Authorize", "Canceled"))
                                                 .addStep(getSnowUpdateNode("state", "Assess"))
                                                 .withStepsInParallel(true)
                                                 .build())
                    .build())
            .build();
    workflowExecuteAndAssert(snowApprovalWorkflow);
  }

  private GraphNode getSnowApprovalNode(String field, String approvalValue, String rejectionValue) {
    HashMap<String, Object> serviceNowApprovalParams = new HashMap<>();
    serviceNowApprovalParams.put("approvalField", field);
    serviceNowApprovalParams.put("approvalOperator", "equalsTo");
    serviceNowApprovalParams.put("approvalValue", approvalValue);
    serviceNowApprovalParams.put("issueNumber", "${snowIssue.issueNumber}");
    serviceNowApprovalParams.put("rejectionField", field);
    serviceNowApprovalParams.put("rejectionValue", rejectionValue);
    serviceNowApprovalParams.put("rejectionOperator", "equalsTo");
    serviceNowApprovalParams.put("ticketType", "CHANGE_REQUEST");
    serviceNowApprovalParams.put("snowConnectorId", snowSetting.getUuid());

    HashMap<String, Object> approvalStateParams = new HashMap<>();
    approvalStateParams.put("serviceNowApprovalParams", serviceNowApprovalParams);

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

  private GraphNode getSnowUpdateNode(String field, String value) {
    ServiceNowCreateUpdateParams params = new ServiceNowCreateUpdateParams();
    params.setAction(ServiceNowAction.UPDATE);
    params.setSnowConnectorId(snowSetting.getUuid());
    params.setTicketType("CHANGE_REQUEST");
    params.setIssueNumber("${snowIssue.issueNumber}");
    Map<ServiceNowFields, String> fields = new HashMap<>();
    Map<String, String> additionalFields = new HashMap<>();
    try {
      fields.put(ServiceNowFields.valueOf(field), value);
      fields.put(WORK_NOTES, "Started progress");
    } catch (IllegalArgumentException e) {
      additionalFields.put(field, value);
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
                        .put("sweepingOutputScope", "WORKFLOW")
                        .build())
        .build();
  }

  private void workflowExecuteAndAssert(Workflow workflow) {
    Workflow savedWorkflow =
        WorkflowRestUtils.createWorkflow(bearerToken, environment.getAccountId(), environment.getAppId(), workflow);
    assertThat(savedWorkflow).isNotNull();

    ExecutionArgs executionArgs = new ExecutionArgs();
    executionArgs.setWorkflowType(savedWorkflow.getWorkflowType());
    executionArgs.setExecutionCredential(SSHExecutionCredential.Builder.aSSHExecutionCredential()
                                             .withExecutionType(ExecutionCredential.ExecutionType.SSH)
                                             .build());
    executionArgs.setOrchestrationId(savedWorkflow.getUuid());

    WorkflowExecution workflowExecution =
        WorkflowRestUtils.startWorkflow(bearerToken, environment.getAppId(), environment.getUuid(), executionArgs);

    Awaitility.await()
        .atMost(360, TimeUnit.SECONDS)
        .pollInterval(60, TimeUnit.SECONDS)
        .until(()
                   -> workflowExecutionService.getWorkflowExecution(environment.getAppId(), workflowExecution.getUuid())
                          .getStatus()
                == ExecutionStatus.SUCCESS);

    WorkflowExecution completedExecution =
        workflowExecutionService.getExecutionDetails(environment.getAppId(), workflowExecution.getUuid(), false);

    assertThat(completedExecution).isNotNull();
  }
}
