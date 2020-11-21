package io.harness.ngtriggers.beans.source.webhook;

import io.harness.ngtriggers.beans.source.NGTriggerSpec;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonTypeName("Webhook")
public class WebhookTriggerConfig implements NGTriggerSpec {
  String type;
  WebhookTriggerSpec spec;
}
