package io.harness.delegate.beans;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder(toBuilder = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ThirdPartyApiCallLogDetails {
  String stateExecutionId;
  String accountId;
  String delegateId;
  String delegateTaskId;
}
