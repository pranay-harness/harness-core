package software.wings.graphql.schema.mutation.cloudProvider.k8s;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.utils.RequestField;

import software.wings.graphql.schema.type.cloudProvider.k8s.QLManualClusterDetailsAuthenticationType;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@Scope(PermissionAttribute.ResourceType.SETTING)
@JsonIgnoreProperties(ignoreUnknown = true)
@TargetModule(Module._380_CG_GRAPHQL)
public class QLUpdateManualClusterDetails {
  private RequestField<String> masterUrl;

  private RequestField<QLManualClusterDetailsAuthenticationType> type;
  private RequestField<QLUpdateUsernameAndPasswordAuthentication> usernameAndPassword;
  private RequestField<QLUpdateServiceAccountToken> serviceAccountToken;
  private RequestField<QLUpdateOIDCToken> oidcToken;
  private RequestField<QLUpdateNone> none;
}
