package io.harness.delegate.task.servicenow;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.servicenow.ServiceNowCapabilityHelper;
import io.harness.delegate.beans.connector.servicenow.ServiceNowConnectorDTO;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.task.TaskParameters;
import io.harness.expression.ExpressionEvaluator;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.servicenow.ServiceNowActionNG;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@OwnedBy(CDC)
@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ServiceNowTaskNGParameters implements TaskParameters, ExecutionCapabilityDemander {
  ServiceNowConnectorDTO serviceNowConnectorDTO;
  List<EncryptedDataDetail> encryptionDetails;

  ServiceNowActionNG action;
  String ticketType;
  String ticketNumber;

  List<String> delegateSelectors;

  public Set<String> getDelegateSelectors() {
    Set<String> combinedDelegateSelectors = new HashSet<>();
    if (serviceNowConnectorDTO != null && serviceNowConnectorDTO.getDelegateSelectors() != null) {
      combinedDelegateSelectors.addAll(serviceNowConnectorDTO.getDelegateSelectors());
    }
    if (delegateSelectors != null) {
      combinedDelegateSelectors.addAll(delegateSelectors);
    }
    return combinedDelegateSelectors;
  }

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(ExpressionEvaluator maskingEvaluator) {
    return ServiceNowCapabilityHelper.generateDelegateCapabilities(
        serviceNowConnectorDTO, encryptionDetails, maskingEvaluator);
  }
}
