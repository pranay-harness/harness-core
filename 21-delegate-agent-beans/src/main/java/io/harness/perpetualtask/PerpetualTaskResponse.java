package io.harness.perpetualtask;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PerpetualTaskResponse {
  int responseCode;
  String responseMessage;
  PerpetualTaskState perpetualTaskState;
}
