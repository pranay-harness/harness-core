package software.wings.security.encryption.secretsmanagerconfigs;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@OwnedBy(PL)
@Value
@Builder
public class CustomSecretsManagerShellScript {
  @NonNull private ScriptType scriptType;
  @NonNull private String scriptString;
  @NonNull private List<String> variables;
  private long timeoutMillis;

  public enum ScriptType { BASH, POWERSHELL }
}
