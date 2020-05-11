package software.wings.beans.trigger;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.annotations.dev.OwnedBy;
import lombok.Builder;
import lombok.Value;
import software.wings.beans.WebHookToken;

import javax.validation.constraints.NotNull;

@OwnedBy(CDC)
@JsonTypeName("WEBHOOK")
@Value
@Builder
public class WebhookCondition implements Condition {
  private WebHookToken webHookToken;
  @NotNull private Type type = Type.WEBHOOK;
  private PayloadSource payloadSource;
}
