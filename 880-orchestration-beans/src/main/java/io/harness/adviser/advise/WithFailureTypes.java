package io.harness.adviser.advise;

import io.harness.pms.execution.failure.FailureType;

import java.util.Set;

public interface WithFailureTypes {
  Set<FailureType> getApplicableFailureTypes();
}
