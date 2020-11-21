package io.harness.timeout;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import lombok.Data;

@OwnedBy(CDC)
@Data
public class TimeoutDetails {
  TimeoutInstance timeoutInstance;
  long expiredAt;

  public TimeoutDetails(TimeoutInstance timeoutInstance) {
    this.timeoutInstance = timeoutInstance;
    this.expiredAt = System.currentTimeMillis();
  }
}
