package io.harness.delegate.task.executioncapability;

import com.google.inject.Singleton;

import io.harness.delegate.beans.executioncapability.CapabilityResponse;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.IgnoreValidationCapability;

@Singleton

public class IgnoreValidationCapabilityCheck implements CapabilityCheck {
  @Override
  public CapabilityResponse performCapabilityCheck(ExecutionCapability delegateCapability) {
    IgnoreValidationCapability ignoreValidationCapability = (IgnoreValidationCapability) delegateCapability;

    return CapabilityResponse.builder().delegateCapability(ignoreValidationCapability).validated(true).build();
  }
}
