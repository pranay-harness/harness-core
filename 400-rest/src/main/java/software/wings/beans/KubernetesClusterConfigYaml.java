package software.wings.beans;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.ccm.config.CCMConfigYaml;
import io.harness.k8s.model.KubernetesClusterAuthType;
import io.harness.k8s.model.OidcGrantType;

import software.wings.security.UsageRestrictionsYaml;
import software.wings.yaml.setting.CloudProviderYaml;

import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@TargetModule(HarnessModule._870_CG_YAML_BEANS)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class KubernetesClusterConfigYaml extends CloudProviderYaml {
  private boolean useKubernetesDelegate;
  private String delegateName;
  private List<String> delegateSelectors;
  private String masterUrl;
  private String username;
  private String usernameSecretId;
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
  private CCMConfigYaml continuousEfficiencyConfig;

  @lombok.Builder
  public KubernetesClusterConfigYaml(boolean useKubernetesDelegate, String delegateName, List<String> delegateSelectors,
      String type, String harnessApiVersion, String masterUrl, String username, String usernameSecretId,
      String password, String caCert, String clientCert, String clientKey, String clientKeyPassphrase,
      String serviceAccountToken, String clientKeyAlgo, boolean skipValidation, UsageRestrictionsYaml usageRestrictions,
      CCMConfigYaml ccmConfig, KubernetesClusterAuthType authType, String oidcIdentityProviderUrl, String oidcUsername,
      OidcGrantType oidcGrantType, String oidcScopes, String oidcSecret, String oidcPassword, String oidcClientId) {
    super(type, harnessApiVersion, usageRestrictions);
    this.useKubernetesDelegate = useKubernetesDelegate;
    this.delegateName = delegateName;
    this.delegateSelectors = delegateSelectors;
    this.masterUrl = masterUrl;
    this.username = username;
    this.usernameSecretId = usernameSecretId;
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
