package io.harness.functional.trigger;

import static io.harness.rule.OwnerRule.HARSH;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.trigger.Action.ActionType.WORKFLOW;

import com.google.common.collect.ImmutableMap;

import io.harness.beans.WorkflowType;
import io.harness.category.element.FunctionalTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.Artifact.ArtifactMetadataKeys;
import software.wings.beans.trigger.ArtifactCondition;
import software.wings.beans.trigger.DeploymentTrigger;
import software.wings.beans.trigger.TriggerArgs;
import software.wings.beans.trigger.WorkflowAction;

public class DepTriggerNewArtifactFunctionalTest extends AbstractTriggerFunctionalTestHelper {
  @Test
  @Owner(developers = HARSH)
  @Category(FunctionalTests.class)
  public void shouldCRUDArtifactTriggerForWorkflow() {
    String name = "New Artifact Trigger" + System.currentTimeMillis();
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
            .condition(ArtifactCondition.builder()
                           .artifactServerId("SETTING_ID")
                           .artifactStreamId(artifactStream.getUuid())
                           .build())
            .build();

    DeploymentTrigger savedTrigger = saveAndGetTrigger(trigger);

    WorkflowAction workflowAction = (WorkflowAction) savedTrigger.getAction();
    assertThat(savedTrigger).isNotNull();
    assertThat(savedTrigger.getUuid()).isNotEmpty();
    assertThat(workflowAction.getWorkflowId()).isEqualTo(buildWorkflow.getUuid());
    assertThat(workflowAction.getActionType()).isEqualTo(WORKFLOW);

    savedTrigger = getDeploymentTrigger(savedTrigger.getUuid(), application.getUuid());
    workflowAction = (WorkflowAction) savedTrigger.getAction();
    assertThat(savedTrigger).isNotNull();
    assertThat(savedTrigger.getUuid()).isNotEmpty();
    assertThat(workflowAction.getWorkflowId()).isEqualTo(buildWorkflow.getUuid());
    assertThat(workflowAction.getActionType()).isEqualTo(WORKFLOW);

    // Update the saved trigger
    savedTrigger.setDescription("Updated description");

    DeploymentTrigger updatedTrigger = saveAndGetTrigger(savedTrigger);

    assertThat(updatedTrigger).isNotNull();
    assertThat(updatedTrigger.getUuid()).isEqualTo(savedTrigger.getUuid());
    assertThat(updatedTrigger.getDescription()).isEqualTo("Updated description");

    Artifact artifact = Artifact.Builder.anArtifact()
                            .withAppId(application.getUuid())
                            .withArtifactStreamId(artifactStream.getArtifactStreamId())
                            .withMetadata(ImmutableMap.of(ArtifactMetadataKeys.buildNo, "1.2"))
                            .withDisplayName("Some artifact")
                            .build();

    // execute trigger by calling api
    deploymentTriggerService.triggerExecutionPostArtifactCollectionAsync(
        application.getAccountId(), application.getAppId(), artifactStream.getUuid(), asList(artifact));

    boolean status = workflowExecutionService.workflowExecutionsRunning(
        WorkflowType.ORCHESTRATION, application.getAppId(), buildWorkflow.getUuid());

    // Execution should be running
    assertThat(status).isTrue();

    deleteTrigger(savedTrigger.getUuid(), application.getUuid());

    // Make sure that it is deleted
    DeploymentTrigger deploymentTrigger = getDeploymentTrigger(savedTrigger.getUuid(), application.getUuid());
    assertThat(deploymentTrigger).isNull();
  }
}
