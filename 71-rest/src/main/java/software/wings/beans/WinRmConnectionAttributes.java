package software.wings.beans;

import static software.wings.audit.ResourceType.CONNECTION_ATTRIBUTES;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonView;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
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

import java.util.ArrayList;
import java.util.List;
import javax.validation.constraints.NotNull;

@JsonTypeName("WINRM_CONNECTION_ATTRIBUTES")
@Data
@Builder
@ToString(exclude = "password")
@EqualsAndHashCode(callSuper = false)
public class WinRmConnectionAttributes extends SettingValue implements EncryptableSetting {
  @Attributes(required = true) @NotNull private AuthenticationScheme authenticationScheme;
  private String domain;
  @Attributes(required = true) @NotEmpty private String username;
  @Attributes(required = true) @Encrypted(fieldName = "password") private char[] password;
  @Attributes(required = true) private boolean useSSL;
  @Attributes(required = true) private int port;
  @Attributes(required = true) private boolean skipCertChecks;
  @SchemaIgnore @NotEmpty private String accountId;

  @JsonView(JsonViews.Internal.class) @SchemaIgnore private String encryptedPassword;

  // Always called inside other ExecutionCapabilityDemander Check ShellScriptParameters and
  // ConnectivityValidationDelegateRequest
  // Returning empty list here
  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    return new ArrayList<>();
  }

  public enum AuthenticationScheme { BASIC, NTLM }

  public WinRmConnectionAttributes() {
    super(SettingVariableTypes.WINRM_CONNECTION_ATTRIBUTES.name());
  }

  public WinRmConnectionAttributes(AuthenticationScheme authenticationScheme, String domain, String username,
      char[] password, boolean useSSL, int port, boolean skipCertChecks, String accountId, String encryptedPassword) {
    super(SettingVariableTypes.WINRM_CONNECTION_ATTRIBUTES.name());
    this.authenticationScheme = authenticationScheme;
    this.domain = domain;
    this.username = username;
    this.password = password == null ? null : password.clone();
    this.useSSL = useSSL;
    this.port = port;
    this.skipCertChecks = skipCertChecks;
    this.accountId = accountId;
    this.encryptedPassword = encryptedPassword;
  }

  @Override
  public String fetchResourceCategory() {
    return CONNECTION_ATTRIBUTES.name();
  }

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  public static final class Yaml extends SettingValue.Yaml {
    private AuthenticationScheme authenticationScheme;
    private String domain;
    private String userName;
    private String password;
    private boolean useSSL;
    private int port;
    private boolean skipCertChecks;

    @lombok.Builder
    public Yaml(String type, String harnessApiVersion, AuthenticationScheme authenticationScheme, String domain,
        String userName, String password, boolean useSSL, int port, boolean skipCertChecks,
        UsageRestrictions.Yaml usageRestrictions) {
      super(type, harnessApiVersion, usageRestrictions);
      this.authenticationScheme = authenticationScheme;
      this.domain = domain;
      this.userName = userName;
      this.password = password;
      this.useSSL = useSSL;
      this.port = port;
      this.skipCertChecks = skipCertChecks;
    }
  }
}
