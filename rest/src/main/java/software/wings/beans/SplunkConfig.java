package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonView;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.jersey.JsonViews;
import software.wings.security.annotations.Encrypted;
import software.wings.security.encryption.Encryptable;
import software.wings.settings.SettingValue;

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

  /**
   * Instantiates a new Splunk config.
   */
  public SplunkConfig() {
    super(SettingVariableTypes.SPLUNK.name());
  }

  public SplunkConfig(String splunkUrl, String username, char[] password, String accountId) {
    super(SettingVariableTypes.SPLUNK.name());
    this.splunkUrl = splunkUrl;
    this.username = username;
    this.password = password;
    this.accountId = accountId;
  }
}
