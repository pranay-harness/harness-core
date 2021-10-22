package io.harness.cvng.beans;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LogRecordDTO {
  String accountId;
  String verificationTaskId;
  String host;
  long timestamp;
  String log;
}
