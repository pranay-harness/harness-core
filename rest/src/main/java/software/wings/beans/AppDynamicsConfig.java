package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonView;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;
import org.hibernate.validator.constraints.NotEmpty;
import ro.fortsoft.pf4j.Extension;
import software.wings.jersey.JsonViews;
import software.wings.security.annotations.Encrypted;
import software.wings.security.encryption.Encryptable;
import software.wings.settings.SettingValue;
import software.wings.sm.StateType;

/**
 * Created by anubhaw on 8/4/16.
 */
@Extension
@JsonTypeName("APP_DYNAMICS")
@Data
@Builder
@ToString(exclude = "password")
public class AppDynamicsConfig extends SettingValue implements Encryptable {
  @Attributes(title = "User Name", required = true) @NotEmpty private String username;
  @Attributes(title = "Account Name", required = true) @NotEmpty private String accountname;
  @JsonView(JsonViews.Internal.class)
  @Attributes(title = "Password", required = true)
  @NotEmpty
  @Encrypted
  private char[] password;
  @Attributes(title = "Controller URL", required = true) @NotEmpty private String controllerUrl;
  @SchemaIgnore @NotEmpty private String accountId;

  @SchemaIgnore private String encryptedPassword;

  /**
   * Instantiates a new App dynamics config.
   */
  public AppDynamicsConfig() {
    super(StateType.APP_DYNAMICS.name());
  }

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
}
