package software.wings.graphql.schema.mutation.cloudProvider.k8s;

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
public class QLUpdateUsernameAndPasswordAuthentication {
  private RequestField<String> userName;
  private RequestField<String> userNameSecretId;
  private RequestField<String> passwordSecretId;
}
