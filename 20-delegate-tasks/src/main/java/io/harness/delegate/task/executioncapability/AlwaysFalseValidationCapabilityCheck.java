package io.harness.delegate.task.executioncapability;

import com.google.inject.Singleton;

import io.harness.delegate.beans.executioncapability.AlwaysFalseValidationCapability;
import io.harness.delegate.beans.executioncapability.CapabilityResponse;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;

@Singleton

public class AlwaysFalseValidationCapabilityCheck implements CapabilityCheck {
  @Override
  public CapabilityResponse performCapabilityCheck(ExecutionCapability delegateCapability) {
    AlwaysFalseValidationCapability ignoreValidationCapability = (AlwaysFalseValidationCapability) delegateCapability;

    return CapabilityResponse.builder().delegateCapability(ignoreValidationCapability).validated(false).build();
  }
}
