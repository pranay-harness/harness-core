package software.wings.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonView;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.task.mixin.SSHConnectionExecutionCapabilityGenerator;
import io.harness.encryption.Encrypted;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.annotation.EncryptableSetting;
import software.wings.audit.ResourceType;
import software.wings.jersey.JsonViews;
import software.wings.settings.SettingValue;
import software.wings.settings.UsageRestrictions;
import software.wings.yaml.setting.ArtifactServerYaml;

import java.util.Collections;
import java.util.List;

@OwnedBy(CDC)
@JsonTypeName("SMB")
@Data
@Builder
@ToString(exclude = {"password"})
@EqualsAndHashCode(callSuper = false)
public class SmbConfig extends SettingValue implements EncryptableSetting {
  @Attributes(title = "SMB URL", required = true) @NotEmpty private String smbUrl;
  @Attributes(title = "Domain") private String domain;
  @Attributes(title = "Username") private String username;
  @Attributes(title = "Password") @Encrypted(fieldName = "password") private char[] password;
  @SchemaIgnore @NotEmpty private String accountId;
  @JsonView(JsonViews.Internal.class) @SchemaIgnore private String encryptedPassword;

  public SmbConfig() {
    super(SettingVariableTypes.SMB.name());
  }

  public SmbConfig(
      String smbUrl, String domain, String username, char[] password, String accountId, String encryptedPassword) {
    this();
    this.smbUrl = smbUrl;
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

  /*
    Scheme syntax:
    smb://[<user>@]<host>[:<port>][/[<path>]][?<param1>=<value1>[;<param2>=<value2>]] or
    smb://[<user>@]<workgroup>[:<port>][/] or s
    mb://[[<domain>;]<username>[:<password>]@]<server>[:<port>][/[<share>[/[<path>]]][?[<param>=<value>[<param2>=<value2>[...]]]]]
   */
  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    return Collections.singletonList(
        SSHConnectionExecutionCapabilityGenerator.buildSSHConnectionExecutionCapability(smbUrl));
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
