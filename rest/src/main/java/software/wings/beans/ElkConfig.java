package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonView;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import lombok.Data;
import lombok.ToString;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.jersey.JsonViews;
import software.wings.security.annotations.Encrypted;
import software.wings.security.encryption.Encryptable;
import software.wings.settings.SettingValue;

/**
 * The type ELK config.
 */
@JsonTypeName("ELK")
@Data
@ToString(exclude = "password")
public class ElkConfig extends SettingValue implements Encryptable {
  @Attributes(title = "Elk Url", required = true) @NotEmpty private String elkUrl;
  @Attributes(title = "Username", required = true) @NotEmpty private String username;
  @JsonView(JsonViews.Internal.class)
  @Attributes(title = "Password", required = true)
  @NotEmpty
  @Encrypted
  private char[] password;
  @SchemaIgnore @NotEmpty private String accountId;

  /**
   * Instantiates a new Splunk config.
   */
  public ElkConfig() {
    super(SettingVariableTypes.ELK.name());
  }
}
