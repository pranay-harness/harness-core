package software.wings.service.intfc.servicenow;

import software.wings.beans.ServiceNowConfig;
import software.wings.beans.TaskType;
import software.wings.delegatetasks.DelegateTaskType;

public interface ServiceNowDelegateService {
  @DelegateTaskType(TaskType.SERVICENOW_VALIDATION) void validateConnector(ServiceNowConfig config);
}
