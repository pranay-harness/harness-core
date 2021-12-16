package io.harness.delegate.beans.connector.servicenow;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.ConnectorTaskParams;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.task.TaskParameters;
import io.harness.expression.ExpressionEvaluator;
import io.harness.security.encryption.EncryptedDataDetail;

import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@OwnedBy(CDC)
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class ServiceNowConnectionTaskParams
    extends ConnectorTaskParams implements TaskParameters, ExecutionCapabilityDemander {
  ServiceNowConnectorDTO serviceNowConnectorDTO;
  List<EncryptedDataDetail> encryptionDetails;

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(ExpressionEvaluator maskingEvaluator) {
    return ServiceNowCapabilityHelper.fetchRequiredExecutionCapabilitiesForValidation(
        maskingEvaluator, serviceNowConnectorDTO);
  }
}
