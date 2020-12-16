package io.harness.tasks;

import io.harness.exception.FailureType;

import java.util.EnumSet;

public interface ErrorResponseData extends ResponseData {
  String getErrorMessage();
  EnumSet<FailureType> getFailureTypes();
}
