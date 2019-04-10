package software.wings.beans.approval;

import lombok.Getter;
import lombok.Setter;
import software.wings.service.impl.servicenow.ServiceNowServiceImpl.ServiceNowTicketType;

@Getter
@Setter
public class ServiceNowApprovalParams {
  private String snowConnectorId;
  private String approvalField;
  private String approvalValue;
  private String rejectionField;
  private String rejectionValue;
  private String issueNumber;
  private ServiceNowTicketType ticketType;
}
