/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.functional.approval;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.generator.EnvironmentGenerator.Environments.GENERIC_TEST;
import static io.harness.rule.OwnerRule.ROHIT;

import static software.wings.sm.StateType.APPROVAL;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.FunctionalTests;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.functional.WorkflowUtils;
import io.harness.generator.EnvironmentGenerator;
import io.harness.generator.OwnerManager;
import io.harness.generator.OwnerManager.Owners;
import io.harness.generator.Randomizer.Seed;
import io.harness.generator.SettingGenerator;
import io.harness.generator.SettingGenerator.Settings;
import io.harness.rule.Owner;
import io.harness.testframework.restutils.WorkflowRestUtils;

import software.wings.beans.CanaryOrchestrationWorkflow;
import software.wings.beans.Environment;
import software.wings.beans.GraphNode;
import software.wings.beans.PhaseStep;
import software.wings.beans.SettingAttribute;
import software.wings.beans.Workflow;
import software.wings.service.intfc.WorkflowService;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Slf4j
@OwnedBy(HarnessTeam.CDC)
public class ServicenowApprovalCrudTest extends AbstractFunctionalTest {
  @Inject private OwnerManager ownerManager;
  @Inject private SettingGenerator settingGenerator;
  @Inject private WorkflowUtils workflowUtils;
  @Inject private WorkflowService workflowService;
  @Inject private EnvironmentGenerator environmentGenerator;

  private Environment environment;

  private Owners owners;
  private final Seed seed = new Seed(0);

  @Before
  public void setUp() {
    owners = ownerManager.create();
    environment = environmentGenerator.ensurePredefined(seed, owners, GENERIC_TEST);
    assertThat(environment).isNotNull();
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(FunctionalTests.class)
  @Ignore("Functional Flakiness fixing. Needs to be fixed")
  public void shouldCreateReadUpdateApprovalStepInWorkflow() {
    log.info("Creating the workflow");

    String workflowName = "SERVICE NOW APPROVAL" + System.currentTimeMillis();
    Workflow snowWorkflow =
        workflowUtils.buildCanaryWorkflowPostDeploymentStep(workflowName, environment.getUuid(), getSnowApprovalNode());

    Workflow savedWorkflow =
        WorkflowRestUtils.createWorkflow(bearerToken, environment.getAccountId(), environment.getAppId(), snowWorkflow);
    assertThat(savedWorkflow).isNotNull();

    log.info("Checking for Phase Step Addition");
    String phaseName = ((CanaryOrchestrationWorkflow) savedWorkflow.getOrchestrationWorkflow())
                           .getPostDeploymentSteps()
                           .getSteps()
                           .get(0)
                           .getName();
    assertThat(phaseName).isEqualToIgnoringCase("Approval Snow");

    // Update Workflow's Approve Stage
    log.info("Updating the graph node");

    String updatedTicketId = "INC0000022";
    PhaseStep phaseStep = new PhaseStep();
    phaseStep.setSteps(getSnowUpdatedApprovalNode(updatedTicketId));
    PhaseStep phaseStepUpdated =
        workflowService.updatePostDeployment(environment.getAppId(), savedWorkflow.getUuid(), phaseStep);

    // Read the updated Value of the issueNumber
    log.info("Asserting the issue Number Post update");
    HashMap<String, Object> approvalStateParams =
        (HashMap<String, Object>) phaseStepUpdated.getSteps().get(0).getProperties().get("approvalStateParams");
    HashMap<String, Object> serviceNowApprovalParams =
        (HashMap<String, Object>) approvalStateParams.get("serviceNowApprovalParams");
    String ticketId = (String) serviceNowApprovalParams.get("issueNumber");
    assertThat(ticketId).isEqualToIgnoringCase(updatedTicketId);

    // Delete the Workflow
    log.info("Deleting the workflow");
    WorkflowRestUtils.deleteWorkflow(bearerToken, savedWorkflow.getUuid(), environment.getAppId());
  }

  private GraphNode getSnowApprovalNode() {
    SettingAttribute snowSetting = settingGenerator.ensurePredefined(seed, owners, Settings.SERVICENOW_CONNECTOR);
    assertThat(snowSetting).isNotNull();

    HashMap<String, Object> serviceNowApprovalParams = new HashMap<>();
    serviceNowApprovalParams.put("approvalField", "status");
    serviceNowApprovalParams.put("approvalOperator", "equalsTo");
    serviceNowApprovalParams.put("approvalValue", "Resolved");
    serviceNowApprovalParams.put("issueNumber", "INC0000020");
    serviceNowApprovalParams.put("rejectionField", "status");
    serviceNowApprovalParams.put("rejectionValue", "Canceled");
    serviceNowApprovalParams.put("rejectionOperator", "equalsTo");
    serviceNowApprovalParams.put("ticketType", "INCIDENT");
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

  private List<GraphNode> getSnowUpdatedApprovalNode(String updatedTicketId) {
    List<GraphNode> graphNodesList = new ArrayList<>();

    SettingAttribute snowSetting = settingGenerator.ensurePredefined(seed, owners, Settings.SERVICENOW_CONNECTOR);
    assertThat(snowSetting).isNotNull();

    HashMap<String, Object> serviceNowApprovalParams = new HashMap<>();
    serviceNowApprovalParams.put("approvalField", "status");
    serviceNowApprovalParams.put("approvalOperator", "equalsTo");
    serviceNowApprovalParams.put("approvalValue", "Resolved");
    serviceNowApprovalParams.put("issueNumber", updatedTicketId);
    serviceNowApprovalParams.put("rejectionField", "status");
    serviceNowApprovalParams.put("rejectionValue", "Canceled");
    serviceNowApprovalParams.put("rejectionOperator", "equalsTo");
    serviceNowApprovalParams.put("ticketType", "INCIDENT");
    serviceNowApprovalParams.put("snowConnectorId", snowSetting.getUuid());

    HashMap<String, Object> approvalStateParams = new HashMap<>();
    approvalStateParams.put("serviceNowApprovalParams", serviceNowApprovalParams);

    graphNodesList.add(GraphNode.builder()
                           .id(generateUuid())
                           .type(APPROVAL.name())
                           .name("Approval Snow")
                           .properties(ImmutableMap.<String, Object>builder()
                                           .put("approvalStateParams", approvalStateParams)
                                           .put("approvalStateType", "SERVICENOW")
                                           .put("timeoutMillis", 1800000)
                                           .build())
                           .build());

    return graphNodesList;
  }
}
