package software.wings.graphql.schema.mutation.cloudProvider;

import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@Scope(PermissionAttribute.ResourceType.SETTING)
public class QLDeleteCloudProviderPayload {
  String clientMutationId;
}
