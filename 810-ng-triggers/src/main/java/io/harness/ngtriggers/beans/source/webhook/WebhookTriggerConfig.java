package io.harness.ngtriggers.beans.source.webhook;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXTERNAL_PROPERTY;

import io.harness.ngtriggers.beans.source.NGTriggerSpec;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonTypeName("Webhook")
public class WebhookTriggerConfig implements NGTriggerSpec {
  String type;

  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = EXTERNAL_PROPERTY, property = "type", visible = true)
  WebhookTriggerSpec spec;

  @Builder
  public WebhookTriggerConfig(String type, WebhookTriggerSpec spec) {
    this.type = type;
    this.spec = spec;
  }
}