package io.harness.delegate.beans.executioncapability;

import io.harness.expression.ExpressionEvaluator;

import java.util.List;

public interface ExecutionCapabilityDemander {
  List<io.harness.delegate.beans.executioncapability.ExecutionCapability> fetchRequiredExecutionCapabilities(
      ExpressionEvaluator maskingEvaluator);
}
