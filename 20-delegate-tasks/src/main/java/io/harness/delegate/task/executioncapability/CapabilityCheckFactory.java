package io.harness.delegate.task.executioncapability;
import static io.harness.delegate.beans.executioncapability.CapabilityType.ALWAYS_TRUE;
import static io.harness.delegate.beans.executioncapability.CapabilityType.HTTP;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.delegate.beans.executioncapability.CapabilityType;

@Singleton
public class CapabilityCheckFactory {
  @Inject HttpConnectionCapabilityCheck httpConnectionCapabilityCheck;
  @Inject IgnoreValidationCapabilityCheck ignoreValidationCapabilityCheck;

  public CapabilityCheck obtainCapabilityCheck(CapabilityType capabilityCheckType) {
    if (HTTP.equals(capabilityCheckType)) {
      return httpConnectionCapabilityCheck;
    }

    if (ALWAYS_TRUE.equals(capabilityCheckType)) {
      return ignoreValidationCapabilityCheck;
    }

    return null;
  }
}
