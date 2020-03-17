package software.wings.delegatetasks.validation.capabilities;

import io.harness.delegate.beans.executioncapability.CapabilityType;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.Builder;
import lombok.Value;
import software.wings.beans.PcfConfig;

import java.util.List;
import javax.validation.constraints.NotNull;

@Value
@Builder
public class PcfConnectivityCapability implements ExecutionCapability {
  @NotNull private PcfConfig pcfConfig;
  List<EncryptedDataDetail> encryptionDetails;
  private final CapabilityType capabilityType = CapabilityType.PCF_CONNECTIVITY;

  @Override
  public String fetchCapabilityBasis() {
    return "Pcf:" + pcfConfig.getEndpointUrl() + "/" + pcfConfig.getUsername();
  }
}
