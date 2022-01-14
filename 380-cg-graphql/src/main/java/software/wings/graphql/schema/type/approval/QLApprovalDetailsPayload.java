package software.wings.graphql.schema.type.approval;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import static software.wings.security.PermissionAttribute.PermissionType.DEPLOYMENT;
import static software.wings.security.PermissionAttribute.ResourceType.APPLICATION;

import io.harness.annotations.dev.OwnedBy;

import software.wings.security.annotations.Scope;

import java.util.List;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLApprovalDetailsPayloadKeys")
@Scope(value = APPLICATION, scope = DEPLOYMENT)
@OwnedBy(CDC)
public class QLApprovalDetailsPayload {
  List<QLApprovalDetails> approvalDetails;
}
