package io.harness.cvng.beans;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.Set;
@Value
@Builder
public class HostRecordDTO {
  String accountId;
  String verificationTaskId;
  Set<String> hosts;
  Instant startTime;
  Instant endTime;
}
