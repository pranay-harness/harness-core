package software.wings.graphql.schema.mutation.cloudProvider;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.utils.RequestField;
import lombok.Builder;
import lombok.Value;
import software.wings.graphql.schema.type.secrets.QLUsageScope;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

@Value
@Builder
@Scope(PermissionAttribute.ResourceType.SETTING)
@JsonIgnoreProperties(ignoreUnknown = true)
public class QLUpdatePcfCloudProviderInput {
  private RequestField<String> name;
  private RequestField<QLUsageScope> usageScope;

  private RequestField<String> endpointUrl;
  private RequestField<String> userName;
  private RequestField<String> passwordSecretId;
}
