package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.task.mixin.HttpConnectionExecutionCapabilityGenerator;
import io.harness.encryption.Encrypted;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.annotation.EncryptableSetting;
import software.wings.audit.ResourceType;
import software.wings.settings.SettingValue;

import java.util.Arrays;
import java.util.List;

/**
 * Created by bzane on 2/28/17
 */
@JsonTypeName("KUBERNETES")
@Data
@ToString(exclude = {"password", "caCert", "clientCert", "clientKey", "clientKeyPassphrase", "serviceAccountToken"})
@EqualsAndHashCode(callSuper = true)
public class KubernetesConfig extends SettingValue implements EncryptableSetting, ExecutionCapabilityDemander {
  @NotEmpty private String masterUrl;
  private String username;
  @Encrypted private char[] password;
  @Encrypted private char[] caCert;
  @Encrypted private char[] clientCert;
  @Encrypted private char[] clientKey;
  @Encrypted private char[] clientKeyPassphrase;
  @Encrypted private char[] serviceAccountToken;
  private String clientKeyAlgo;
  private String namespace;
  @NotEmpty private String accountId;

  private String encryptedPassword;
  private String encryptedCaCert;
  private String encryptedClientCert;
  private String encryptedClientKey;
  private String encryptedClientKeyPassphrase;
  private String encryptedServiceAccountToken;

  private KubernetesClusterAuthType authType;
  // -- OIDC AUTH fields.
  private String oidcIdentityProviderUrl;
  private String oidcUsername;
  private OidcGrantType oidcGrantType;
  private String oidcScopes;
  @Encrypted private char[] oidcClientId;
  @Encrypted private char[] oidcSecret;
  @Encrypted private char[] oidcPassword;
  private String encryptedOidcSecret;
  private String encryptedOidcPassword;
  private String encryptedOidcClientId;

  /**
   * Instantiates a new setting value.
   */
  public KubernetesConfig() {
    super(SettingVariableTypes.KUBERNETES.name());
  }

  @Builder
  public KubernetesConfig(String masterUrl, String username, char[] password, char[] caCert, char[] clientCert,
      char[] clientKey, char[] clientKeyPassphrase, char[] serviceAccountToken, String clientKeyAlgo, String namespace,
      String accountId, String encryptedPassword, String encryptedCaCert, String encryptedClientCert,
      String encryptedClientKey, String encryptedClientKeyPassphrase, String encryptedServiceAccountToken,
      KubernetesClusterAuthType authType, char[] oidcClientId, char[] oidcSecret, String oidcIdentityProviderUrl,
      String oidcUsername, char[] oidcPassword, String oidcScopes, String encryptedOidcSecret,
      String encryptedOidcPassword, String encryptedOidcClientId, OidcGrantType oidcGrantType) {
    this();
    this.masterUrl = masterUrl;
    this.username = username;
    this.password = password == null ? null : password.clone();
    this.caCert = caCert == null ? null : caCert.clone();
    this.clientCert = clientCert == null ? null : clientCert.clone();
    this.clientKey = clientKey == null ? null : clientKey.clone();
    this.clientKeyPassphrase = clientKeyPassphrase == null ? null : clientKeyPassphrase.clone();
    this.serviceAccountToken = serviceAccountToken == null ? null : serviceAccountToken.clone();
    this.clientKeyAlgo = clientKeyAlgo;
    this.namespace = namespace;
    this.accountId = accountId;
    this.encryptedPassword = encryptedPassword;
    this.encryptedCaCert = encryptedCaCert;
    this.encryptedClientCert = encryptedClientCert;
    this.encryptedClientKey = encryptedClientKey;
    this.encryptedClientKeyPassphrase = encryptedClientKeyPassphrase;
    this.encryptedServiceAccountToken = encryptedServiceAccountToken;
    this.authType = authType;
    this.oidcClientId = oidcClientId == null ? null : oidcClientId.clone();
    this.oidcSecret = oidcSecret == null ? null : oidcSecret.clone();
    this.oidcIdentityProviderUrl = oidcIdentityProviderUrl;
    this.oidcUsername = oidcUsername;
    this.oidcPassword = oidcPassword == null ? null : oidcPassword.clone();
    this.oidcScopes = oidcScopes;
    this.encryptedOidcClientId = encryptedOidcClientId;
    this.encryptedOidcPassword = encryptedOidcPassword;
    this.encryptedOidcSecret = encryptedOidcSecret;
    this.oidcGrantType = oidcGrantType == null ? OidcGrantType.password : oidcGrantType;
  }

  @Override
  public String fetchResourceCategory() {
    return ResourceType.CLOUD_PROVIDER.name();
  }

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    return Arrays.asList(HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapability(masterUrl));
  }
}
