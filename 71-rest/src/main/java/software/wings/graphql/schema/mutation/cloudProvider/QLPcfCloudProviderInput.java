package software.wings.graphql.schema.mutation.cloudProvider;

import io.harness.utils.RequestField;
import lombok.Builder;
import lombok.Value;
import software.wings.graphql.schema.type.secrets.QLUsageScope;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

@Value
@Builder
@Scope(PermissionAttribute.ResourceType.SETTING)
public class QLPcfCloudProviderInput {
  private RequestField<String> name;
  private RequestField<QLUsageScope> usageScope;

  private RequestField<String> endpointUrl;
  private RequestField<String> userName;
  private RequestField<String> password;
}
