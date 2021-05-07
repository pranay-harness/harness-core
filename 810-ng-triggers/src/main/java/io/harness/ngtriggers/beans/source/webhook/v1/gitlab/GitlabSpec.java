package io.harness.ngtriggers.beans.source.webhook.v1.gitlab;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXTERNAL_PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ngtriggers.beans.source.webhook.v1.WebhookTriggerSpecV1;
import io.harness.ngtriggers.beans.source.webhook.v1.gitlab.event.GitlabEventSpec;
import io.harness.ngtriggers.beans.source.webhook.v1.gitlab.event.GitlabTriggerEvent;

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
public class GitlabSpec implements WebhookTriggerSpecV1 {
  GitlabTriggerEvent type;

  @JsonTypeInfo(use = NAME, property = "type", include = EXTERNAL_PROPERTY, visible = true) GitlabEventSpec spec;

  @Builder
  public GitlabSpec(GitlabTriggerEvent type, GitlabEventSpec spec) {
    this.type = type;
    this.spec = spec;
  }
}
