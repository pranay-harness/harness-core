package io.harness.delegate.beans;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DelegateConnectionHeartbeat {
  private String delegateConnectionId;
  private String version;
  private String location;
}
