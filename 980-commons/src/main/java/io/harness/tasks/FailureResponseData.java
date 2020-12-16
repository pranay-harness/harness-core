package io.harness.tasks;

import io.harness.exception.FailureType;

import java.util.EnumSet;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class FailureResponseData implements ErrorResponseData {
  String errorMessage;
  EnumSet<FailureType> failureTypes;
  // TODO : Add more fields here for better handling
}
