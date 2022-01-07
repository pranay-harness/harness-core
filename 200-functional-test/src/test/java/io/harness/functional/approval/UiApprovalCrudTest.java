/*
 * Copyright 2020 Harness Inc. All rights reserved.
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

import io.harness.category.element.FunctionalTests;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.functional.WorkflowUtils;
import io.harness.generator.ApplicationGenerator;
import io.harness.generator.ApplicationGenerator.Applications;
import io.harness.generator.EnvironmentGenerator;
import io.harness.generator.OwnerManager;
import io.harness.generator.OwnerManager.Owners;
import io.harness.generator.Randomizer.Seed;
import io.harness.rule.Owner;
import io.harness.testframework.restutils.UserGroupRestUtils;
import io.harness.testframework.restutils.WorkflowRestUtils;

import software.wings.beans.Application;
import software.wings.beans.CanaryOrchestrationWorkflow;
import software.wings.beans.Environment;
import software.wings.beans.GraphNode;
import software.wings.beans.PhaseStep;
import software.wings.beans.Workflow;
import software.wings.beans.security.UserGroup;
import software.wings.service.intfc.WorkflowService;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Slf4j
public class UiApprovalCrudTest extends AbstractFunctionalTest {
  @Inject private OwnerManager ownerManager;
  @Inject private ApplicationGenerator applicationGenerator;
  @Inject private EnvironmentGenerator environmentGenerator;
  @Inject private WorkflowUtils workflowUtils;
  @Inject private WorkflowService workflowService;

  private Application application;
  private final Seed seed = new Seed(0);
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
    log.info("Creating the workflow");

    log.info("Fetching User Group Id");
    List<UserGroup> userGroupLists = UserGroupRestUtils.getUserGroups(getAccount(), bearerToken);
    String userGroupId = userGroupLists.get(0).getUuid();
    String userGroupIdNew = userGroupLists.get(1).getUuid();

    String workflowName = "Test Approval" + System.currentTimeMillis();
    Workflow uiWorkflow = workflowUtils.buildCanaryWorkflowPostDeploymentStep(
        workflowName, environment.getUuid(), getApprovalNode(userGroupId));

    Workflow savedWorkflow =
        WorkflowRestUtils.createWorkflow(bearerToken, application.getAccountId(), application.getUuid(), uiWorkflow);
    assertThat(savedWorkflow).isNotNull();

    log.info("Asserting that the Phase step of approval is added");
    String phaseName = ((CanaryOrchestrationWorkflow) savedWorkflow.getOrchestrationWorkflow())
                           .getPostDeploymentSteps()
                           .getSteps()
                           .get(0)
                           .getName();
    assertThat(phaseName).isEqualToIgnoringCase("Test Approval");

    // Update Workflow's Approve Stage
    log.info("Updating the graph node");
    PhaseStep phaseStep = new PhaseStep();
    phaseStep.setName("Post-Deployment");
    phaseStep.setSteps(getUpdatedApprovalNode(userGroupIdNew));
    PhaseStep phaseStepUpdated =
        workflowService.updatePostDeployment(application.getUuid(), savedWorkflow.getUuid(), phaseStep);

    log.info("Asserting the userGroupId of the approval Stage");
    ArrayList<String> userGroupIds =
        (ArrayList<String>) phaseStepUpdated.getSteps().get(0).getProperties().get("userGroups");
    String userGroupIdUpdated = userGroupIds.get(0);

    // Read the updated Value
    assertThat(userGroupIdUpdated).isEqualToIgnoringCase(userGroupIdNew);

    // Delete the Workflow
    log.info("Deleting the workflow");
    WorkflowRestUtils.deleteWorkflow(bearerToken, savedWorkflow.getUuid(), application.getAppId());
  }

  private GraphNode getApprovalNode(String userGroupId) {
    return GraphNode.builder()
        .id(generateUuid())
        .type(APPROVAL.name())
        .name("Test Approval")
        .properties(ImmutableMap.<String, Object>builder()
                        .put("approvalStateType", "USER_GROUP")
                        .put("timeoutMillis", 1800000)
                        .put("userGroups", Collections.singletonList(userGroupId))
                        .build())
        .build();
  }

  private List<GraphNode> getUpdatedApprovalNode(String userGroupIdNew) {
    List<GraphNode> graphNodesList = new ArrayList<>();
    graphNodesList.add(GraphNode.builder()
                           .id(generateUuid())
                           .type(APPROVAL.name())
                           .name("Test Approval Updated")
                           .properties(ImmutableMap.<String, Object>builder()
                                           .put("approvalStateType", "USER_GROUP")
                                           .put("timeoutMillis", 1800000)
                                           .put("userGroups", Collections.singletonList(userGroupIdNew))
                                           .build())
                           .build());
    return graphNodesList;
  }
}
