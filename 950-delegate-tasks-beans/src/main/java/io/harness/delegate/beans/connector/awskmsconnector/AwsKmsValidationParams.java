package io.harness.delegate.beans.connector.awskmsconnector;

import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.delegate.beans.connector.ConnectorValidationParams;
import io.harness.delegate.beans.connector.awsconnector.AwsConnectorDTO;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.task.mixin.HttpConnectionExecutionCapabilityGenerator;
import io.harness.expression.ExpressionEvaluator;
import lombok.Builder;
import lombok.Value;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Value
@Builder
public class AwsKmsValidationParams implements ConnectorValidationParams, ExecutionCapabilityDemander {
  AwsConnectorDTO awsKmsConnectorDTO;
  String connectorName;

  @Override
  public ConnectorType getConnectorType() {
    return ConnectorType.AWS_KMS;
  }

  //Shashank: Review this logic
  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(ExpressionEvaluator maskingEvaluator) {
    List<ExecutionCapability> executionCapabilities = new ArrayList<>(
            Arrays.asList(HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapabilityForKms(
                    null, maskingEvaluator)));
    return executionCapabilities;
  }
}
