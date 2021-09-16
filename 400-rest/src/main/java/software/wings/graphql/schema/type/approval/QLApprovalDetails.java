package software.wings.graphql.schema.type.approval;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EmbeddedUser;

import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;
import software.wings.sm.states.ApprovalState.ApprovalStateType;

@Scope(PermissionAttribute.ResourceType.PIPELINE)
@OwnedBy(CDC)
public interface QLApprovalDetails {
  String getApprovalId();
  ApprovalStateType getApprovalType();
  String getApprovalName();
  String getStageName();
  String getStepName();
  Long getStartedAt();
  Long getWillExpireAt();
  EmbeddedUser getTriggeredBy();
}
