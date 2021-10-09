package io.harness.delegate.beans.connector.awssecretmanager;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.delegate.beans.connector.ConnectorCapabilityBaseHelper.populateDelegateSelectorCapability;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.delegate.beans.connector.ConnectorValidationParams;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.task.mixin.HttpConnectionExecutionCapabilityGenerator;
import io.harness.expression.ExpressionEvaluator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@OwnedBy(PL)
@Value
@Builder
public class AwsSecretManagerValidationParams implements ConnectorValidationParams, ExecutionCapabilityDemander {
  AwsSecretManagerDTO awsSecretManagerDTO;
  String connectorName;

  @Override
  public ConnectorType getConnectorType() {
    return ConnectorType.AWS_SECRET_MANAGER;
  }

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(ExpressionEvaluator maskingEvaluator) {
    List<ExecutionCapability> executionCapabilities =
        new ArrayList<>(Arrays.asList(HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapability(
            awsSecretManagerDTO.getRegion(), maskingEvaluator)));
    populateDelegateSelectorCapability(executionCapabilities, awsSecretManagerDTO.getDelegateSelectors());
    return executionCapabilities;
  }
}