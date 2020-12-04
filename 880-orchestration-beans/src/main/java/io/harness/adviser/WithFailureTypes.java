package io.harness.adviser;

import io.harness.pms.execution.failure.FailureType;

import java.util.Set;

public interface WithFailureTypes {
  Set<FailureType> getApplicableFailureTypes();
}
