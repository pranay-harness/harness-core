package software.wings.graphql.schema.mutation.application.payload;

import lombok.Builder;
import lombok.Value;
import software.wings.graphql.schema.mutation.QLMutationPayload;
import software.wings.graphql.schema.type.QLGitSyncConfig;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

@Value
@Builder
@Scope(PermissionAttribute.ResourceType.APPLICATION)
public class QLUpdateApplicationGitSyncConfigPayload implements QLMutationPayload {
  private String clientMutationId;
  private QLGitSyncConfig gitSyncConfig;
}
