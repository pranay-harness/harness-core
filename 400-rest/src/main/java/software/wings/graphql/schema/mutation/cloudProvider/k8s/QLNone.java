package software.wings.graphql.schema.mutation.cloudProvider.k8s;

import io.harness.utils.RequestField;

import software.wings.graphql.schema.type.secrets.QLUsageScope;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@Scope(PermissionAttribute.ResourceType.SETTING)
@JsonIgnoreProperties(ignoreUnknown = true)
public class QLNone {
  private RequestField<String> userName;
  private RequestField<String> passwordSecretId;
  private RequestField<String> caCertificateSecretId;
  private RequestField<String> clientCertificateSecretId;
  private RequestField<String> clientKeySecretId;
  private RequestField<String> clientKeyPassphraseSecretId;
  private RequestField<String> clientKeyAlgorithm;
  private RequestField<String> serviceAccountTokenSecretId;
  private RequestField<QLUsageScope> usageScope;
}
