package software.wings.service.intfc.servicenow;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import software.wings.api.ServiceNowExecutionData;
import software.wings.beans.TaskType;
import software.wings.beans.approval.ServiceNowApprovalParams;
import software.wings.beans.servicenow.ServiceNowTaskParameters;
import software.wings.delegatetasks.DelegateTaskType;
import software.wings.service.impl.servicenow.ServiceNowServiceImpl.ServiceNowMetaDTO;

import java.util.List;
import java.util.Map;
import java.util.Set;

@OwnedBy(CDC)
public interface ServiceNowDelegateService {
  @DelegateTaskType(TaskType.SERVICENOW_VALIDATION) boolean validateConnector(ServiceNowTaskParameters taskParameters);
  @DelegateTaskType(TaskType.SERVICENOW_SYNC)
  List<ServiceNowMetaDTO> getStates(ServiceNowTaskParameters taskParameters);
  @DelegateTaskType(TaskType.SERVICENOW_SYNC)
  List<ServiceNowMetaDTO> getApprovalStates(ServiceNowTaskParameters taskParameters);
  @DelegateTaskType(TaskType.SERVICENOW_SYNC)
  Map<String, List<ServiceNowMetaDTO>> getCreateMeta(ServiceNowTaskParameters taskParameters);
  @DelegateTaskType(TaskType.SERVICENOW_SYNC)
  List<ServiceNowMetaDTO> getAdditionalFields(ServiceNowTaskParameters taskParameters);
  @DelegateTaskType(TaskType.SERVICENOW_SYNC)
  ServiceNowExecutionData getIssueUrl(ServiceNowTaskParameters taskParameters, ServiceNowApprovalParams approvalParams);
  @DelegateTaskType(TaskType.SERVICENOW_SYNC)
  String getIssueFieldStatus(ServiceNowTaskParameters taskParameters, String field);
  @DelegateTaskType(TaskType.SERVICENOW_SYNC)
  Map<String, String> getIssueStatus(ServiceNowTaskParameters taskParameters, Set<String> criteriaFields);
}
