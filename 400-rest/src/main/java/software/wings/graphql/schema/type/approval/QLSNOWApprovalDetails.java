package software.wings.graphql.schema.type.approval;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.EmbeddedUser;

import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;
import software.wings.service.impl.servicenow.ServiceNowServiceImpl.ServiceNowTicketType;
import software.wings.sm.states.ApprovalState.ApprovalStateType;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLSNOWApprovalDetailsKeys")
@Scope(PermissionAttribute.ResourceType.DEPLOYMENT)
@TargetModule(HarnessModule._380_CG_GRAPHQL)
@OwnedBy(CDC)
public class QLSNOWApprovalDetails implements QLApprovalDetails {
  String approvalId;
  ApprovalStateType approvalType;
  String approvalName;
  String stageName;
  String stepName;
  Long startedAt;
  Long willExpireAt;
  String ticketUrl;
  ServiceNowTicketType ticketType;
  String approvalCondition;
  String rejectionCondition;
  EmbeddedUser triggeredBy;
  String currentStatus;
}
