package io.harness.delegate.beans.connector.awsconnector;

import io.harness.delegate.beans.connector.ConnectorTaskParams;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.delegate.beans.connector.ConnectorValidationParams;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.expression.ExpressionEvaluator;
import io.harness.security.encryption.EncryptedDataDetail;

import java.util.List;
import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
public class AwsValidationParams
    extends ConnectorTaskParams implements ConnectorValidationParams, ExecutionCapabilityDemander {
  AwsConnectorDTO awsConnectorDTO;
  List<EncryptedDataDetail> encryptedDataDetails;
  String connectorName;

  @Override
  public ConnectorType getConnectorType() {
    return ConnectorType.AWS;
  }

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(ExpressionEvaluator maskingEvaluator) {
    return AwsCapabilityHelper.fetchRequiredExecutionCapabilities(awsConnectorDTO, maskingEvaluator);
  }
}
