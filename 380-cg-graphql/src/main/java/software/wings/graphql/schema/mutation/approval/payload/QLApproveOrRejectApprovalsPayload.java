package software.wings.graphql.schema.mutation.approval.payload;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import static software.wings.security.PermissionAttribute.PermissionType.DEPLOYMENT;
import static software.wings.security.PermissionAttribute.ResourceType.APPLICATION;

import io.harness.annotations.dev.OwnedBy;

import software.wings.security.annotations.Scope;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "ApproveOrRejectApprovalsPayloadKeys")
@Scope(value = APPLICATION, scope = DEPLOYMENT)
@OwnedBy(CDC)
public class QLApproveOrRejectApprovalsPayload {
  Boolean success;
  String clientMutationId;
}
