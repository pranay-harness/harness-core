package io.harness.pms.sdk.core.adviser;

import io.harness.pms.contracts.execution.failure.FailureType;

import java.util.Set;

public interface WithFailureTypes {
  Set<FailureType> getApplicableFailureTypes();
}
