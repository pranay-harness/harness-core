package software.wings.beans.trigger;

import lombok.Builder;
import lombok.Data;
import software.wings.beans.WebHookToken;

/**
 * Created by sgurubelli on 10/25/17.
 */
@Data
@Builder
public class WebHookTriggerCondition extends TriggerCondition {
  private WebHookToken webHookToken;
  private String artifactStreamId;

  public WebHookTriggerCondition() {
    super(TriggerConditionType.WEBHOOK);
  }

  public WebHookTriggerCondition(WebHookToken webHookToken, String artifactStreamId) {
    this();
    this.webHookToken = webHookToken;
    this.artifactStreamId = artifactStreamId;
  }
}
