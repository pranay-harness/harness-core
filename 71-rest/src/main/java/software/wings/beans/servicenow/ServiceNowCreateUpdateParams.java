package software.wings.beans.servicenow;

import lombok.Getter;
import lombok.Setter;
import software.wings.delegatetasks.servicenow.ServiceNowAction;

import java.util.Map;

public class ServiceNowCreateUpdateParams {
  @Getter @Setter private ServiceNowAction action;
  @Getter @Setter String snowConnectorId;
  @Getter @Setter private String ticketType;
  @Setter private Map<ServiceNowFields, String> fields;
  @Getter @Setter private String issueNumber;
  @Getter @Setter private String ticketId;

  public Map<ServiceNowFields, String> fetchFields() {
    return fields;
  }
}
