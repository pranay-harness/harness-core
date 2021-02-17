package io.harness.delegate.beans.connector.awsconnector;

import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.task.mixin.HttpConnectionExecutionCapabilityGenerator;
import io.harness.expression.ExpressionEvaluator;

import java.util.Arrays;
import java.util.List;
import lombok.experimental.UtilityClass;

@UtilityClass
public class AwsCapabilityHelper {
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(
      ExpressionEvaluator maskingEvaluator, AwsConnectorDTO awsConnectorDTO) {
    final String AWS_URL = "https://aws.amazon.com/";
    return Arrays.asList(
        HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapability(AWS_URL, maskingEvaluator));
  }
}
