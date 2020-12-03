package io.harness.delegate.task.executioncapability;

import io.harness.delegate.beans.executioncapability.AlwaysFalseValidationCapability;
import io.harness.delegate.beans.executioncapability.CapabilityResponse;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.expression.ExpressionEvaluator;

import com.google.inject.Singleton;

@Singleton

public class AlwaysFalseValidationCapabilityCheck implements CapabilityCheck {
  @Override
  public CapabilityResponse performCapabilityCheck(ExecutionCapability delegateCapability) {
    AlwaysFalseValidationCapability ignoreValidationCapability = (AlwaysFalseValidationCapability) delegateCapability;

    return CapabilityResponse.builder().delegateCapability(ignoreValidationCapability).validated(false).build();
  }
}
