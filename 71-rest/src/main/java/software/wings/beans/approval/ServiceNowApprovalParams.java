package software.wings.beans.approval;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import lombok.Getter;
import lombok.Setter;
import software.wings.service.impl.servicenow.ServiceNowServiceImpl.ServiceNowTicketType;

import java.util.HashSet;
import java.util.Set;

@OwnedBy(CDC)
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
  private Criteria approval;
  private Criteria rejection;

  public Set<String> getAllCriteriaFields() {
    Set<String> fields = new HashSet<>();
    if (approval != null) {
      approval.fetchConditions().keySet().forEach(field -> fields.add(field));
    }
    if (rejection != null) {
      rejection.fetchConditions().keySet().forEach(field -> fields.add(field));
    }
    return fields;
  }
}
