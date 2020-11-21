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
public class QLUpdatePcfCloudProviderInput {
  private RequestField<String> name;

  private RequestField<String> endpointUrl;
  private RequestField<String> userName;
  private RequestField<String> userNameSecretId;
  private RequestField<String> passwordSecretId;

  private RequestField<Boolean> skipValidation;
}
