package io.harness.functional.approval;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.generator.EnvironmentGenerator.Environments.GENERIC_TEST;
import static io.harness.generator.SettingGenerator.Settings.HARNESS_JIRA;
import static io.harness.rule.OwnerRule.ROHIT;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.sm.StateType.APPROVAL;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import io.harness.category.element.FunctionalTests;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.functional.WorkflowUtils;
import io.harness.generator.ApplicationGenerator;
import io.harness.generator.ApplicationGenerator.Applications;
import io.harness.generator.EnvironmentGenerator;
import io.harness.generator.OwnerManager;
import io.harness.generator.OwnerManager.Owners;
import io.harness.generator.Randomizer.Seed;
import io.harness.generator.SettingGenerator;
import io.harness.rule.Owner;
import io.harness.testframework.restutils.WorkflowRestUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.Application;
import software.wings.beans.CanaryOrchestrationWorkflow;
import software.wings.beans.Environment;
import software.wings.beans.GraphNode;
import software.wings.beans.PhaseStep;
import software.wings.beans.SettingAttribute;
import software.wings.beans.Workflow;
import software.wings.service.intfc.WorkflowService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Slf4j
public class JiraApprovalCrudTest extends AbstractFunctionalTest {
  @Inject private ApplicationGenerator applicationGenerator;
  @Inject private EnvironmentGenerator environmentGenerator;
  @Inject private OwnerManager ownerManager;
  @Inject private SettingGenerator settingGenerator;
  @Inject private WorkflowUtils workflowUtils;
  @Inject private WorkflowService workflowService;

  private final Seed seed = new Seed(0);
  private Application application;
  private Owners owners;

  @Before
  public void setUp() {
    owners = ownerManager.create();
    application = applicationGenerator.ensurePredefined(seed, owners, Applications.GENERIC_TEST);
    assertThat(application).isNotNull();
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(FunctionalTests.class)
  public void shouldCreateReadUpdateApprovalStepInWorkflow() {
    Environment environment = environmentGenerator.ensurePredefined(seed, owners, GENERIC_TEST);
    assertThat(environment).isNotNull();
    logger.info("Creating the workflow");
    Workflow uiWorkflow = workflowUtils.buildCanaryWorkflowPostDeploymentStep(
        "JIRA APPROVAL" + System.currentTimeMillis(), environment.getUuid(), getJiraApprovalNode());

    Workflow savedWorkflow =
        WorkflowRestUtils.createWorkflow(bearerToken, application.getAccountId(), application.getUuid(), uiWorkflow);
    assertThat(savedWorkflow).isNotNull();

    logger.info("Checking for Phase Step Addition");
    String phaseName = ((CanaryOrchestrationWorkflow) savedWorkflow.getOrchestrationWorkflow())
                           .getPostDeploymentSteps()
                           .getSteps()
                           .get(0)
                           .getName();
    assertThat(phaseName).isEqualToIgnoringCase("Approval JIRA");

    // Update Workflow's Approve Stage with new graph node
    logger.info("Updating the graph node");

    String updatedTicketId = "2345";
    PhaseStep phaseStep = new PhaseStep();
    phaseStep.setSteps(getJiraUpdatedApprovalNode(updatedTicketId));
    PhaseStep phaseStepUpdated =
        workflowService.updatePostDeployment(application.getUuid(), savedWorkflow.getUuid(), phaseStep);

    // Read the updated Value
    logger.info("Asserting the issueId Post Update");
    HashMap<String, Object> approvalStateParams =
        (HashMap<String, Object>) phaseStepUpdated.getSteps().get(0).getProperties().get("approvalStateParams");
    HashMap<String, Object> jiraApprovalParams =
        (HashMap<String, Object>) approvalStateParams.get("jiraApprovalParams");
    String ticketId = (String) jiraApprovalParams.get("issueId");
    assertThat(ticketId).isEqualToIgnoringCase(updatedTicketId);

    // Delete the Workflow
    logger.info("Deleting the workflow");
    WorkflowRestUtils.deleteWorkflow(bearerToken, savedWorkflow.getUuid(), application.getAppId());
  }

  private GraphNode getJiraApprovalNode() {
    SettingAttribute jiraSetting = settingGenerator.ensurePredefined(seed, owners, HARNESS_JIRA);
    assertThat(jiraSetting).isNotNull();

    HashMap<String, Object> jiraApprovalParams = new HashMap<>();
    jiraApprovalParams.put("approvalField", "status");
    jiraApprovalParams.put("approvalOperator", "equalsTo");
    jiraApprovalParams.put("approvalValue", "Approved");
    jiraApprovalParams.put("description", "Test_Approval");
    jiraApprovalParams.put("issueId", "1234");
    jiraApprovalParams.put("issueType", "Story");
    jiraApprovalParams.put("project", "TJI");
    jiraApprovalParams.put("jiraConnectorId", jiraSetting.getUuid());
    jiraApprovalParams.put("jiraAction", "CREATE_TICKET");
    jiraApprovalParams.put("rejectionField", "status");
    jiraApprovalParams.put("rejectionValue", "Rejected");
    jiraApprovalParams.put("rejectionOperator", "equalsTo");

    HashMap<String, Object> approvalStateParams = new HashMap<>();
    approvalStateParams.put("jiraApprovalParams", jiraApprovalParams);

    return GraphNode.builder()
        .id(generateUuid())
        .type(APPROVAL.name())
        .name("Approval JIRA")
        .properties(ImmutableMap.<String, Object>builder()
                        .put("approvalStateParams", approvalStateParams)
                        .put("approvalStateType", "JIRA")
                        .put("timeoutMillis", 1800000)
                        .build())
        .build();
  }

  private List<GraphNode> getJiraUpdatedApprovalNode(String updatedTicketId) {
    List<GraphNode> graphNodesList = new ArrayList<>();

    SettingAttribute jiraSetting = settingGenerator.ensurePredefined(seed, owners, HARNESS_JIRA);
    assertThat(jiraSetting).isNotNull();

    HashMap<String, Object> jiraApprovalParams = new HashMap<>();
    jiraApprovalParams.put("approvalField", "status");
    jiraApprovalParams.put("approvalOperator", "equalsTo");
    jiraApprovalParams.put("approvalValue", "Approved");
    jiraApprovalParams.put("description", "Test_Approval");
    jiraApprovalParams.put("issueId", updatedTicketId);
    jiraApprovalParams.put("issueType", "Story");
    jiraApprovalParams.put("project", "TJI");
    jiraApprovalParams.put("jiraConnectorId", jiraSetting.getUuid());
    jiraApprovalParams.put("jiraAction", "CREATE_TICKET");
    jiraApprovalParams.put("rejectionField", "status");
    jiraApprovalParams.put("rejectionValue", "Rejected");
    jiraApprovalParams.put("rejectionOperator", "equalsTo");

    HashMap<String, Object> approvalStateParams = new HashMap<>();
    approvalStateParams.put("jiraApprovalParams", jiraApprovalParams);

    graphNodesList.add(GraphNode.builder()
                           .id(generateUuid())
                           .type(APPROVAL.name())
                           .name("Approval JIRA")
                           .properties(ImmutableMap.<String, Object>builder()
                                           .put("approvalStateParams", approvalStateParams)
                                           .put("approvalStateType", "JIRA")
                                           .put("timeoutMillis", 1800000)
                                           .build())
                           .build());

    return graphNodesList;
  }
}
