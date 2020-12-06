package software.wings.graphql.schema.mutation.cloudProvider;

import io.harness.utils.RequestField;

import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@Scope(PermissionAttribute.ResourceType.SETTING)
@JsonIgnoreProperties(ignoreUnknown = true)
public class QLUpdateAzureCloudProviderInput {
  private RequestField<String> name;

  private RequestField<String> clientId;
  private RequestField<String> tenantId;
  private RequestField<String> keySecretId;
}
