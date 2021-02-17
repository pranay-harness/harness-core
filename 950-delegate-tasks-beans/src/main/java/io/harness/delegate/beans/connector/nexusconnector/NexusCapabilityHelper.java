package io.harness.delegate.beans.connector.nexusconnector;

import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.task.mixin.HttpConnectionExecutionCapabilityGenerator;
import io.harness.expression.ExpressionEvaluator;

import java.util.Arrays;
import java.util.List;
import lombok.experimental.UtilityClass;

@UtilityClass
public class NexusCapabilityHelper {
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(
      ExpressionEvaluator maskingEvaluator, NexusConnectorDTO nexusConnectorDTO) {
    String nexusServerUrl = nexusConnectorDTO.getNexusServerUrl();
    return Arrays.asList(HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapability(
        nexusServerUrl, maskingEvaluator));
  }
}
