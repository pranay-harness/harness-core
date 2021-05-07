package io.harness.ngtriggers.beans.source.webhook.v1.awscodecommit;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXTERNAL_PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ngtriggers.beans.source.webhook.v1.WebhookTriggerSpecV1;
import io.harness.ngtriggers.beans.source.webhook.v1.awscodecommit.event.AwsCodeCommitEventSpec;
import io.harness.ngtriggers.beans.source.webhook.v1.awscodecommit.event.AwsCodeCommitTriggerEvent;

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
public class AwsCodeCommitSpec implements WebhookTriggerSpecV1 {
  AwsCodeCommitTriggerEvent type;

  @JsonTypeInfo(use = NAME, property = "type", include = EXTERNAL_PROPERTY, visible = true) AwsCodeCommitEventSpec spec;

  @Builder
  public AwsCodeCommitSpec(AwsCodeCommitTriggerEvent type, AwsCodeCommitEventSpec spec) {
    this.type = type;
    this.spec = spec;
  }
}
