package io.harness.cvng.beans;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class LogRecordDTO {
  String accountId;
  @Deprecated String cvConfigId;
  String verificationTaskId;
  String host;
  long timestamp;
  String log;
}
