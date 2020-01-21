package software.wings.graphql.schema.mutation.application.payload;

import lombok.Builder;
import lombok.Value;
import software.wings.graphql.schema.mutation.QLMutationPayload;
import software.wings.graphql.schema.type.QLApplication;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

@Value
@Builder
@Scope(PermissionAttribute.ResourceType.APPLICATION)
public class QLRemoveApplicationGitConfigPayload implements QLMutationPayload {
  private String requestId;
  private QLApplication application;
}
