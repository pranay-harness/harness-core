package io.harness.delegate.beans;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DelegateHeartbeatDetails {
  private int numberOfRegisteredDelegates;
  private int numberOfConnectedDelegates;
}
