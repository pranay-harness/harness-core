package io.harness.delegate.beans.connector.newrelicconnector;

import io.harness.delegate.beans.connector.newrelic.NewRelicConnectorDTO;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.task.mixin.HttpConnectionExecutionCapabilityGenerator;
import io.harness.expression.ExpressionEvaluator;

import java.util.Arrays;
import java.util.List;
import lombok.experimental.UtilityClass;

@UtilityClass
public class NewRelicCapabilityHelper {
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(
      ExpressionEvaluator maskingEvaluator, NewRelicConnectorDTO newRelicConnectorDTO) {
    return Arrays.asList(HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapability(
        newRelicConnectorDTO.getUrl(), maskingEvaluator));
  }
}
