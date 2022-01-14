package io.harness.ngtriggers.beans.entity.metadata.status;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ngtriggers.beans.entity.metadata.WebhookRegistrationStatus;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@OwnedBy(PIPELINE)
public class WebhookAutoRegistrationStatus {
  WebhookRegistrationStatus registrationResult;
  String detailedMessage;
}
