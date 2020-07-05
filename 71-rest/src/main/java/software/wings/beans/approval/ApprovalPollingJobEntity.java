package software.wings.beans.approval;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static software.wings.beans.approval.ServiceNowApprovalParams.validateTimeWindow;

import io.harness.annotation.HarnessEntity;
import io.harness.annotations.dev.OwnedBy;
import io.harness.iterator.PersistentRegularIterable;
import io.harness.mongo.index.FdIndex;
import io.harness.persistence.AccountAccess;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import software.wings.service.impl.servicenow.ServiceNowServiceImpl.ServiceNowTicketType;
import software.wings.sm.states.ApprovalState.ApprovalStateType;

import java.util.Map;

@OwnedBy(CDC)
@FieldNameConstants(innerTypeName = "ApprovalPollingJobEntityKeys")
@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@ToString(exclude = "scriptString")
@Entity(value = "approvalPollingJob")
@HarnessEntity(exportable = false)
@Slf4j
public class ApprovalPollingJobEntity implements PersistentRegularIterable, AccountAccess {
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
  Criteria approval;
  Criteria rejection;
  boolean changeWindowPresent;
  String changeWindowStartField;
  String changeWindowEndField;

  // shell script approval fields
  String scriptString;
  String activityId;

  ApprovalStateType approvalType;

  public boolean withinChangeWindow(Map<String, String> currentStatus) {
    if (changeWindowPresent) {
      return validateTimeWindow(changeWindowEndField, changeWindowStartField, currentStatus);
    }
    return true;
  }

  @FdIndex private Long nextIteration;

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
