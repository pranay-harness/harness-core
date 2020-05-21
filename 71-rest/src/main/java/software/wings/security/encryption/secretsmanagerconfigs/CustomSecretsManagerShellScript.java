package software.wings.security.encryption.secretsmanagerconfigs;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class CustomSecretsManagerShellScript {
  @NonNull private ScriptType scriptType;
  @NonNull private String scriptString;
  @NonNull private List<String> variables;
  private long timeoutMillis;

  public enum ScriptType { BASH, POWERSHELL }
}
