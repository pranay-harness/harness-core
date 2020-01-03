package io.harness.functional.trigger;

import static io.harness.rule.OwnerRule.HARSH;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.trigger.Action.ActionType.WORKFLOW;
import static software.wings.beans.trigger.TriggerExecution.Status.RUNNING;

import com.google.inject.Inject;

import io.harness.beans.WorkflowType;
import io.harness.category.element.FunctionalTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.EntityType;
import software.wings.beans.WebHookRequest;
import software.wings.beans.WebHookResponse;
import software.wings.beans.WebHookToken;
import software.wings.beans.trigger.CustomPayloadSource;
import software.wings.beans.trigger.DeploymentTrigger;
import software.wings.beans.trigger.TriggerArgs;
import software.wings.beans.trigger.TriggerArtifactSelectionLastCollected;
import software.wings.beans.trigger.TriggerArtifactVariable;
import software.wings.beans.trigger.WebhookCondition;
import software.wings.beans.trigger.WorkflowAction;
import software.wings.service.impl.WebHookServiceImpl;
import software.wings.utils.CryptoUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class DepTriggerWebhookFunctionalTest extends AbstractTriggerFunctionalTestHelper {
  @Inject private WebHookServiceImpl webHookService;
  final String token = CryptoUtils.secureRandAlphaNumString(40);

  @Test
  @Owner(developers = HARSH)
  @Category(FunctionalTests.class)
  public void shouldCRUDAndExecuteWebhookTriggerForWorkflow() {
    String name = "Webhook" + System.currentTimeMillis();
    DeploymentTrigger trigger =
        DeploymentTrigger.builder()
            .action(WorkflowAction.builder()
                        .triggerArgs(TriggerArgs.builder()
                                         .triggerArtifactVariables(Arrays.asList(
                                             TriggerArtifactVariable.builder()
                                                 .variableName("artifact")
                                                 .variableValue(TriggerArtifactSelectionLastCollected.builder()
                                                                    .artifactStreamId("${var1}")
                                                                    .artifactServerId("${var2}")
                                                                    .artifactFilter("${var3}")
                                                                    .build())
                                                 .entityId("${entity}")
                                                 .entityType(EntityType.SERVICE)
                                                 .build()))
                                         .variables(buildWorkflow.getOrchestrationWorkflow().getUserVariables())
                                         .build())
                        .workflowId(buildWorkflow.getUuid())
                        .build())
            .name(name)
            .appId(application.getAppId())
            .condition(WebhookCondition.builder()
                           .webHookToken(WebHookToken.builder().webHookToken(token).build())
                           .payloadSource(CustomPayloadSource.builder().build())
                           .build())
            .build();

    DeploymentTrigger savedTrigger = saveAndGetTrigger(trigger);

    WorkflowAction workflowAction = (WorkflowAction) savedTrigger.getAction();
    assertThat(savedTrigger).isNotNull();
    assertThat(savedTrigger.getUuid()).isNotEmpty();
    assertThat(workflowAction.getActionType()).isEqualTo(WORKFLOW);
    assertThat(workflowAction.getWorkflowId()).isEqualTo(buildWorkflow.getUuid());

    savedTrigger = getDeploymentTrigger(savedTrigger.getUuid(), application.getUuid());
    workflowAction = (WorkflowAction) savedTrigger.getAction();
    assertThat(savedTrigger).isNotNull();
    assertThat(workflowAction.getActionType()).isEqualTo(WORKFLOW);
    assertThat(savedTrigger.getUuid()).isNotEmpty();
    assertThat(workflowAction.getWorkflowId()).isEqualTo(buildWorkflow.getUuid());

    // Update the saved trigger
    savedTrigger.setDescription("Updated description webhook");
    DeploymentTrigger updatedTrigger = saveAndGetTrigger(savedTrigger);

    assertThat(updatedTrigger).isNotNull();
    assertThat(updatedTrigger.getUuid()).isEqualTo(savedTrigger.getUuid());
    assertThat(updatedTrigger.getDescription()).isEqualTo("Updated description webhook");

    // execute trigger by calling api

    Map<String, String> parameters = new HashMap<>();
    parameters.put("var1", artifactStream.getName());
    parameters.put("var2", "Harness Jenkins");
    parameters.put("var3", "filter");
    parameters.put("entity", service.getName());
    WebHookRequest request = WebHookRequest.builder().parameters(parameters).application(application.getUuid()).build();

    WebHookResponse response =
        (WebHookResponse) webHookService.execute(savedTrigger.getWebHookToken(), request).getEntity();
    assertThat(response).isNotNull();
    assertThat(response.getStatus()).isEqualTo(RUNNING.name());

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
