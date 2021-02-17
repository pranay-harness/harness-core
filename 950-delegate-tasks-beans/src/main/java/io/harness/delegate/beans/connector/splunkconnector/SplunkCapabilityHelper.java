package io.harness.delegate.beans.connector.splunkconnector;

import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.task.mixin.HttpConnectionExecutionCapabilityGenerator;
import io.harness.expression.ExpressionEvaluator;

import java.util.Arrays;
import java.util.List;
import lombok.experimental.UtilityClass;

@UtilityClass
public class SplunkCapabilityHelper {
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(
      ExpressionEvaluator maskingEvaluator, SplunkConnectorDTO splunkConnectorDTO) {
    final String splunkUrl = splunkConnectorDTO.getSplunkUrl();
    return Arrays.asList(
        HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapability(splunkUrl, maskingEvaluator));
  }
}
