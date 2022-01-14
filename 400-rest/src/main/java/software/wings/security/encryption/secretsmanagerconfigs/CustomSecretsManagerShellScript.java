package software.wings.security.encryption.secretsmanagerconfigs;

import static io.harness.annotations.dev.HarnessModule._440_SECRET_MANAGEMENT_SERVICE;
import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import java.util.List;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@OwnedBy(PL)
@Value
@Builder
@TargetModule(_440_SECRET_MANAGEMENT_SERVICE)
public class CustomSecretsManagerShellScript {
  @NonNull private ScriptType scriptType;
  @NonNull private String scriptString;
  @NonNull private List<String> variables;
  private long timeoutMillis;

  public enum ScriptType { BASH, POWERSHELL }
}
