package io.harness.tasks;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.exception.FailureType;

import java.util.EnumSet;

@TargetModule(Module._955_DELEGATE_BEANS)
public interface ErrorResponseData extends ResponseData {
  String getErrorMessage();
  EnumSet<FailureType> getFailureTypes();
}
