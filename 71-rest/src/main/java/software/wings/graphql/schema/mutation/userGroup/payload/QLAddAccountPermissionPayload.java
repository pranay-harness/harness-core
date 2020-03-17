package software.wings.graphql.schema.mutation.userGroup.payload;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import software.wings.graphql.schema.type.usergroup.QLUserGroup;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLAddAccountPermissionPayloadKeys")
@Scope(PermissionAttribute.ResourceType.USER)
public class QLAddAccountPermissionPayload {
  private String clientMutationId;
  private QLUserGroup userGroup;
}
