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
import software.wings.annotation.Encryptable;
import software.wings.annotation.Encrypted;
import software.wings.jersey.JsonViews;
import software.wings.settings.SettingValue;
import software.wings.settings.UsageRestrictions;

import javax.validation.constraints.NotNull;

@JsonTypeName("WINRM_CONNECTION_ATTRIBUTES")
@Builder
@Data
@EqualsAndHashCode(callSuper = false)
@ToString(exclude = "password")
public class WinRmConnectionAttributes extends SettingValue implements Encryptable {
  @Attributes(required = true) @NotNull private AuthenticationScheme authenticationScheme;
  private String domain;
  @Attributes(required = true) @NotEmpty private String username;
  @Attributes(required = true) @Encrypted private char[] password;
  @Attributes(required = true) private boolean useSSL;
  @Attributes(required = true) private int port;
  @Attributes(required = true) private boolean skipCertChecks;
  @SchemaIgnore @NotEmpty private String accountId;

  @JsonView(JsonViews.Internal.class) @SchemaIgnore private String encryptedPassword;

  public enum AuthenticationScheme { BASIC, NTLM }

  public WinRmConnectionAttributes() {
    super(SettingVariableTypes.WINRM_CONNECTION_ATTRIBUTES.name());
  }

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public WinRmConnectionAttributes(AuthenticationScheme authenticationScheme, String domain, String username,
      char[] password, boolean useSSL, int port, boolean skipCertChecks, String accountId, String encryptedPassword) {
    super(SettingVariableTypes.WINRM_CONNECTION_ATTRIBUTES.name());
    this.authenticationScheme = authenticationScheme;
    this.domain = domain;
    this.username = username;
    this.password = password;
    this.useSSL = useSSL;
    this.port = port;
    this.skipCertChecks = skipCertChecks;
    this.accountId = accountId;
    this.encryptedPassword = encryptedPassword;
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @NoArgsConstructor
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
        UsageRestrictions usageRestrictions) {
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
