package software.wings.beans.config;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonView;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.annotation.Encryptable;
import software.wings.annotation.Encrypted;
import software.wings.jersey.JsonViews;
import software.wings.settings.SettingValue;
import software.wings.yaml.setting.VerificationProviderYaml;

/**
 * Created by rsingh on 8/21/17.
 */
@JsonTypeName("LOGZ")
@Data
@EqualsAndHashCode(callSuper = false)
@ToString(exclude = "token")
public class LogzConfig extends SettingValue implements Encryptable {
  @Attributes(title = "Logz.io URL", required = true) @NotEmpty private String logzUrl;

  @JsonView(JsonViews.Internal.class) @Attributes(title = "Token", required = true) @Encrypted private char[] token;

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

  @Data
  @EqualsAndHashCode(callSuper = true)
  @NoArgsConstructor
  public static final class Yaml extends VerificationProviderYaml {
    private String logzUrl;
    private String token;

    @Builder
    public Yaml(String type, String harnessApiVersion, String logzUrl, String token) {
      super(type, harnessApiVersion);
      this.logzUrl = logzUrl;
      this.token = token;
    }
  }
}
