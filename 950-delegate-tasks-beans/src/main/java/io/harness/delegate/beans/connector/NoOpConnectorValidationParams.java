package io.harness.delegate.beans.connector;

import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.expression.ExpressionEvaluator;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class NoOpConnectorValidationParams implements ConnectorValidationParams {
  @Override
  public ConnectorType getConnectorType() {
    return null;
  }

  @Override
  public String getConnectorName() {
    return null;
  }

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(ExpressionEvaluator maskingEvaluator) {
    return null;
  }
}
