package software.wings.beans.servicenow;

import lombok.Builder;
import lombok.Data;
import software.wings.beans.ServiceNowConfig;
import software.wings.delegatetasks.jira.ServiceNowAction;

@Data
@Builder
public class ServiceNowTaskParameters {
  private String accountId;
  private ServiceNowConfig serviceNowConfig;
  private ServiceNowAction action;
}
