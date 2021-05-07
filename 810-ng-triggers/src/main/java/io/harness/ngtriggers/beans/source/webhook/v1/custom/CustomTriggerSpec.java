package io.harness.ngtriggers.beans.source.webhook.v1.custom;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ngtriggers.beans.source.webhook.WebhookCondition;
import io.harness.ngtriggers.beans.source.webhook.v1.WebhookTriggerSpecV1;
import io.harness.ngtriggers.beans.source.webhook.v1.git.PayloadAware;

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
public class CustomTriggerSpec implements WebhookTriggerSpecV1, PayloadAware {
  List<WebhookCondition> payloadConditions;
  List<WebhookCondition> headerConditions;
  String jexlCondition;
}
