package software.wings.core.winrm.executors;

import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.annotation.Encrypted;
import lombok.Builder;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.annotation.EncryptableSetting;
import software.wings.beans.WinRmConnectionAttributes.AuthenticationScheme;
import software.wings.settings.SettingValue.SettingVariableTypes;

import java.util.Map;

@Data
@Builder
public class WinRmSessionConfig implements EncryptableSetting {
  @NotEmpty private String accountId;
  @NotEmpty private String appId;
  @NotEmpty private String executionId;
  @NotEmpty private String commandUnitName;
  @NotEmpty private String hostname;
  private AuthenticationScheme authenticationScheme;
  private String domain;
  @NotEmpty private String username;
  @Encrypted private String password;
  private int port;
  private boolean useSSL;
  private boolean skipCertChecks;
  private String workingDirectory;
  private final Map<String, String> environment;

  @SchemaIgnore private String encryptedPassword;

  @Override
  public SettingVariableTypes getSettingType() {
    return SettingVariableTypes.WINRM_SESSION_CONFIG;
  }

  @Override
  public boolean isDecrypted() {
    return false;
  }

  @Override
  public void setDecrypted(boolean decrypted) {
    throw new IllegalStateException();
  }
}
