package software.wings.beans;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonView;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
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
  @Attributes(title = "Master URL", required = true) @NotEmpty private String masterUrl;
  @Attributes(title = "User Name") private String username;
  @Encrypted @Attributes(title = "Password") private char[] password;
  @Encrypted @Attributes(title = "CA Certificate") private char[] caCert;
  @Encrypted @Attributes(title = "Client Certificate") private char[] clientCert;
  @Encrypted @Attributes(title = "Client Key") private char[] clientKey;
  @Encrypted @Attributes(title = "Client Key Passphrase") private char[] clientKeyPassphrase;
  @Attributes(title = "Client Key Algorithm") private String clientKeyAlgo;

  @JsonView(JsonViews.Internal.class) @SchemaIgnore private String encryptedPassword;
  @JsonView(JsonViews.Internal.class) @SchemaIgnore private String encryptedCaCert;
  @JsonView(JsonViews.Internal.class) @SchemaIgnore private String encryptedClientCert;
  @JsonView(JsonViews.Internal.class) @SchemaIgnore private String encryptedClientKey;
  @JsonView(JsonViews.Internal.class) @SchemaIgnore private String encryptedClientKeyPassphrase;

  @SchemaIgnore @NotEmpty private String accountId;

  @SchemaIgnore @Transient private boolean decrypted;

  public KubernetesClusterConfig() {
    super(SettingVariableTypes.KUBERNETES_CLUSTER.name());
  }

  public KubernetesClusterConfig(String masterUrl, String username, char[] password, char[] caCert, char[] clientCert,
      char[] clientKey, char[] clientKeyPassphrase, String clientKeyAlgo, String encryptedPassword,
      String encryptedCaCert, String encryptedClientCert, String encryptedClientKey,
      String encryptedClientKeyPassphrase, String accountId, boolean decrypted) {
    this();
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

  @SchemaIgnore
  public KubernetesConfig createKubernetesConfig(String namespace) {
    KubernetesConfigBuilder kubernetesConfig = KubernetesConfig.builder()
                                                   .accountId(getAccountId())
                                                   .masterUrl(masterUrl)
                                                   .username(username)
                                                   .clientKeyAlgo(clientKeyAlgo)
                                                   .namespace(isNotBlank(namespace) ? namespace : "default");
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
  public static class Yaml extends CloudProviderYaml {
    private String masterUrl;
    private String username;
    private String password;
    private String caCert;
    private String clientCert;
    private String clientKey;
    private String clientKeyPassphrase;
    private String clientKeyAlgo;

    @lombok.Builder
    public Yaml(String type, String harnessApiVersion, String masterUrl, String username, String password,
        String caCert, String clientCert, String clientKey, String clientKeyPassphrase, String clientKeyAlgo,
        UsageRestrictions usageRestrictions) {
      super(type, harnessApiVersion, usageRestrictions);
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
