package software.wings.beans.config;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonView;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import lombok.Data;
import lombok.ToString;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.jersey.JsonViews;
import software.wings.annotation.Encrypted;
import software.wings.annotation.Encryptable;
import software.wings.settings.SettingValue;

/**
 * Created by rsingh on 8/21/17.
 */
@JsonTypeName("LOGZ")
@Data
@ToString(exclude = "token")
public class LogzConfig extends SettingValue implements Encryptable {
  @Attributes(title = "Logz.io URL", required = true) @NotEmpty private String logzUrl;

  @JsonView(JsonViews.Internal.class)
  @Attributes(title = "Token", required = true)
  @NotEmpty
  @Encrypted
  private char[] token;

  @SchemaIgnore @NotEmpty private String accountId;

  @SchemaIgnore private String encryptedToken;
  /**
   * Instantiates a new Splunk config.
   */
  public LogzConfig() {
    super(SettingVariableTypes.LOGZ.name());
  }

  public String getLogzUrl() {
    return logzUrl;
  }

  public void setLogzUrl(String logzUrl) {
    this.logzUrl = logzUrl;
  }

  public char[] getToken() {
    return token;
  }

  public void setToken(char[] token) {
    this.token = token;
  }

  public String getAccountId() {
    return accountId;
  }

  public void setAccountId(String accountId) {
    this.accountId = accountId;
  }
}
