package software.wings.beans.trigger;

import lombok.Builder;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

/**
 * Created by sgurubelli on 10/25/17.
 */
@Data
@Builder
public class WebHookTriggerCondition extends TriggerCondition {
  @NotEmpty private String webHookToken;
  @NotEmpty private String requestBody;
  private String artifactStreamId;

  public WebHookTriggerCondition() {
    super(TriggerConditionType.WEBHOOK);
  }

  public WebHookTriggerCondition(String webHookToken, String requestBody, String artifactStreamId) {
    this();
    this.webHookToken = webHookToken;
    this.requestBody = requestBody;
    this.artifactStreamId = artifactStreamId;
  }
}
