package software.wings.delegatetasks.validation.capabilities;

import io.harness.delegate.beans.executioncapability.CapabilityType;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.Builder;
import lombok.Value;
import software.wings.beans.GitConfig;

import java.util.List;

@Value
@Builder

public class GitConnectionCapability implements ExecutionCapability {
  GitConfig gitConfig;
  List<EncryptedDataDetail> encryptedDataDetails;
  CapabilityType capabilityType = CapabilityType.GIT_CONNECTION;

  @Override
  public String fetchCapabilityBasis() {
    return "GIT:" + gitConfig.getRepoUrl();
  }
}
