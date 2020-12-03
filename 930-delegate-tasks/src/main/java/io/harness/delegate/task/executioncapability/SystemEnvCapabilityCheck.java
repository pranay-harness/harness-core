package io.harness.delegate.task.executioncapability;

import io.harness.delegate.beans.executioncapability.CapabilityResponse;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.SystemEnvCheckerCapability;
import io.harness.expression.ExpressionEvaluator;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SystemEnvCapabilityCheck implements CapabilityCheck {
  @Override
  public CapabilityResponse performCapabilityCheck(ExecutionCapability delegateCapability) {
    SystemEnvCheckerCapability systemEnvCheckerCapability = (SystemEnvCheckerCapability) delegateCapability;
    boolean valid = systemEnvCheckerCapability.getComparate().equals(
        System.getenv().get(systemEnvCheckerCapability.getSystemPropertyName()));
    return CapabilityResponse.builder().delegateCapability(systemEnvCheckerCapability).validated(valid).build();
  }
}
