package io.harness.delegate.task.executioncapability;

import io.harness.capability.CapabilityParameters;
import io.harness.capability.CapabilitySubjectPermission;

public interface ProtoCapabilityCheck {
  CapabilitySubjectPermission performCapabilityCheckWithProto(CapabilityParameters parameters);
}
