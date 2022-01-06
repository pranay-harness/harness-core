/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.features.utils;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.ANKIT;

import static software.wings.beans.CanaryOrchestrationWorkflow.CanaryOrchestrationWorkflowBuilder.aCanaryOrchestrationWorkflow;
import static software.wings.beans.PhaseStep.PhaseStepBuilder.aPhaseStep;
import static software.wings.beans.PhaseStepType.POST_DEPLOYMENT;
import static software.wings.beans.PhaseStepType.PRE_DEPLOYMENT;
import static software.wings.beans.Workflow.WorkflowBuilder.aWorkflow;
import static software.wings.sm.StateType.JIRA_CREATE_UPDATE;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.beans.WorkflowType;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.beans.GraphNode;
import software.wings.beans.Workflow;

import com.google.common.collect.ImmutableMap;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class WorkflowUtilsTest extends WingsBaseTest {
  @Test
  @Owner(developers = ANKIT)
  @Category(UnitTests.class)
  public void testGetWorkflowUsages() {
    Workflow jiraWorkflow =
        aWorkflow()
            .uuid("some-wf-uuid")
            .name("Create Jira")
            .envId("some-env-id")
            .workflowType(WorkflowType.ORCHESTRATION)
            .orchestrationWorkflow(
                aCanaryOrchestrationWorkflow()
                    .withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT).build())
                    .withPostDeploymentSteps(aPhaseStep(POST_DEPLOYMENT).addStep(createJiraNode()).build())
                    .build())
            .build();

    List<Workflow> workflowList = Collections.singletonList(jiraWorkflow);
    assertThat(WorkflowUtils.getMatchingWorkflows(workflowList, WorkflowUtils.JIRA_USAGE_PREDICATE)).isNotEmpty();

    Workflow noJiraWorkflow =
        aWorkflow()
            .uuid("some-wf-uuid")
            .name("Create Jira")
            .envId("some-env-id")
            .workflowType(WorkflowType.ORCHESTRATION)
            .orchestrationWorkflow(
                aCanaryOrchestrationWorkflow().withPreDeploymentSteps(aPhaseStep(PRE_DEPLOYMENT).build()).build())
            .build();

    workflowList = Collections.singletonList(noJiraWorkflow);
    assertThat(WorkflowUtils.getMatchingWorkflows(workflowList, WorkflowUtils.JIRA_USAGE_PREDICATE)).isEmpty();
  }

  private GraphNode createJiraNode() {
    return GraphNode.builder()
        .id(generateUuid())
        .type(JIRA_CREATE_UPDATE.name())
        .name("Create Jira")
        .properties(ImmutableMap.<String, Object>builder()
                        .put("description", "test123")
                        .put("issueType", "Story")
                        .put("jiraAction", "CREATE_TICKET")
                        .put("jiraConnectorId", "some-jira-setting-uuid")
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
