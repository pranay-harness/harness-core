package software.wings.licensing.violations.checkers;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.CanaryOrchestrationWorkflow.CanaryOrchestrationWorkflowBuilder.aCanaryOrchestrationWorkflow;
import static software.wings.beans.PhaseStep.PhaseStepBuilder.aPhaseStep;
import static software.wings.beans.PhaseStepType.POST_DEPLOYMENT;
import static software.wings.beans.PhaseStepType.PRE_DEPLOYMENT;
import static software.wings.beans.Workflow.WorkflowBuilder.aWorkflow;
import static software.wings.sm.StateType.JIRA_CREATE_UPDATE;

import com.google.common.collect.ImmutableMap;

import io.harness.beans.WorkflowType;
import io.harness.category.element.UnitTests;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;
import software.wings.beans.FeatureUsageViolation.Usage;
import software.wings.beans.GraphNode;
import software.wings.beans.Workflow;

import java.util.Collections;
import java.util.List;

public class WorkflowViolationCheckerMixinTest extends WingsBaseTest implements WorkflowViolationCheckerMixin {
  @Test
  @Category(UnitTests.class)
  public void testGetWorkflowViolationUsages() {
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
    List<Usage> usages =
        getWorkflowViolationUsages(workflowList, LiveNotificationViolationChecker.IS_JIRA_STATE_PRESENT);
    assertThat(usages).isNotEmpty();

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
    usages = getWorkflowViolationUsages(workflowList, LiveNotificationViolationChecker.IS_JIRA_STATE_PRESENT);
    assertThat(usages).isEmpty();
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
