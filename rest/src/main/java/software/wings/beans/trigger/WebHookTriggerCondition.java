package software.wings.beans.trigger;

import static software.wings.beans.trigger.TriggerConditionType.WEBHOOK;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.beans.WebHookToken;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by sgurubelli on 10/25/17.
 */

@JsonTypeName("WEBHOOK")
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
public class WebHookTriggerCondition extends TriggerCondition {
  private WebHookToken webHookToken;
  private String artifactStreamId;
  @Builder.Default private Map<String, String> parameters = new HashMap<>();
  private WebhookSource webhookSource;
  private List<WebhookEventType> eventTypes;
  private List<PrAction> actions;

  public WebHookTriggerCondition() {
    super(WEBHOOK);
  }

  public WebHookTriggerCondition(WebHookToken webHookToken, String artifactStreamId, Map<String, String> parameters,
      WebhookSource webhookSource, List<WebhookEventType> eventTypes, List<PrAction> actions) {
    this();
    this.webHookToken = webHookToken;
    this.artifactStreamId = artifactStreamId;
    this.parameters = parameters;
    this.webhookSource = webhookSource;
    this.eventTypes = eventTypes;
    this.actions = actions;
  }
}
