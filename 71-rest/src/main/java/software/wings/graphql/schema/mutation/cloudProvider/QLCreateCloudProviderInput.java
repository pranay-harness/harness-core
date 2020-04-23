package software.wings.graphql.schema.mutation.cloudProvider;

import lombok.Builder;
import lombok.Value;
import software.wings.graphql.schema.mutation.QLMutationInput;
import software.wings.graphql.schema.type.QLCloudProviderType;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

@Value
@Builder
@Scope(PermissionAttribute.ResourceType.SETTING)
public class QLCreateCloudProviderInput implements QLMutationInput {
  String clientMutationId;

  QLCloudProviderType cloudProviderType;
  QLPcfCloudProviderInput pcfCloudProvider;
}
