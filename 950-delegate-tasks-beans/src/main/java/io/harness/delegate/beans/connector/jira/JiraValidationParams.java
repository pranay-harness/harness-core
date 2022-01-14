package io.harness.delegate.beans.connector.jira;

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
public class JiraValidationParams
    extends ConnectorTaskParams implements ConnectorValidationParams, ExecutionCapabilityDemander {
  JiraConnectorDTO jiraConnectorDTO;
  List<EncryptedDataDetail> encryptedDataDetails;
  String connectorName;

  @Override
  public ConnectorType getConnectorType() {
    return ConnectorType.JIRA;
  }

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(ExpressionEvaluator maskingEvaluator) {
    return JiraCapabilityHelper.fetchRequiredExecutionCapabilities(maskingEvaluator, jiraConnectorDTO);
  }
}
