package io.harness.delegate.beans;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import lombok.Data;

@Data
@OwnedBy(HarnessTeam.DEL)
public class DelegateUnregisterRequest {
  private final String delegateId;
  private final String hostName;
  private final boolean isNg;
  private final String delegateType;
  private final String ipAddress;
}
