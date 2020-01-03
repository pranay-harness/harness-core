package io.harness.functional.trigger;

import static io.harness.rule.OwnerRule.HARSH;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.trigger.Action.ActionType.WORKFLOW;

import io.harness.beans.WorkflowType;
import io.harness.category.element.FunctionalTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.trigger.DeploymentTrigger;
import software.wings.beans.trigger.ScheduledCondition;
import software.wings.beans.trigger.TriggerArgs;
import software.wings.beans.trigger.WorkflowAction;

public class DepTriggerScheduledFunctionalTest extends AbstractTriggerFunctionalTestHelper {
  @Test
  @Owner(developers = HARSH)
  @Category(FunctionalTests.class)
  public void shouldCRUDAndExecuteScheduleTriggerForWorkflow() {
    String name = "Scheduled Trigger" + System.currentTimeMillis();
    DeploymentTrigger trigger =
        DeploymentTrigger.builder()
            .action(WorkflowAction.builder()
                        .triggerArgs(TriggerArgs.builder()
                                         .variables(buildWorkflow.getOrchestrationWorkflow().getUserVariables())
                                         .build())
                        .workflowId(buildWorkflow.getUuid())
                        .build())
            .name(name)
            .appId(application.getAppId())
            .condition(ScheduledCondition.builder().cronExpression("* * * * ?").build())
            .build();

    DeploymentTrigger savedTrigger = saveAndGetTrigger(trigger);

    WorkflowAction workflowAction = (WorkflowAction) savedTrigger.getAction();
    ScheduledCondition condition = (ScheduledCondition) savedTrigger.getCondition();
    assertThat(savedTrigger).isNotNull();
    assertThat(savedTrigger.getUuid()).isNotEmpty();
    assertThat(workflowAction.getWorkflowId()).isEqualTo(buildWorkflow.getUuid());
    assertThat(condition.getCronExpression()).isEqualTo("* * * * ?");
    assertThat(workflowAction.getActionType()).isEqualTo(WORKFLOW);

    savedTrigger = getDeploymentTrigger(savedTrigger.getUuid(), application.getUuid());
    workflowAction = (WorkflowAction) savedTrigger.getAction();
    assertThat(savedTrigger).isNotNull();
    assertThat(savedTrigger.getUuid()).isNotEmpty();
    assertThat(workflowAction.getWorkflowId()).isEqualTo(buildWorkflow.getUuid());
    assertThat(workflowAction.getActionType()).isEqualTo(WORKFLOW);

    savedTrigger.setDescription("Updated description");
    ScheduledCondition updatedCondition = ScheduledCondition.builder().cronExpression("0/2 * ? * * *").build();
    savedTrigger.setCondition(updatedCondition);
    DeploymentTrigger updatedTrigger = saveAndGetTrigger(savedTrigger);

    assertThat(updatedTrigger).isNotNull();
    assertThat(updatedTrigger.getUuid()).isEqualTo(savedTrigger.getUuid());
    assertThat(updatedTrigger.getDescription()).isEqualTo("Updated description");
    assertThat(((ScheduledCondition) updatedTrigger.getCondition()).getCronExpression()).isEqualTo("0/2 * ? * * *");

    // execute trigger by calling api
    deploymentTriggerService.triggerScheduledExecutionAsync(trigger);

    boolean status = workflowExecutionService.workflowExecutionsRunning(
        WorkflowType.ORCHESTRATION, application.getAppId(), buildWorkflow.getUuid());

    // Execution should be running
    assertThat(status).isTrue();

    // Delete the trigger
    deleteTrigger(savedTrigger.getUuid(), application.getUuid());

    // Make sure that it is deleted
    DeploymentTrigger deploymentTrigger = getDeploymentTrigger(savedTrigger.getUuid(), application.getUuid());

    assertThat(deploymentTrigger).isNull();
  }
}
