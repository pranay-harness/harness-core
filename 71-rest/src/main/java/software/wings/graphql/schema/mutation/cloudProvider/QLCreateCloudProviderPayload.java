package software.wings.graphql.schema.mutation.cloudProvider;

import software.wings.graphql.schema.mutation.QLMutationPayload;
import software.wings.graphql.schema.type.cloudProvider.QLCloudProvider;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@Scope(PermissionAttribute.ResourceType.SETTING)
public class QLCreateCloudProviderPayload implements QLMutationPayload {
  String clientMutationId;

  QLCloudProvider cloudProvider;
}
