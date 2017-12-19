package software.wings.beans.trigger;

import static software.wings.beans.trigger.TriggerConditionType.*;

import lombok.Builder;
import lombok.Data;
import software.wings.beans.WebHookToken;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by sgurubelli on 10/25/17.
 */
@Data
@Builder
public class WebHookTriggerCondition extends TriggerCondition {
  private WebHookToken webHookToken;
  private String artifactStreamId;
  private Map<String, String> parameters = new HashMap<>();
  private WebhookType webhookType;
  private WebhookEventType webhookEventType;

  public WebHookTriggerCondition() {
    super(WEBHOOK);
  }

  public WebHookTriggerCondition(WebHookToken webHookToken, String artifactStreamId, Map<String, String> parameters,
      WebhookType webhookType, WebhookEventType webhookEventType) {
    this();
    this.webHookToken = webHookToken;
    this.artifactStreamId = artifactStreamId;
    this.parameters = parameters;
    this.webhookType = webhookType;
    this.webhookEventType = webhookEventType;
  }
}
