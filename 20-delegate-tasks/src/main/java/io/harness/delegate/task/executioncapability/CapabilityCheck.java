package io.harness.delegate.task.executioncapability;

import io.harness.delegate.beans.executioncapability.CapabilityCheckResponse;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;

public interface CapabilityCheck {
  CapabilityCheckResponse performCapabilityCheck(ExecutionCapability delegateCapability);
}
