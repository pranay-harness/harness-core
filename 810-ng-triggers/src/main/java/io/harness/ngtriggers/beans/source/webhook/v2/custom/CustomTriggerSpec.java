package io.harness.ngtriggers.beans.source.webhook.v2.custom;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ngtriggers.beans.source.webhook.WebhookCondition;
import io.harness.ngtriggers.beans.source.webhook.v2.WebhookTriggerSpecV2;
import io.harness.ngtriggers.beans.source.webhook.v2.git.GitAware;
import io.harness.ngtriggers.beans.source.webhook.v2.git.PayloadAware;

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
public class CustomTriggerSpec implements WebhookTriggerSpecV2, PayloadAware {
  List<WebhookCondition> payloadConditions;
  List<WebhookCondition> headerConditions;
  String jexlCondition;

  @Override
  public GitAware fetchGitAware() {
    return null;
  }

  @Override
  public PayloadAware fetchPayloadAware() {
    return this;
  }

  @Override
  public List<WebhookCondition> fetchHeaderConditions() {
    return headerConditions;
  }

  @Override
  public List<WebhookCondition> fetchPayloadConditions() {
    return payloadConditions;
  }

  @Override
  public String fetchJexlCondition() {
    return jexlCondition;
  }
}
