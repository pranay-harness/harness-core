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
import software.wings.annotation.Encrypted;
import software.wings.security.encryption.Encryptable;
import software.wings.settings.SettingValue;

/**
 * Created by anubhaw on 11/22/16.
 */
@JsonTypeName("BAMBOO")
@Data
@Builder
@ToString(exclude = "password")
public class BambooConfig extends SettingValue implements Encryptable {
  @Attributes(title = "Bamboo URL", required = true) @NotEmpty private String bambooUrl;
  @Attributes(title = "Username", required = true) @NotEmpty private String username;
  @JsonView(JsonViews.Internal.class)
  @Attributes(title = "Password", required = true)
  @NotEmpty
  @Encrypted
  private char[] password;
  @SchemaIgnore @NotEmpty private String accountId;

  @SchemaIgnore private String encryptedPassword;

  /**
   * Instantiates a new BambooService config.
   */
  public BambooConfig() {
    super(SettingVariableTypes.BAMBOO.name());
  }

  public BambooConfig(String bambooUrl, String username, char[] password, String accountId, String encryptedPassword) {
    this();
    this.bambooUrl = bambooUrl;
    this.username = username;
    this.password = password;
    this.accountId = accountId;
    this.encryptedPassword = encryptedPassword;
  }
}
