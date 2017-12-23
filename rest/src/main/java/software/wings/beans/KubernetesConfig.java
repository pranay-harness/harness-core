package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.SchemaIgnore;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.annotation.Encryptable;
import software.wings.annotation.Encrypted;
import software.wings.settings.SettingValue;

/**
 * Created by bzane on 2/28/17
 */
@JsonTypeName("KUBERNETES")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@ToString(exclude = "password")
public class KubernetesConfig extends SettingValue implements Encryptable {
  @NotEmpty private String masterUrl;
  private String username;
  @Encrypted private char[] password;
  @Encrypted private char[] caCert;
  @Encrypted private char[] clientCert;
  @Encrypted private char[] clientKey;
  private String namespace;
  @NotEmpty @SchemaIgnore private String accountId;

  @SchemaIgnore private String encryptedPassword;
  @SchemaIgnore private String encryptedCaCert;
  @SchemaIgnore private String encryptedClientCert;
  @SchemaIgnore private String encryptedClientKey;

  /**
   * Instantiates a new setting value.
   */
  public KubernetesConfig() {
    super(SettingVariableTypes.KUBERNETES.name());
  }

  public KubernetesConfig(String masterUrl, String username, char[] password, char[] caCert, char[] clientCert,
      char[] clientKey, String namespace, String accountId, String encryptedPassword, String encryptedCaCert,
      String encryptedClientCert, String encryptedClientKey) {
    this();
    this.masterUrl = masterUrl;
    this.username = username;
    this.password = password;
    this.caCert = caCert;
    this.clientCert = clientCert;
    this.clientKey = clientKey;
    this.namespace = namespace;
    this.accountId = accountId;
    this.encryptedPassword = encryptedPassword;
    this.encryptedCaCert = encryptedCaCert;
    this.encryptedClientCert = encryptedClientCert;
    this.encryptedClientKey = encryptedClientKey;
  }
}
