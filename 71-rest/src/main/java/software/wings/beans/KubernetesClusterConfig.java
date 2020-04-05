package software.wings.beans;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.wings.service.impl.KubernetesHelperService.getKubernetesConfigFromDefaultKubeConfigFile;
import static software.wings.service.impl.KubernetesHelperService.getKubernetesConfigFromServiceAccount;
import static software.wings.service.impl.KubernetesHelperService.isRunningInCluster;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonView;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.ccm.CCMConfig;
import io.harness.ccm.CloudCostAware;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.SystemEnvCheckerCapability;
import io.harness.delegate.task.mixin.HttpConnectionExecutionCapabilityGenerator;
import io.harness.encryption.Encrypted;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Transient;
import software.wings.annotation.EncryptableSetting;
import software.wings.audit.ResourceType;
import software.wings.beans.KubernetesConfig.KubernetesConfigBuilder;
import software.wings.jersey.JsonViews;
import software.wings.settings.SettingValue;
import software.wings.settings.UsageRestrictions;
import software.wings.yaml.setting.CloudProviderYaml;

import java.util.Collections;
import java.util.List;

@JsonTypeName("KUBERNETES_CLUSTER")
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@EqualsAndHashCode(callSuper = true)
public class KubernetesClusterConfig extends SettingValue implements EncryptableSetting, CloudCostAware {
  private boolean useKubernetesDelegate;
  private String delegateName;
  private String masterUrl;
  private String username;
  @Encrypted(fieldName = "password") private char[] password;
  @Encrypted(fieldName = "ca_certificate") private char[] caCert;
  @Encrypted(fieldName = "client_certificate") private char[] clientCert;
  @Encrypted(fieldName = "client_key") private char[] clientKey;
  @Encrypted(fieldName = "client_key_passphrase") private char[] clientKeyPassphrase;
  @Encrypted(fieldName = "service_account_token") private char[] serviceAccountToken;
  private String clientKeyAlgo;
  private boolean skipValidation;

  @JsonView(JsonViews.Internal.class) private String encryptedPassword;
  @JsonView(JsonViews.Internal.class) private String encryptedCaCert;
  @JsonView(JsonViews.Internal.class) private String encryptedClientCert;
  @JsonView(JsonViews.Internal.class) private String encryptedClientKey;
  @JsonView(JsonViews.Internal.class) private String encryptedClientKeyPassphrase;
  @JsonView(JsonViews.Internal.class) private String encryptedServiceAccountToken;

  @JsonInclude(Include.NON_NULL) private KubernetesClusterAuthType authType;

  // -- OIDC AUTH fields.
  private String oidcIdentityProviderUrl;
  private String oidcUsername;
  private OidcGrantType oidcGrantType;
  private String oidcScopes;
  @Encrypted(fieldName = "oidc_client_id") private char[] oidcClientId;
  @Encrypted(fieldName = "oidc_secret") private char[] oidcSecret;
  @Encrypted(fieldName = "oidc_password") private char[] oidcPassword;
  @JsonView(JsonViews.Internal.class) private String encryptedOidcSecret;
  @JsonView(JsonViews.Internal.class) private String encryptedOidcPassword;
  @JsonView(JsonViews.Internal.class) private String encryptedOidcClientId;
  // -- END OIDC AUTH fields.

  @NotEmpty private String accountId;

  @JsonInclude(Include.NON_NULL) @SchemaIgnore private CCMConfig ccmConfig;

  @Transient private boolean decrypted;

  public KubernetesClusterConfig() {
    super(SettingVariableTypes.KUBERNETES_CLUSTER.name());
  }

  @Builder
  public KubernetesClusterConfig(boolean useKubernetesDelegate, String delegateName, String masterUrl, String username,
      char[] password, char[] caCert, char[] clientCert, char[] clientKey, char[] clientKeyPassphrase,
      char[] serviceAccountToken, String clientKeyAlgo, boolean skipValidation, String encryptedPassword,
      String encryptedCaCert, String encryptedClientCert, String encryptedClientKey,
      String encryptedClientKeyPassphrase, String encryptedServiceAccountToken, String accountId, CCMConfig ccmConfig,
      boolean decrypted, KubernetesClusterAuthType authType, char[] oidcClientId, char[] oidcSecret,
      String oidcIdentityProviderUrl, String oidcUsername, char[] oidcPassword, String oidcScopes,
      String encryptedOidcSecret, String encryptedOidcPassword, String encryptedOidcClientId,
      OidcGrantType oidcGrantType) {
    this();
    this.useKubernetesDelegate = useKubernetesDelegate;
    this.delegateName = delegateName;
    this.masterUrl = masterUrl;
    this.username = username;
    this.password = password == null ? null : password.clone();
    this.caCert = caCert == null ? null : caCert.clone();
    this.clientCert = clientCert == null ? null : clientCert.clone();
    this.clientKey = clientKey == null ? null : clientKey.clone();
    this.clientKeyPassphrase = clientKeyPassphrase == null ? null : clientKeyPassphrase.clone();
    this.serviceAccountToken = serviceAccountToken == null ? null : serviceAccountToken.clone();
    this.clientKeyAlgo = clientKeyAlgo;
    this.skipValidation = skipValidation;
    this.encryptedPassword = encryptedPassword;
    this.encryptedCaCert = encryptedCaCert;
    this.encryptedClientCert = encryptedClientCert;
    this.encryptedClientKey = encryptedClientKey;
    this.encryptedClientKeyPassphrase = encryptedClientKeyPassphrase;
    this.encryptedServiceAccountToken = encryptedServiceAccountToken;
    this.accountId = accountId;
    this.ccmConfig = ccmConfig;
    this.decrypted = decrypted;
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
    this.oidcGrantType = oidcGrantType;
  }

  @Override
  @JsonIgnore
  @SchemaIgnore
  public boolean isDecrypted() {
    return decrypted || isNotBlank(delegateName);
  }

  @Override
  public String fetchResourceCategory() {
    return ResourceType.CLOUD_PROVIDER.name();
  }

  public KubernetesConfig createKubernetesConfig(String namespace) {
    String namespaceNotBlank = isNotBlank(namespace) ? namespace : "default";

    if (isUseKubernetesDelegate()) {
      if (isRunningInCluster()) {
        return getKubernetesConfigFromServiceAccount(namespaceNotBlank);
      } else {
        return getKubernetesConfigFromDefaultKubeConfigFile(namespaceNotBlank);
      }
    }

    KubernetesConfigBuilder kubernetesConfig = KubernetesConfig.builder()
                                                   .accountId(getAccountId())
                                                   .masterUrl(masterUrl)
                                                   .username(username)
                                                   .clientKeyAlgo(clientKeyAlgo)
                                                   .namespace(namespaceNotBlank);

    // Set fields needed by OIDC Auth Type
    if (KubernetesClusterAuthType.OIDC == authType) {
      return initWithOidcAuthDetails(kubernetesConfig);
    }

    if (isNotBlank(encryptedPassword)) {
      kubernetesConfig.encryptedPassword(encryptedPassword);
    } else {
      kubernetesConfig.password(password);
    }

    if (isNotBlank(encryptedCaCert)) {
      kubernetesConfig.encryptedCaCert(encryptedCaCert);
    } else {
      kubernetesConfig.caCert(caCert);
    }

    if (isNotBlank(encryptedClientCert)) {
      kubernetesConfig.encryptedClientCert(encryptedClientCert);
    } else {
      kubernetesConfig.clientCert(clientCert);
    }

    if (isNotBlank(encryptedClientKey)) {
      kubernetesConfig.encryptedClientKey(encryptedClientKey);
    } else {
      kubernetesConfig.clientKey(clientKey);
    }

    if (isNotBlank(encryptedClientKeyPassphrase)) {
      kubernetesConfig.encryptedClientKeyPassphrase(encryptedClientKeyPassphrase);
    } else {
      kubernetesConfig.clientKeyPassphrase(clientKeyPassphrase);
    }

    if (isNotBlank(encryptedServiceAccountToken)) {
      kubernetesConfig.encryptedServiceAccountToken(encryptedServiceAccountToken);
    } else {
      kubernetesConfig.serviceAccountToken(serviceAccountToken);
    }

    return kubernetesConfig.build();
  }

  private KubernetesConfig initWithOidcAuthDetails(KubernetesConfigBuilder kubernetesConfig) {
    kubernetesConfig.oidcClientId(oidcClientId);
    kubernetesConfig.encryptedOidcClientId(encryptedOidcClientId);
    kubernetesConfig.oidcSecret(oidcSecret);
    kubernetesConfig.encryptedOidcSecret(encryptedOidcSecret);
    kubernetesConfig.oidcUsername(oidcUsername);
    kubernetesConfig.oidcPassword(oidcPassword);
    kubernetesConfig.encryptedOidcPassword(encryptedOidcPassword);
    kubernetesConfig.oidcGrantType(oidcGrantType);
    kubernetesConfig.oidcIdentityProviderUrl(oidcIdentityProviderUrl);
    kubernetesConfig.authType(authType);
    kubernetesConfig.oidcScopes(oidcScopes);

    return kubernetesConfig.build();
  }

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    if (useKubernetesDelegate) {
      return Collections.singletonList(
          SystemEnvCheckerCapability.builder().comparate(delegateName).systemPropertyName("DELEGATE_NAME").build());
    }
    return Collections.singletonList(
        HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapability(masterUrl));
  }

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = false)
  public static class Yaml extends CloudProviderYaml {
    private boolean useKubernetesDelegate;
    private String delegateName;
    private String masterUrl;
    private String username;
    private String password;
    private String caCert;
    private String clientCert;
    private String clientKey;
    private String clientKeyPassphrase;
    private String serviceAccountToken;
    private String clientKeyAlgo;
    private boolean skipValidation;
    private KubernetesClusterAuthType authType;
    private String oidcIdentityProviderUrl;
    private String oidcUsername;
    private OidcGrantType oidcGrantType;
    private String oidcScopes;
    private String oidcSecret;
    private String oidcPassword;
    private String oidcClientId;
    private CCMConfig.Yaml continuousEfficiencyConfig;

    @lombok.Builder
    public Yaml(boolean useKubernetesDelegate, String delegateName, String type, String harnessApiVersion,
        String masterUrl, String username, String password, String caCert, String clientCert, String clientKey,
        String clientKeyPassphrase, String serviceAccountToken, String clientKeyAlgo, boolean skipValidation,
        UsageRestrictions.Yaml usageRestrictions, CCMConfig.Yaml ccmConfig, KubernetesClusterAuthType authType,
        String oidcIdentityProviderUrl, String oidcUsername, OidcGrantType oidcGrantType, String oidcScopes,
        String oidcSecret, String oidcPassword, String oidcClientId) {
      super(type, harnessApiVersion, usageRestrictions);
      this.useKubernetesDelegate = useKubernetesDelegate;
      this.delegateName = delegateName;
      this.masterUrl = masterUrl;
      this.username = username;
      this.password = password;
      this.caCert = caCert;
      this.clientCert = clientCert;
      this.clientKey = clientKey;
      this.clientKeyPassphrase = clientKeyPassphrase;
      this.serviceAccountToken = serviceAccountToken;
      this.clientKeyAlgo = clientKeyAlgo;
      this.skipValidation = skipValidation;
      this.continuousEfficiencyConfig = ccmConfig;
      this.authType = authType;
      this.oidcIdentityProviderUrl = oidcIdentityProviderUrl;
      this.oidcUsername = oidcUsername;
      this.oidcPassword = oidcPassword;
      this.oidcClientId = oidcClientId;
      this.oidcGrantType = oidcGrantType;
      this.oidcSecret = oidcSecret;
    }
  }
}
