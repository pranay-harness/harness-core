package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonTypeName;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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
@ToString(exclude = {"password", "caCert", "clientCert", "clientKey", "clientKeyPassphrase", "serviceAccountToken"})
public class KubernetesConfig extends SettingValue implements Encryptable {
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

  /**
   * Instantiates a new setting value.
   */
  public KubernetesConfig() {
    super(SettingVariableTypes.KUBERNETES.name());
  }

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public KubernetesConfig(String masterUrl, String username, char[] password, char[] caCert, char[] clientCert,
      char[] clientKey, char[] clientKeyPassphrase, char[] serviceAccountToken, String clientKeyAlgo, String namespace,
      String accountId, String encryptedPassword, String encryptedCaCert, String encryptedClientCert,
      String encryptedClientKey, String encryptedClientKeyPassphrase, String encryptedServiceAccountToken) {
    this();
    this.masterUrl = masterUrl;
    this.username = username;
    this.password = password;
    this.caCert = caCert;
    this.clientCert = clientCert;
    this.clientKey = clientKey;
    this.clientKeyPassphrase = clientKeyPassphrase;
    this.serviceAccountToken = serviceAccountToken;
    this.clientKeyAlgo = clientKeyAlgo;
    this.namespace = namespace;
    this.accountId = accountId;
    this.encryptedPassword = encryptedPassword;
    this.encryptedCaCert = encryptedCaCert;
    this.encryptedClientCert = encryptedClientCert;
    this.encryptedClientKey = encryptedClientKey;
    this.encryptedClientKeyPassphrase = encryptedClientKeyPassphrase;
    this.encryptedServiceAccountToken = encryptedServiceAccountToken;
  }
}
