package software.wings.beans;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonView;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Transient;
import software.wings.annotation.Encryptable;
import software.wings.annotation.Encrypted;
import software.wings.beans.KubernetesConfig.KubernetesConfigBuilder;
import software.wings.jersey.JsonViews;
import software.wings.settings.SettingValue;
import software.wings.settings.UsageRestrictions;
import software.wings.yaml.setting.CloudProviderYaml;

@JsonTypeName("KUBERNETES_CLUSTER")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
public class KubernetesClusterConfig extends SettingValue implements Encryptable {
  private boolean useKubernetesDelegate;
  private String delegateName;
  private String masterUrl;
  private String username;
  @Encrypted private char[] password;
  @Encrypted private char[] caCert;
  @Encrypted private char[] clientCert;
  @Encrypted private char[] clientKey;
  @Encrypted private char[] clientKeyPassphrase;
  private String clientKeyAlgo;

  @JsonView(JsonViews.Internal.class) private String encryptedPassword;
  @JsonView(JsonViews.Internal.class) private String encryptedCaCert;
  @JsonView(JsonViews.Internal.class) private String encryptedClientCert;
  @JsonView(JsonViews.Internal.class) private String encryptedClientKey;
  @JsonView(JsonViews.Internal.class) private String encryptedClientKeyPassphrase;

  @NotEmpty private String accountId;

  @Transient private boolean decrypted;

  public KubernetesClusterConfig() {
    super(SettingVariableTypes.KUBERNETES_CLUSTER.name());
  }

  public KubernetesClusterConfig(boolean useKubernetesDelegate, String delegateName, String masterUrl, String username,
      char[] password, char[] caCert, char[] clientCert, char[] clientKey, char[] clientKeyPassphrase,
      String clientKeyAlgo, String encryptedPassword, String encryptedCaCert, String encryptedClientCert,
      String encryptedClientKey, String encryptedClientKeyPassphrase, String accountId, boolean decrypted) {
    this();
    this.useKubernetesDelegate = useKubernetesDelegate;
    this.delegateName = delegateName;
    this.masterUrl = masterUrl;
    this.username = username;
    this.password = password;
    this.caCert = caCert;
    this.clientCert = clientCert;
    this.clientKey = clientKey;
    this.clientKeyPassphrase = clientKeyPassphrase;
    this.clientKeyAlgo = clientKeyAlgo;
    this.encryptedPassword = encryptedPassword;
    this.encryptedCaCert = encryptedCaCert;
    this.encryptedClientCert = encryptedClientCert;
    this.encryptedClientKey = encryptedClientKey;
    this.encryptedClientKeyPassphrase = encryptedClientKeyPassphrase;
    this.accountId = accountId;
    this.decrypted = decrypted;
  }

  public KubernetesConfig createKubernetesConfig(String namespace) {
    String namespaceNotBlank = isNotBlank(namespace) ? namespace : "default";

    if (isUseKubernetesDelegate()) {
      return KubernetesConfig.builder().namespace(namespaceNotBlank).build();
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

    return kubernetesConfig.build();
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
    private String clientKeyAlgo;

    @lombok.Builder
    public Yaml(boolean useKubernetesDelegate, String delegateName, String type, String harnessApiVersion,
        String masterUrl, String username, String password, String caCert, String clientCert, String clientKey,
        String clientKeyPassphrase, String clientKeyAlgo, UsageRestrictions usageRestrictions) {
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
      this.clientKeyAlgo = clientKeyAlgo;
    }
  }
}
