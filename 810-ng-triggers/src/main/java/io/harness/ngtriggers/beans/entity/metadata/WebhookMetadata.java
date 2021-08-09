package io.harness.ngtriggers.beans.entity.metadata;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(PIPELINE)
public class WebhookMetadata {
  String type;
  GitMetadata git;
  CustomMetadata custom;
  @Builder.Default WebhookRegistrationStatus registrationStatus = WebhookRegistrationStatus.UNAVAILABLE;
}
