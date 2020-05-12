package software.wings.graphql.schema.mutation.cloudProvider.k8s;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.utils.RequestField;
import lombok.Builder;
import lombok.Value;
import software.wings.graphql.schema.type.cloudProvider.k8s.QLManualClusterDetailsAuthenticationType;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

@Value
@Builder
@Scope(PermissionAttribute.ResourceType.SETTING)
@JsonIgnoreProperties(ignoreUnknown = true)
public class QLManualClusterDetails {
  private RequestField<String> masterUrl;

  private RequestField<QLManualClusterDetailsAuthenticationType> type;
  private RequestField<QLUsernameAndPasswordAuthentication> usernameAndPassword;
  private RequestField<QLServiceAccountToken> serviceAccountToken;
  private RequestField<QLOIDCToken> oidcToken;
  private RequestField<QLNone> none;
}
