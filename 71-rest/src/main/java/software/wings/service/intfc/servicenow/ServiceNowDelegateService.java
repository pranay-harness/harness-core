package software.wings.service.intfc.servicenow;

import software.wings.beans.TaskType;
import software.wings.beans.servicenow.ServiceNowTaskParameters;
import software.wings.delegatetasks.DelegateTaskType;
import software.wings.service.impl.servicenow.ServiceNowServiceImpl.ServiceNowState;

import java.util.List;

public interface ServiceNowDelegateService {
  @DelegateTaskType(TaskType.SERVICENOW_VALIDATION) boolean validateConnector(ServiceNowTaskParameters taskParameters);
  @DelegateTaskType(TaskType.SERVICENOW_SYNC) List<ServiceNowState> getStates(ServiceNowTaskParameters taskParameters);
  @DelegateTaskType(TaskType.SERVICENOW_SYNC) String getIssueUrl(ServiceNowTaskParameters taskParameters);
  @DelegateTaskType(TaskType.SERVICENOW_SYNC) String getIssueStatus(ServiceNowTaskParameters taskParameters);
}
