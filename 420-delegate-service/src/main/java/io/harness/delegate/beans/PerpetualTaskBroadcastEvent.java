package io.harness.delegate.beans;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PerpetualTaskBroadcastEvent {
  private String eventType;
  private String broadcastDelegateId;
}
