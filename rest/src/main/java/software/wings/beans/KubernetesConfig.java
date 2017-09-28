package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.security.annotations.Encrypted;
import software.wings.security.encryption.Encryptable;
import software.wings.settings.SettingValue;

/**
 * Created by bzane on 2/28/17
 */
@JsonTypeName("KUBERNETES")
@Data
@EqualsAndHashCode(callSuper = true)
public class KubernetesConfig extends SettingValue implements Encryptable {
  @Attributes(title = "Cluster master URL", required = true) @NotEmpty private String masterUrl;
  @Attributes(title = "Username", required = true) @NotEmpty private String username;
  @Attributes(title = "Password", required = true) @NotEmpty @Encrypted private char[] password;
  private String caCert;
  private String clientCert;
  private String clientKey;
  private String namespace;
  @NotEmpty @SchemaIgnore private String accountId;

  /**
   * Instantiates a new setting value.
   */
  public KubernetesConfig() {
    super(SettingVariableTypes.KUBERNETES.name());
  }

  public static final class KubernetesConfigBuilder {
    private String masterUrl;
    private String username;
    private char[] password;
    private String caCert;
    private String clientCert;
    private String clientKey;
    private String namespace;
    private String accountId;

    private KubernetesConfigBuilder() {}

    public static KubernetesConfigBuilder aKubernetesConfig() {
      return new KubernetesConfigBuilder();
    }

    public KubernetesConfigBuilder withMasterUrl(String masterUrl) {
      this.masterUrl = masterUrl;
      return this;
    }

    public KubernetesConfigBuilder withUsername(String username) {
      this.username = username;
      return this;
    }

    public KubernetesConfigBuilder withPassword(char[] password) {
      this.password = password;
      return this;
    }

    public KubernetesConfigBuilder withCaCert(String caCert) {
      this.caCert = caCert;
      return this;
    }

    public KubernetesConfigBuilder withClientCert(String clientCert) {
      this.clientCert = clientCert;
      return this;
    }

    public KubernetesConfigBuilder withClientKey(String clientKey) {
      this.clientKey = clientKey;
      return this;
    }

    public KubernetesConfigBuilder withNamespace(String namespace) {
      this.namespace = namespace;
      return this;
    }

    public KubernetesConfigBuilder withAccountId(String accountId) {
      this.accountId = accountId;
      return this;
    }

    public KubernetesConfigBuilder but() {
      return aKubernetesConfig()
          .withMasterUrl(masterUrl)
          .withUsername(username)
          .withPassword(password)
          .withCaCert(caCert)
          .withClientCert(clientCert)
          .withClientKey(clientKey)
          .withNamespace(namespace)
          .withAccountId(accountId);
    }

    public KubernetesConfig build() {
      KubernetesConfig kubernetesConfig = new KubernetesConfig();
      kubernetesConfig.setMasterUrl(masterUrl);
      kubernetesConfig.setUsername(username);
      kubernetesConfig.setPassword(password);
      kubernetesConfig.setCaCert(caCert);
      kubernetesConfig.setClientCert(clientCert);
      kubernetesConfig.setClientKey(clientKey);
      kubernetesConfig.setNamespace(namespace);
      kubernetesConfig.setAccountId(accountId);
      return kubernetesConfig;
    }
  }
}
