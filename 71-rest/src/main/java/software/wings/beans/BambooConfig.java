package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonView;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.harness.annotation.Encrypted;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.annotation.EncryptableSetting;
import software.wings.beans.config.ArtifactSourceable;
import software.wings.jersey.JsonViews;
import software.wings.settings.SettingValue;
import software.wings.settings.UsageRestrictions;
import software.wings.yaml.setting.ArtifactServerYaml;

/**
 * Created by anubhaw on 11/22/16.
 */
@JsonTypeName("BAMBOO")
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@ToString(exclude = "password")
public class BambooConfig extends SettingValue implements EncryptableSetting, ArtifactSourceable {
  @Attributes(title = "Bamboo URL", required = true) @NotEmpty private String bambooUrl;
  @Attributes(title = "Username", required = true) @NotEmpty private String username;
  @Attributes(title = "Password", required = true) @Encrypted private char[] password;
  @SchemaIgnore @NotEmpty private String accountId;

  @JsonView(JsonViews.Internal.class) @SchemaIgnore private String encryptedPassword;

  /**
   * Instantiates a new BambooService config.
   */
  public BambooConfig() {
    super(SettingVariableTypes.BAMBOO.name());
  }

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public BambooConfig(String bambooUrl, String username, char[] password, String accountId, String encryptedPassword) {
    this();
    this.bambooUrl = bambooUrl;
    this.username = username;
    this.password = password;
    this.accountId = accountId;
    this.encryptedPassword = encryptedPassword;
  }

  @Override
  public String fetchUserName() {
    return username;
  }

  @Override
  public String fetchRegistryUrl() {
    return bambooUrl;
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @NoArgsConstructor
  public static final class Yaml extends ArtifactServerYaml {
    @Builder
    public Yaml(String type, String harnessApiVersion, String url, String username, String password,
        UsageRestrictions usageRestrictions) {
      super(type, harnessApiVersion, url, username, password, usageRestrictions);
    }
  }
}
