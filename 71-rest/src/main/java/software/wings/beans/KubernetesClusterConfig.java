package software.wings.beans;

import static io.fabric8.kubernetes.client.Config.KUBERNETES_SERVICE_ACCOUNT_CA_CRT_PATH;
import static io.fabric8.kubernetes.client.Config.KUBERNETES_SERVICE_ACCOUNT_TOKEN_PATH;
import static io.harness.govern.Switch.noop;
import static io.harness.network.Http.joinHostPort;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonView;
import com.github.reinert.jjschema.SchemaIgnore;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.fabric8.kubernetes.client.utils.Utils;
import io.harness.encryption.Encrypted;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Transient;
import software.wings.annotation.EncryptableSetting;
import software.wings.beans.KubernetesConfig.KubernetesConfigBuilder;
import software.wings.jersey.JsonViews;
import software.wings.settings.SettingValue;
import software.wings.settings.UsageRestrictions;
import software.wings.yaml.setting.CloudProviderYaml;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

@JsonTypeName("KUBERNETES_CLUSTER")
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
public class KubernetesClusterConfig extends SettingValue implements EncryptableSetting {
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

  @Transient private boolean decrypted;

  public KubernetesClusterConfig() {
    super(SettingVariableTypes.KUBERNETES_CLUSTER.name());
  }

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public KubernetesClusterConfig(boolean useKubernetesDelegate, String delegateName, String masterUrl, String username,
      char[] password, char[] caCert, char[] clientCert, char[] clientKey, char[] clientKeyPassphrase,
      char[] serviceAccountToken, String clientKeyAlgo, boolean skipValidation, String encryptedPassword,
      String encryptedCaCert, String encryptedClientCert, String encryptedClientKey,
      String encryptedClientKeyPassphrase, String encryptedServiceAccountToken, String accountId, boolean decrypted) {
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
    this.serviceAccountToken = serviceAccountToken;
    this.clientKeyAlgo = clientKeyAlgo;
    this.skipValidation = skipValidation;
    this.encryptedPassword = encryptedPassword;
    this.encryptedCaCert = encryptedCaCert;
    this.encryptedClientCert = encryptedClientCert;
    this.encryptedClientKey = encryptedClientKey;
    this.encryptedClientKeyPassphrase = encryptedClientKeyPassphrase;
    this.encryptedServiceAccountToken = encryptedServiceAccountToken;
    this.accountId = accountId;
    this.decrypted = decrypted;
  }

  @SchemaIgnore
  @Override
  public boolean isDecrypted() {
    return decrypted || isNotBlank(delegateName);
  }

  @SuppressFBWarnings("DMI_HARDCODED_ABSOLUTE_FILENAME")
  private static KubernetesConfig getKubernetesConfigFromServiceAccount(String namespace) {
    KubernetesConfigBuilder kubernetesConfigBuilder = KubernetesConfig.builder().namespace(namespace);

    String masterHost = Utils.getSystemPropertyOrEnvVar("KUBERNETES_SERVICE_HOST", (String) null);
    String masterPort = Utils.getSystemPropertyOrEnvVar("KUBERNETES_SERVICE_PORT", (String) null);
    if (masterHost != null && masterPort != null) {
      String hostPort = joinHostPort(masterHost, masterPort);
      kubernetesConfigBuilder.masterUrl("https://" + hostPort);
    }

    String caCert = getFileContent(KUBERNETES_SERVICE_ACCOUNT_CA_CRT_PATH);
    if (isNotBlank(caCert)) {
      kubernetesConfigBuilder.caCert(caCert.toCharArray());
    }

    String serviceAccountToken = getFileContent(KUBERNETES_SERVICE_ACCOUNT_TOKEN_PATH);
    if (isNotBlank(serviceAccountToken)) {
      kubernetesConfigBuilder.serviceAccountToken(serviceAccountToken.toCharArray());
    }

    return kubernetesConfigBuilder.build();
  }

  private static String getFileContent(String filename) {
    try {
      if (Files.isRegularFile(new File(filename).toPath())) {
        return new String(Files.readAllBytes(new File(filename).toPath()), StandardCharsets.UTF_8);
      }
    } catch (IOException e) {
      noop(); // Ignore
    }
    return "";
  }

  public KubernetesConfig createKubernetesConfig(String namespace) {
    String namespaceNotBlank = isNotBlank(namespace) ? namespace : "default";

    if (isUseKubernetesDelegate()) {
      return getKubernetesConfigFromServiceAccount(namespaceNotBlank);
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

    @lombok.Builder
    public Yaml(boolean useKubernetesDelegate, String delegateName, String type, String harnessApiVersion,
        String masterUrl, String username, String password, String caCert, String clientCert, String clientKey,
        String clientKeyPassphrase, String serviceAccountToken, String clientKeyAlgo, boolean skipValidation,
        UsageRestrictions.Yaml usageRestrictions) {
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
    }
  }
}
