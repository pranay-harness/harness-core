package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonView;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.validator.constraints.NotEmpty;
import ro.fortsoft.pf4j.Extension;
import software.wings.annotation.Encryptable;
import software.wings.annotation.Encrypted;
import software.wings.jersey.JsonViews;
import software.wings.settings.SettingValue;
import software.wings.settings.UsageRestrictions;
import software.wings.sm.StateType;
import software.wings.yaml.setting.VerificationProviderYaml;

/**
 * Created by anubhaw on 8/4/16.
 */
@Extension
@JsonTypeName("APP_DYNAMICS")
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@ToString(exclude = "password")
public class AppDynamicsConfig extends SettingValue implements Encryptable {
  @Attributes(title = "User Name", required = true) @NotEmpty private String username;
  @Attributes(title = "Account Name", required = true) @NotEmpty private String accountname;
  @Attributes(title = "Password", required = true)

  @Encrypted
  private char[] password;
  @Attributes(title = "Controller URL", required = true) @NotEmpty private String controllerUrl;
  @SchemaIgnore @NotEmpty private String accountId;

  @JsonView(JsonViews.Internal.class) @SchemaIgnore private String encryptedPassword;

  /**
   * Instantiates a new App dynamics config.
   */
  public AppDynamicsConfig() {
    super(StateType.APP_DYNAMICS.name());
  }

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public AppDynamicsConfig(String username, String accountname, char[] password, String controllerUrl, String accountId,
      String encryptedPassword) {
    this();
    this.username = username;
    this.accountname = accountname;
    this.password = password;
    this.controllerUrl = controllerUrl;
    this.accountId = accountId;
    this.encryptedPassword = encryptedPassword;
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @NoArgsConstructor
  public static final class Yaml extends VerificationProviderYaml {
    private String username;
    private String password;
    private String accountName;
    private String controllerUrl;

    @Builder
    public Yaml(String type, String harnessApiVersion, String username, String password, String accountName,
        String controllerUrl, UsageRestrictions usageRestrictions) {
      super(type, harnessApiVersion, usageRestrictions);
      this.username = username;
      this.password = password;
      this.accountName = accountName;
      this.controllerUrl = controllerUrl;
    }
  }
}
