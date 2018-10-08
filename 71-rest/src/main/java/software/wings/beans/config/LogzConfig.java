package software.wings.beans.config;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonView;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.annotation.Encrypted;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.annotation.EncryptableSetting;
import software.wings.jersey.JsonViews;
import software.wings.settings.SettingValue;
import software.wings.settings.UsageRestrictions;
import software.wings.sm.StateType;
import software.wings.yaml.setting.VerificationProviderYaml;

/**
 * Created by rsingh on 8/21/17.
 */
@JsonTypeName("LOGZ")
@Data
@EqualsAndHashCode(callSuper = false)
@ToString(exclude = "token")
@Builder
public class LogzConfig extends SettingValue implements EncryptableSetting {
  @Attributes(title = "Logz.io URL", required = true) @NotEmpty private String logzUrl;

  @Attributes(title = "Token", required = true) @Encrypted private char[] token;

  @SchemaIgnore @NotEmpty private String accountId;

  @JsonView(JsonViews.Internal.class) @SchemaIgnore private String encryptedToken;
  /**
   * Instantiates a new Splunk config.
   */
  public LogzConfig() {
    super(StateType.LOGZ.name());
  }

  private LogzConfig(String logzUrl, char[] token, String accountId, String encryptedToken) {
    this();
    this.logzUrl = logzUrl;
    this.token = token;
    this.accountId = accountId;
    this.encryptedToken = encryptedToken;
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @NoArgsConstructor
  public static final class Yaml extends VerificationProviderYaml {
    private String logzUrl;
    private String token;

    @Builder
    public Yaml(
        String type, String harnessApiVersion, String logzUrl, String token, UsageRestrictions usageRestrictions) {
      super(type, harnessApiVersion, usageRestrictions);
      this.logzUrl = logzUrl;
      this.token = token;
    }
  }
}
