package software.wings.helpers.ext.mail;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static software.wings.audit.ResourceType.COLLABORATION_PROVIDER;
import static software.wings.yaml.YamlHelper.ENCRYPTED_VALUE_STR;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonView;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.task.mixin.SocketConnectivityCapabilityGenerator;
import io.harness.encryption.Encrypted;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.annotation.EncryptableSetting;
import software.wings.jersey.JsonViews;
import software.wings.settings.SettingValue;
import software.wings.settings.UsageRestrictions;
import software.wings.stencils.DefaultValue;
import software.wings.yaml.setting.CollaborationProviderYaml;

import java.util.Collections;
import java.util.List;

/**
 * Created by peeyushaggarwal on 5/20/16.
 */
@JsonTypeName("SMTP")
@Data
@Builder
@EqualsAndHashCode(callSuper = false)
public class SmtpConfig extends SettingValue implements EncryptableSetting {
  private static final String HOST_PLACEHOLDER_STRING = "host_placeholder";
  @Attributes(title = "Host", required = true) @NotEmpty private String host;
  @Attributes(title = "Port", required = true) private int port;
  @DefaultValue("wings") @Attributes(title = "From Address") private String fromAddress;
  @DefaultValue("true") @Attributes(title = "SSL") private boolean useSSL;
  @DefaultValue("false") @Attributes(title = "Start TLS") private boolean startTLS;
  @Attributes(title = "Username") private String username;
  @Attributes(title = "Password") @Encrypted(fieldName = "password") private char[] password;
  @SchemaIgnore @NotEmpty private String accountId;

  @JsonView(JsonViews.Internal.class) @SchemaIgnore private String encryptedPassword;

  /**
   * Instantiates a new smtp config.
   */
  public SmtpConfig() {
    super(SettingVariableTypes.SMTP.name());
  }

  public SmtpConfig(String host, int port, String fromAddress, boolean useSSL, boolean startTLS, String username,
      char[] password, String accountId, String encryptedPassword) {
    this();
    this.host = host;
    this.port = port;
    this.fromAddress = fromAddress;
    this.useSSL = useSSL;
    this.startTLS = startTLS;
    this.username = username;
    this.password = password == null ? null : password.clone();
    this.accountId = accountId;
    this.encryptedPassword = encryptedPassword;
  }

  public boolean valid() {
    return isNotEmpty(host) && !HOST_PLACEHOLDER_STRING.equals(host);
  }

  @Override
  public String fetchResourceCategory() {
    return COLLABORATION_PROVIDER.name();
  }

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    return Collections.singletonList(
        SocketConnectivityCapabilityGenerator.buildSocketConnectivityCapability(host, String.valueOf(port)));
  }

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  public static final class Yaml extends CollaborationProviderYaml {
    private String host;
    private int port;
    private String fromAddress;
    private boolean useSSL;
    private String username;
    private String password = ENCRYPTED_VALUE_STR;

    @Builder
    public Yaml(String type, String harnessApiVersion, String host, int port, String fromAddress, boolean useSSL,
        String username, String password, UsageRestrictions.Yaml usageRestrictions) {
      super(type, harnessApiVersion, usageRestrictions);
      this.host = host;
      this.port = port;
      this.fromAddress = fromAddress;
      this.useSSL = useSSL;
      this.username = username;
      this.password = password;
    }
  }
}
