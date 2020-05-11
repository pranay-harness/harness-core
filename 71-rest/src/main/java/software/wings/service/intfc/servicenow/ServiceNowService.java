package software.wings.service.intfc.servicenow;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import software.wings.api.ServiceNowExecutionData;
import software.wings.beans.SettingAttribute;
import software.wings.beans.approval.ApprovalPollingJobEntity;
import software.wings.service.impl.servicenow.ServiceNowServiceImpl.ServiceNowMetaDTO;
import software.wings.service.impl.servicenow.ServiceNowServiceImpl.ServiceNowTicketType;

import java.util.List;
import java.util.Map;

@OwnedBy(CDC)
public interface ServiceNowService {
  void validateCredential(SettingAttribute settingAttribute);
  List<ServiceNowMetaDTO> getStates(
      ServiceNowTicketType ticketType, String accountId, String connectorId, String appId);
  Map<String, List<ServiceNowMetaDTO>> getCreateMeta(
      ServiceNowTicketType ticketType, String accountId, String connectorId, String appId);
  List<ServiceNowMetaDTO> getAdditionalFields(
      ServiceNowTicketType ticketType, String accountId, String connectorId, String appId);
  ServiceNowExecutionData getIssueUrl(
      String issueNumber, String connectorId, ServiceNowTicketType ticketType, String appId, String accountId);
  ServiceNowExecutionData getApprovalStatus(String connectorId, String accountId, String appId, String issueNumber,
      String approvalField, String approvalValue, String rejectionField, String rejectionValue, String ticketType);
  ServiceNowExecutionData getApprovalStatus(ApprovalPollingJobEntity entity);

  void handleServiceNowPolling(ApprovalPollingJobEntity entity);
}
