package software.wings.beans.config;

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
 * Created by rsingh on 8/21/17.
 */
@JsonTypeName("LOGZ")
@Data
@ToString(exclude = "token")
public class LogzConfig extends SettingValue implements Encryptable {
  @Attributes(title = "Url", required = true) @NotEmpty private String url;

  @JsonView(JsonViews.Internal.class)
  @Attributes(title = "Token", required = true)
  @NotEmpty
  @Encrypted
  private char[] token;

  @SchemaIgnore @NotEmpty private String accountId;

  /**
   * Instantiates a new Splunk config.
   */
  public LogzConfig() {
    super(SettingVariableTypes.LOGZ.name());
  }
}
