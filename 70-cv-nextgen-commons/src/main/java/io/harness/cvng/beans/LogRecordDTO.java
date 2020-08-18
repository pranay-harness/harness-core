package io.harness.cvng.beans;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class LogRecordDTO {
  String accountId;
  String cvConfigId;
  String verificationTaskId;
  String host;
  long timestamp;
  String log;
}
