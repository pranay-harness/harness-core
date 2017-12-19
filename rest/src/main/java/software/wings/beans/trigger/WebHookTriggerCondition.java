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

  public WebHookTriggerCondition() {
    super(WEBHOOK);
  }

  public WebHookTriggerCondition(WebHookToken webHookToken, String artifactStreamId, Map<String, String> parameters) {
    this();
    this.webHookToken = webHookToken;
    this.artifactStreamId = artifactStreamId;
    this.parameters = parameters;
  }
}
