package io.harness.tasks;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.exception.FailureType;

import java.util.EnumSet;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@TargetModule(Module._955_DELEGATE_BEANS)
public class FailureResponseData implements ErrorResponseData {
  String errorMessage;
  EnumSet<FailureType> failureTypes;
  // TODO : Add more fields here for better handling
}
