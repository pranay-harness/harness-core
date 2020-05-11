package software.wings.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static java.util.Collections.singletonList;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonView;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.encryption.Encrypted;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.annotation.EncryptableSetting;
import software.wings.audit.ResourceType;
import software.wings.delegatetasks.validation.capabilities.SftpCapability;
import software.wings.jersey.JsonViews;
import software.wings.settings.SettingValue;
import software.wings.settings.UsageRestrictions;
import software.wings.yaml.setting.ArtifactServerYaml;

import java.util.List;

@OwnedBy(CDC)
@JsonTypeName("SFTP")
@Data
@Builder
@ToString(exclude = {"password"})
@EqualsAndHashCode(callSuper = false)
public class SftpConfig extends SettingValue implements EncryptableSetting {
  @Attributes(title = "SFTP URL", required = true) @NotEmpty private String sftpUrl;
  @Attributes(title = "Domain") private String domain;
  @Attributes(title = "Username") private String username;
  @Attributes(title = "Password") @Encrypted(fieldName = "password") private char[] password;
  @SchemaIgnore @NotEmpty private String accountId;
  @JsonView(JsonViews.Internal.class) @SchemaIgnore private String encryptedPassword;

  public SftpConfig() {
    super(SettingVariableTypes.SFTP.name());
  }

  public SftpConfig(
      String sftpUrl, String domain, String username, char[] password, String accountId, String encryptedPassword) {
    this();
    this.sftpUrl = sftpUrl;
    this.domain = domain;
    this.username = username;
    this.password = password == null ? null : password.clone();
    this.accountId = accountId;
    this.encryptedPassword = encryptedPassword;
  }

  @Override
  public String fetchResourceCategory() {
    return ResourceType.ARTIFACT_SERVER.name();
  }

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    return singletonList(SftpCapability.builder().sftpUrl(getSftpUrl()).build());
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
