package io.harness.ngtriggers.beans.dto.eventmapping;

import io.harness.ngtriggers.beans.response.TriggerEventResponse;

import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WebhookEventProcessingResult {
  boolean mappedToTriggers;
  List<TriggerEventResponse> responses;
}
