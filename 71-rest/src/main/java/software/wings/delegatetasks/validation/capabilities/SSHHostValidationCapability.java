package software.wings.delegatetasks.validation.capabilities;

import io.harness.delegate.beans.executioncapability.CapabilityType;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.Builder;
import lombok.Value;
import software.wings.beans.SSHExecutionCredential;
import software.wings.beans.SettingAttribute;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;

@Value
@Builder
public class SSHHostValidationCapability implements ExecutionCapability {
  @NotNull BasicValidationInfo validationInfo;
  @NotNull private SettingAttribute hostConnectionAttributes;
  @NotNull private SettingAttribute bastionConnectionAttributes;
  private List<EncryptedDataDetail> hostConnectionCredentials;
  private List<EncryptedDataDetail> bastionConnectionCredentials;
  private SSHExecutionCredential sshExecutionCredential;
  private Map<String, String> envVariables = new HashMap<>();
  @Builder.Default private final CapabilityType capabilityType = CapabilityType.SSH_HOST_CONNECTION;

  @Override
  public String fetchCapabilityBasis() {
    if (validationInfo.isExecuteOnDelegate()) {
      return "localhost";
    }
    return validationInfo.getPublicDns();
  }
}
