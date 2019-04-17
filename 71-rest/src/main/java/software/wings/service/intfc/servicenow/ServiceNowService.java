package software.wings.service.intfc.servicenow;

import io.harness.beans.EmbeddedUser;
import software.wings.beans.ApprovalDetails;
import software.wings.beans.ApprovalDetails.Action;
import software.wings.beans.SettingAttribute;
import software.wings.service.impl.servicenow.ServiceNowServiceImpl.ServiceNowMetaDTO;
import software.wings.service.impl.servicenow.ServiceNowServiceImpl.ServiceNowTicketType;

import java.util.List;
import java.util.Map;

public interface ServiceNowService {
  void validateCredential(SettingAttribute settingAttribute);
  List<ServiceNowMetaDTO> getStates(
      ServiceNowTicketType ticketType, String accountId, String connectorId, String appId);
  Map<String, List<ServiceNowMetaDTO>> getCreateMeta(
      ServiceNowTicketType ticketType, String accountId, String connectorId, String appId);
  String getIssueUrl(
      String issueNumber, String connectorId, ServiceNowTicketType ticketType, String appId, String accountId);
  ApprovalDetails.Action getApprovalStatus(String connectorId, String accountId, String appId, String issueNumber,
      String approvalField, String approvalValue, String rejectionField, String rejectionValue, String ticketType);
  void approveWorkflow(Action action, String approvalId, EmbeddedUser user, String appId, String workflowExecutionId);
}
