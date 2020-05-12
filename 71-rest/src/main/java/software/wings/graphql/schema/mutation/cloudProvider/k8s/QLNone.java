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
public class QLNone {
  private RequestField<String> userName;
  private RequestField<String> passwordSecretId;
  private RequestField<String> caCertificateSecretId;
  private RequestField<String> clientCertificateSecretId;
  private RequestField<String> clientKeySecretId;
  private RequestField<String> clientKeyPassphraseSecretId;
  private RequestField<String> clientKeyAlgorithm;
  private RequestField<String> serviceAccountTokenSecretId;
}
