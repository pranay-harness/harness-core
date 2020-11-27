package io.harness.adviser.advise;

import io.harness.exception.FailureType;

import java.util.Set;

public interface WithFailureTypes {
  Set<FailureType> getApplicableFailureTypes();
}
