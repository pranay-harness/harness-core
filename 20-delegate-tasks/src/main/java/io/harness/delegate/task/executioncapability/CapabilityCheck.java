package io.harness.delegate.task.executioncapability;

import io.harness.delegate.beans.executioncapability.CapabilityResponse;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;

public interface CapabilityCheck { CapabilityResponse performCapabilityCheck(ExecutionCapability delegateCapability); }
