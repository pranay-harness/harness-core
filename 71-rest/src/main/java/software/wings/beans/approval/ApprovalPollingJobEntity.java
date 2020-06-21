package software.wings.beans.approval;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotation.HarnessEntity;
import io.harness.annotations.dev.OwnedBy;
import io.harness.iterator.PersistentRegularIterable;
import io.harness.mongo.index.Indexed;
import io.harness.persistence.AccountAccess;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import software.wings.service.impl.servicenow.ServiceNowServiceImpl.ServiceNowTicketType;
import software.wings.sm.states.ApprovalState.ApprovalStateType;

import java.util.HashSet;
import java.util.Set;

@OwnedBy(CDC)
@FieldNameConstants(innerTypeName = "ApprovalPollingJobEntityKeys")
@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@ToString(exclude = "scriptString")
@Entity(value = "approvalPollingJob")
@HarnessEntity(exportable = false)
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

  // shell script approval fields
  String scriptString;
  String activityId;

  ApprovalStateType approvalType;

  public Set<String> getAllServiceNowFields() {
    Set<String> fields = new HashSet<>();
    if (approval != null) {
      approval.fetchConditions().keySet().forEach(field -> fields.add(field));
    }
    if (rejection != null) {
      rejection.fetchConditions().keySet().forEach(field -> fields.add(field));
    }
    return fields;
  }

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
