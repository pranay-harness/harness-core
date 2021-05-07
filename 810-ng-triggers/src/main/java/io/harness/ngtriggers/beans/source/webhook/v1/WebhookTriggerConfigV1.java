package io.harness.ngtriggers.beans.source.webhook.v1;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXTERNAL_PROPERTY;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ngtriggers.beans.source.NGTriggerSpecV1;
import io.harness.ngtriggers.beans.source.WebhookTriggerType;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@OwnedBy(PIPELINE)
public class WebhookTriggerConfigV1 implements NGTriggerSpecV1 {
  WebhookTriggerType type;

  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = EXTERNAL_PROPERTY, property = "type", visible = true)
  WebhookTriggerSpecV1 spec;

  @Builder
  public WebhookTriggerConfigV1(WebhookTriggerType type, WebhookTriggerSpecV1 spec) {
    this.type = type;
    this.spec = spec;
  }
}