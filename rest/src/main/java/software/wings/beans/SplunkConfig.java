package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonView;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.annotation.Encryptable;
import software.wings.annotation.Encrypted;
import software.wings.jersey.JsonViews;
import software.wings.settings.SettingValue;
import software.wings.yaml.setting.VerificationProviderYaml;

/**
 * The type Splunk config.
 */
@JsonTypeName("SPLUNK")
@Data
@ToString(exclude = "password")
@Builder
public class SplunkConfig extends SettingValue implements Encryptable {
  @Attributes(title = "URL", required = true) @NotEmpty private String splunkUrl;

  @NotEmpty @Attributes(title = "User Name", required = true) private String username;

  @JsonView(JsonViews.Internal.class)
  @Attributes(title = "Password", required = true)
  @NotEmpty
  @Encrypted
  private char[] password;

  @SchemaIgnore @NotEmpty private String accountId;

  @SchemaIgnore private String encryptedPassword;

  /**
   * Instantiates a new Splunk config.
   */
  public SplunkConfig() {
    super(SettingVariableTypes.SPLUNK.name());
  }

  public SplunkConfig(String splunkUrl, String username, char[] password, String accountId, String encryptedPassword) {
    this();
    this.splunkUrl = splunkUrl;
    this.username = username;
    this.password = password;
    this.accountId = accountId;
    this.encryptedPassword = encryptedPassword;
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  public static final class Yaml extends VerificationProviderYaml {
    private String splunkUrl;
    private String username;
    private String password;

    public Yaml() {}

    public Yaml(String type, String name, String splunkUrl, String username, String password) {
      super(type, name);
      this.splunkUrl = splunkUrl;
      this.username = username;
      this.password = password;
    }
  }
}
