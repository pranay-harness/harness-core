package io.harness.ngtriggers.beans.dto;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LastTriggerExecutionDetails {
  Long lastExecutionTime;
  boolean lastExecutionSuccessful;
  String lastExecutionStatus;
  String planExecutionId;
  String message;
}
