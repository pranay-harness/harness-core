package software.wings.beans.trigger;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;
import software.wings.beans.WebHookToken;

import javax.validation.constraints.NotNull;

@JsonTypeName("WEBHOOK")
@Value
@Builder
public class WebhookCondition implements Condition {
  private WebHookToken webHookToken;
  @NotNull private Type type = Type.WEBHOOK;
  private PayloadSource payloadSource;
}
