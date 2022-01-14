package io.harness.delegate.beans;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DelegateInitializationDetails {
  private String delegateId;
  private String hostname;
  private boolean initialized;
  private boolean profileError;
  private long profileExecutedAt;
}
