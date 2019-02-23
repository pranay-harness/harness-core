package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonView;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.harness.encryption.Encrypted;
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
import software.wings.yaml.setting.ArtifactServerYaml;

@JsonTypeName("SFTP")
@Data
@Builder
@ToString(exclude = {"password"})
@EqualsAndHashCode(callSuper = false)
public class SftpConfig extends SettingValue implements EncryptableSetting {
  @Attributes(title = "SFTP URL", required = true) @NotEmpty private String sftpUrl;
  @Attributes(title = "Domain") private String domain;
  @Attributes(title = "Username") private String username;
  @Attributes(title = "Password") @Encrypted private char[] password;
  @SchemaIgnore @NotEmpty private String accountId;
  @JsonView(JsonViews.Internal.class) @SchemaIgnore private String encryptedPassword;

  public SftpConfig() {
    super(SettingVariableTypes.SFTP.name());
  }

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public SftpConfig(
      String sftpUrl, String domain, String username, char[] password, String accountId, String encryptedPassword) {
    this();
    this.sftpUrl = sftpUrl;
    this.domain = domain;
    this.username = username;
    this.password = password;
    this.accountId = accountId;
    this.encryptedPassword = encryptedPassword;
  }

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  public static final class Yaml extends ArtifactServerYaml {
    String domain;
    @Builder
    public Yaml(String type, String harnessApiVersion, String url, String domain, String username, String password,
        UsageRestrictions.Yaml usageRestrictions) {
      super(type, harnessApiVersion, url, username, password, usageRestrictions);
      this.domain = domain;
    }
  }
}