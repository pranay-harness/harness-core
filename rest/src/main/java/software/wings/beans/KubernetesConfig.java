package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.annotation.Encrypted;
import software.wings.annotation.Encryptable;
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
  @Attributes(title = "Cluster master URL", required = true) @NotEmpty private String masterUrl;
  @Attributes(title = "Username", required = true) @NotEmpty private String username;
  @Attributes(title = "Password", required = true) @Encrypted private char[] password;
  private String caCert;
  private String clientCert;
  private String clientKey;
  private String namespace;
  @NotEmpty @SchemaIgnore private String accountId;

  @SchemaIgnore private String encryptedPassword;

  /**
   * Instantiates a new setting value.
   */
  public KubernetesConfig() {
    super(SettingVariableTypes.KUBERNETES.name());
  }

  public KubernetesConfig(String masterUrl, String username, char[] password, String caCert, String clientCert,
      String clientKey, String namespace, String accountId, String encryptedPassword) {
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
  }
}
