package software.wings.helpers.ext.mail;

import static software.wings.yaml.YamlHelper.ENCRYPTED_VALUE_STR;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonView;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.annotation.Encryptable;
import software.wings.annotation.Encrypted;
import software.wings.jersey.JsonViews;
import software.wings.settings.SettingValue;
import software.wings.stencils.DefaultValue;
import software.wings.yaml.setting.CollaborationProviderYaml;

/**
 * Created by peeyushaggarwal on 5/20/16.
 */
@JsonTypeName("SMTP")
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
public class SmtpConfig extends SettingValue implements Encryptable {
  @Attributes(title = "Host", required = true) @NotEmpty private String host;
  @Attributes(title = "Port", required = true) private int port;
  @DefaultValue("wings") @Attributes(title = "From Address") private String fromAddress;
  @DefaultValue("true") @Attributes(title = "SSL") private boolean useSSL;
  @Attributes(title = "Username") private String username;
  @JsonView(JsonViews.Internal.class) @Attributes(title = "Password") @Encrypted private char[] password;
  @SchemaIgnore @NotEmpty private String accountId;

  @SchemaIgnore private String encryptedPassword;

  /**
   * Instantiates a new smtp config.
   */
  public SmtpConfig() {
    super(SettingVariableTypes.SMTP.name());
  }

  public SmtpConfig(String host, int port, String fromAddress, boolean useSSL, String username, char[] password,
      String accountId, String encryptedPassword) {
    this();
    this.host = host;
    this.port = port;
    this.fromAddress = fromAddress;
    this.useSSL = useSSL;
    this.username = username;
    this.password = password;
    this.accountId = accountId;
    this.encryptedPassword = encryptedPassword;
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @NoArgsConstructor
  public static final class Yaml extends CollaborationProviderYaml {
    private String host;
    private int port;
    private String fromAddress;
    private boolean useSSL;
    private String username;
    private String password = ENCRYPTED_VALUE_STR;

    @Builder
    public Yaml(String type, String harnessApiVersion, String host, int port, String fromAddress, boolean useSSL,
        String username, String password) {
      super(type, harnessApiVersion);
      this.host = host;
      this.port = port;
      this.fromAddress = fromAddress;
      this.useSSL = useSSL;
      this.username = username;
      this.password = password;
    }
  }
}
