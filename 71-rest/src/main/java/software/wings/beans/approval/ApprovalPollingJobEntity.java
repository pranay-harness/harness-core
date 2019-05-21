package software.wings.beans.approval;

import io.harness.persistence.PersistentIterable;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Indexed;
import software.wings.service.impl.servicenow.ServiceNowServiceImpl.ServiceNowTicketType;
import software.wings.sm.states.ApprovalState.ApprovalStateType;

@Entity(value = "approvalPollingJob")
@FieldNameConstants(innerTypeName = "ApprovalPollingJobEntityKeys")
@Data
@Builder
public class ApprovalPollingJobEntity implements PersistentIterable {
  String appId;
  String accountId;
  String stateExecutionInstanceId;
  String workflowExecutionId;

  String connectorId;
  @Id String approvalId;
  String approvalField;
  String approvalValue;
  String rejectionField;
  String rejectionValue;

  // jira fields
  String issueId;

  // snow fields
  String issueNumber;
  ServiceNowTicketType issueType;

  ApprovalStateType approvalType;

  @Indexed private Long nextIteration;

  @Override
  public Long obtainNextIteration(String fieldName) {
    return nextIteration;
  }

  @Override
  public void updateNextIteration(String fieldName, Long nextIteration) {
    this.nextIteration = nextIteration;
  }

  @Override
  public String getUuid() {
    return approvalId;
  }
}
