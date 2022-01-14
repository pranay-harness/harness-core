package software.wings.graphql.schema.type.approval;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.EmbeddedUser;

import software.wings.beans.NameValuePair;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;
import software.wings.sm.states.ApprovalState.ApprovalStateType;

import java.util.List;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLUGApprovalDetailsKeys")
@Scope(PermissionAttribute.ResourceType.DEPLOYMENT)
@TargetModule(HarnessModule._380_CG_GRAPHQL)
@OwnedBy(CDC)
public class QLUGApprovalDetails implements QLApprovalDetails {
  String approvalId;
  ApprovalStateType approvalType;
  String approvalName;
  String stageName;
  String appId;
  Long startedAt;
  Long willExpireAt;
  Integer timeoutMillis;
  EmbeddedUser triggeredBy;
  List<String> approvers; // UserGroups
  String stepName;
  List<NameValuePair> variables;
  String executionId;
}
