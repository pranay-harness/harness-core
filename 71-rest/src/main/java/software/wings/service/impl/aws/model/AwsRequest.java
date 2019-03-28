package software.wings.service.impl.aws.model;

import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import lombok.AllArgsConstructor;
import lombok.Data;
import software.wings.beans.AwsConfig;
import software.wings.delegatetasks.delegatecapability.CapabilityHelper;
import software.wings.security.encryption.EncryptedDataDetail;

import java.util.List;
import javax.validation.constraints.NotNull;

@Data
@AllArgsConstructor
public class AwsRequest implements ExecutionCapabilityDemander {
  @NotNull private AwsConfig awsConfig;
  @NotNull private List<EncryptedDataDetail> encryptionDetails;

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    return CapabilityHelper.generateDelegateCapabilities(awsConfig, getEncryptionDetails());
  }
}