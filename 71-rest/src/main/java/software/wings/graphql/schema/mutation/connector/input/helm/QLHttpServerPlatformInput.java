package software.wings.graphql.schema.mutation.connector.input.helm;

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
public class QLHttpServerPlatformInput {
  private RequestField<String> URL;
  private RequestField<String> userName;
  private RequestField<String> passwordSecretId;
}
