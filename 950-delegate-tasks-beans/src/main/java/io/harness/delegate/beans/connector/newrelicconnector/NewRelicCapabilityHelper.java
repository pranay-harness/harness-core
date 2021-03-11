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
  private static final String BASE_URL = "https://insights-api.newrelic.com/v1/accounts/";

  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(
      ExpressionEvaluator maskingEvaluator, NewRelicConnectorDTO newRelicConnectorDTO) {
    return Arrays.asList(HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapability(
        BASE_URL + newRelicConnectorDTO.getNewRelicAccountId(), maskingEvaluator));
  }
}
