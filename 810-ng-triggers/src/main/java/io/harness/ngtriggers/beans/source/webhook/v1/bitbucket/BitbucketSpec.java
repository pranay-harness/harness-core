package io.harness.ngtriggers.beans.source.webhook.v1.bitbucket;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXTERNAL_PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ngtriggers.beans.source.webhook.v1.WebhookTriggerSpecV1;
import io.harness.ngtriggers.beans.source.webhook.v1.bitbucket.event.BitbucketEventSpec;
import io.harness.ngtriggers.beans.source.webhook.v1.bitbucket.event.BitbucketTriggerEvent;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@OwnedBy(PIPELINE)
public class BitbucketSpec implements WebhookTriggerSpecV1 {
  BitbucketTriggerEvent type;

  @JsonTypeInfo(use = NAME, property = "type", include = EXTERNAL_PROPERTY, visible = true) BitbucketEventSpec spec;

  @Builder
  public BitbucketSpec(BitbucketTriggerEvent type, BitbucketEventSpec spec) {
    this.type = type;
    this.spec = spec;
  }
}
