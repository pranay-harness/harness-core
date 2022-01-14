package io.harness.steps.approval.step.servicenow;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.servicenow.ServiceNowConnectorDTO;
import io.harness.steps.approval.step.servicenow.entities.ServiceNowApprovalInstance;

@OwnedBy(CDC)
public interface ServiceNowApprovalHelperService {
  ServiceNowConnectorDTO getServiceNowConnector(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String connectorIdentifierRef);

  void handlePollingEvent(ServiceNowApprovalInstance serviceNowApprovalInstance);
}
