package io.harness.ngtriggers.beans.source.webhook.custom;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ngtriggers.beans.source.webhook.WebhookCondition;
import io.harness.ngtriggers.beans.source.webhook.WebhookTriggerSpec_1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@OwnedBy(PIPELINE)
public class CustomTriggerSpec implements WebhookTriggerSpec_1 {
  List<WebhookCondition> payloadConditions;
  List<WebhookCondition> headerConditions;
  String jexlCondition;
}
