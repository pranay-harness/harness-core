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
@Builder
@EqualsAndHashCode(callSuper = true)
public class KubernetesClusterConfig extends SettingValue implements EncryptableSetting, CloudCostAware {
  private boolean useKubernetesDelegate;
  private String delegateName;
  private String masterUrl;
  private String username;
  @Encrypted private char[] password;
  @Encrypted private char[] caCert;
  @Encrypted private char[] clientCert;
  @Encrypted private char[] clientKey;
  @Encrypted private char[] clientKeyPassphrase;
  @Encrypted private char[] serviceAccountToken;
  private String clientKeyAlgo;
  private boolean skipValidation;

  @JsonView(JsonViews.Internal.class) private String encryptedPassword;
  @JsonView(JsonViews.Internal.class) private String encryptedCaCert;
  @JsonView(JsonViews.Internal.class) private String encryptedClientCert;
  @JsonView(JsonViews.Internal.class) private String encryptedClientKey;
  @JsonView(JsonViews.Internal.class) private String encryptedClientKeyPassphrase;
  @JsonView(JsonViews.Internal.class) private String encryptedServiceAccountToken;

  @NotEmpty private String accountId;

  @JsonInclude(Include.NON_NULL) @SchemaIgnore private CCMConfig ccmConfig;

  @Transient private boolean decrypted;

  public KubernetesClusterConfig() {
    super(SettingVariableTypes.KUBERNETES_CLUSTER.name());
  }

  public KubernetesClusterConfig(boolean useKubernetesDelegate, String delegateName, String masterUrl, String username,
      char[] password, char[] caCert, char[] clientCert, char[] clientKey, char[] clientKeyPassphrase,
      char[] serviceAccountToken, String clientKeyAlgo, boolean skipValidation, String encryptedPassword,
      String encryptedCaCert, String encryptedClientCert, String encryptedClientKey,
      String encryptedClientKeyPassphrase, String encryptedServiceAccountToken, String accountId, CCMConfig ccmConfig,
      boolean decrypted) {
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
    private CCMConfig.Yaml continuousEfficiencyConfig;

    @lombok.Builder
    public Yaml(boolean useKubernetesDelegate, String delegateName, String type, String harnessApiVersion,
        String masterUrl, String username, String password, String caCert, String clientCert, String clientKey,
        String clientKeyPassphrase, String serviceAccountToken, String clientKeyAlgo, boolean skipValidation,
        UsageRestrictions.Yaml usageRestrictions, CCMConfig.Yaml ccmConfig) {
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
    }
  }
}
