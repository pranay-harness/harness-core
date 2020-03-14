package software.wings.delegatetasks.validation.capabilities;

import io.harness.delegate.beans.executioncapability.CapabilityType;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import lombok.Builder;
import lombok.Value;

import javax.validation.constraints.NotNull;

@Value
@Builder
public class SftpCapability implements ExecutionCapability {
  @NotNull String sftpUrl;
  @Builder.Default private final CapabilityType capabilityType = CapabilityType.SFTP;

  @Override
  public String fetchCapabilityBasis() {
    return sftpUrl;
  }
}
