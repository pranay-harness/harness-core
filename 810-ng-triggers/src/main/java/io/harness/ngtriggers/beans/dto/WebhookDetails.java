package io.harness.ngtriggers.beans.dto;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WebhookDetails {
  String webhookSecret;
  String webhookSourceRepo;
}
