package software.wings.graphql.schema.mutation.cloudProvider.k8s;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.utils.RequestField;
import lombok.Builder;
import lombok.Value;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

@Value
@Builder
@Scope(PermissionAttribute.ResourceType.SETTING)
@JsonIgnoreProperties(ignoreUnknown = true)
public class QLUsernameAndPasswordAuthentication {
  private RequestField<String> userName;
  private RequestField<String> passwordSecretId;
}
