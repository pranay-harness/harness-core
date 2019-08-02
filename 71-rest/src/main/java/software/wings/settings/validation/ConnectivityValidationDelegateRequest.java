package software.wings.settings.validation;

import static io.harness.exception.WingsException.USER;

import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.task.mixin.SSHConnectionExecutionCapabilityGenerator;
import io.harness.exception.InvalidRequestException;
import lombok.Builder;
import lombok.Data;
import software.wings.beans.HostConnectionAttributes;
import software.wings.beans.SettingAttribute;
import software.wings.delegatetasks.delegatecapability.CapabilityHelper;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.settings.SettingValue;

import java.util.Collections;
import java.util.List;

@Data
@Builder
public class ConnectivityValidationDelegateRequest implements ExecutionCapabilityDemander {
  private SettingAttribute settingAttribute;
  private List<EncryptedDataDetail> encryptedDataDetails;

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    if (settingAttribute == null) {
      return CapabilityHelper.generateKmsHttpCapabilities(encryptedDataDetails);
    }
    SettingValue value = settingAttribute.getValue();
    ConnectivityValidationAttributes connectivityValidationAttributes = settingAttribute.getValidationAttributes();
    if (!(connectivityValidationAttributes instanceof SshConnectionConnectivityValidationAttributes)) {
      throw new InvalidRequestException("Must send Ssh connectivity attributes", USER);
    }
    SshConnectionConnectivityValidationAttributes validationAttributes =
        (SshConnectionConnectivityValidationAttributes) settingAttribute.getValidationAttributes();
    if (value instanceof HostConnectionAttributes) {
      String validationUrl =
          "ssh://" + validationAttributes.getHostName() + ":" + ((HostConnectionAttributes) value).getSshPort();
      return Collections.singletonList(
          SSHConnectionExecutionCapabilityGenerator.buildSSHConnectionExecutionCapability(validationUrl));
    } else {
      String validationUrl = "ssh://" + validationAttributes.getHostName() + ":22";
      return Collections.singletonList(
          SSHConnectionExecutionCapabilityGenerator.buildSSHConnectionExecutionCapability(validationUrl));
    }
  }
}