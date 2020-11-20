package io.harness.ngtriggers.beans.source.webhook;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.ngtriggers.beans.source.NGTriggerSpec;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonTypeName("Webhook")
public class WebhookTriggerConfig implements NGTriggerSpec {
  String type;
  WebhookTriggerSpec spec;
}
