package software.wings.service.impl.trigger;

import com.google.inject.Inject;

import software.wings.beans.WebHookToken;
import software.wings.beans.trigger.Condition.Type;
import software.wings.beans.trigger.DeploymentTrigger;
import software.wings.beans.trigger.WebhookCondition;
import software.wings.utils.CryptoUtils;

public class WebhookConditionTriggerProcessor implements TriggerProcessor {
  @Inject private transient DeploymentTriggerServiceHelper triggerServiceHelper;

  @Override
  public void validateTriggerConditionSetup(DeploymentTrigger deploymentTrigger, DeploymentTrigger existingTrigger) {
    updateWebhookToken(deploymentTrigger, existingTrigger);
  }

  @Override
  public void validateTriggerActionSetup(DeploymentTrigger deploymentTrigger, DeploymentTrigger existingTrigger) {
    triggerServiceHelper.validateTriggerAction(deploymentTrigger);
  }

  @Override
  public void transformTriggerConditionRead(DeploymentTrigger deploymentTrigger) {
    // No need to update anything for webhook trigger
  }

  @Override
  public void transformTriggerActionRead(DeploymentTrigger deploymentTrigger) {
    triggerServiceHelper.reBuildTriggerActionWithNames(deploymentTrigger);
  }

  @Override
  public void executeTriggerOnEvent(String appId, TriggerExecutionParams triggerExecutionParams) {}

  private void updateWebhookToken(DeploymentTrigger deploymentTrigger, DeploymentTrigger existingTrigger) {
    WebhookCondition webhookCondition = (WebhookCondition) deploymentTrigger.getCondition();
    WebHookToken webHookToken = generateWebhookToken(getExistingWebhookToken(existingTrigger));
    deploymentTrigger.setCondition(WebhookCondition.builder()
                                       .webHookToken(webHookToken)
                                       .payloadSource(webhookCondition.getPayloadSource())
                                       .build());
  }

  private WebHookToken generateWebhookToken(WebHookToken existingToken) {
    WebHookToken webHookToken;
    if (existingToken == null || existingToken.getWebHookToken() == null) {
      webHookToken = WebHookToken.builder().webHookToken(CryptoUtils.secureRandAlphaNumString(40)).build();
    } else {
      webHookToken = existingToken;
    }

    return webHookToken;
  }

  private WebHookToken getExistingWebhookToken(DeploymentTrigger existingTrigger) {
    WebHookToken existingWebhookToken = null;
    if (existingTrigger != null && existingTrigger.getType().equals(Type.WEBHOOK)) {
      WebhookCondition existingTriggerCondition = (WebhookCondition) existingTrigger.getCondition();
      existingWebhookToken = existingTriggerCondition.getWebHookToken();
    }
    return existingWebhookToken;
  }
}
