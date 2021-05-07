package io.harness.ngtriggers.beans.source.webhook;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXTERNAL_PROPERTY;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ngtriggers.beans.source.NGTriggerSpec_1;
import io.harness.ngtriggers.beans.source.WebhookTriggerType;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@OwnedBy(PIPELINE)
public class WebhookTriggerConfig_1 implements NGTriggerSpec_1 {
  WebhookTriggerType type;

  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = EXTERNAL_PROPERTY, property = "type", visible = true)
  WebhookTriggerSpec_1 spec;

  @Builder
  public WebhookTriggerConfig_1(WebhookTriggerType type, WebhookTriggerSpec_1 spec) {
    this.type = type;
    this.spec = spec;
  }
}