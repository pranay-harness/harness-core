package io.harness.ngtriggers.beans.source.webhook.github;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXTERNAL_PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ngtriggers.beans.source.webhook.WebhookTriggerSpec_1;
import io.harness.ngtriggers.beans.source.webhook.github.event.GithubEventSpec;
import io.harness.ngtriggers.beans.source.webhook.github.event.GithubTriggerEvent;

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
public class GithubSpec implements WebhookTriggerSpec_1 {
  GithubTriggerEvent type;

  @JsonTypeInfo(use = NAME, property = "type", include = EXTERNAL_PROPERTY, visible = true) GithubEventSpec spec;

  @Builder
  public GithubSpec(GithubTriggerEvent type, GithubEventSpec spec) {
    this.type = type;
    this.spec = spec;
  }
}
