package io.harness.delegate.beans.connector.cvconnector;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
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
@OwnedBy(HarnessTeam.CV)
public class CVConnectorValidationParams implements ConnectorValidationParams, ExecutionCapabilityDemander {
  ConnectorConfigDTO connectorConfigDTO;
  List<EncryptedDataDetail> encryptedDataDetails;
  String connectorName;
  ConnectorType connectorType;

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(ExpressionEvaluator maskingEvaluator) {
    return CVConnectorCapabilitiesHelper.fetchRequiredExecutionCapabilities(connectorConfigDTO, maskingEvaluator);
  }

  @Override
  public ConnectorType getConnectorType() {
    return connectorType;
  }
}
