package software.wings.delegatetasks.validation.capabilities;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.delegate.beans.executioncapability.CapabilityType;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.SSHExecutionCredential;
import software.wings.beans.SSHVaultConfig;
import software.wings.beans.SettingAttribute;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@TargetModule(Module._930_DELEGATE_TASKS)
@OwnedBy(CDP)
public class SSHHostValidationCapability implements ExecutionCapability {
  @NotNull BasicValidationInfo validationInfo;
  @NotNull private SettingAttribute hostConnectionAttributes;
  @NotNull private SettingAttribute bastionConnectionAttributes;
  private List<EncryptedDataDetail> hostConnectionCredentials;
  private List<EncryptedDataDetail> bastionConnectionCredentials;
  private SSHExecutionCredential sshExecutionCredential;
  private SSHVaultConfig sshVaultConfig;
  private Map<String, String> envVariables = new HashMap<>();
  @Builder.Default private final CapabilityType capabilityType = CapabilityType.SSH_HOST_CONNECTION;

  @Override
  public EvaluationMode evaluationMode() {
    return EvaluationMode.AGENT;
  }

  @Override
  public String fetchCapabilityBasis() {
    if (validationInfo.isExecuteOnDelegate()) {
      return "localhost";
    }
    return validationInfo.getPublicDns();
  }

  @Override
  public Duration getMaxValidityPeriod() {
    return Duration.ofHours(6);
  }

  @Override
  public Duration getPeriodUntilNextValidation() {
    return Duration.ofHours(4);
  }
}
