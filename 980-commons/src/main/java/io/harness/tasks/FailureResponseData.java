package io.harness.tasks;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class FailureResponseData implements ErrorResponseData {
  String message;
  // TODO : Add more fields here for better handling
}
