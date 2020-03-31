package software.wings.beans;

import static java.util.stream.Collectors.toList;

import io.harness.delegate.beans.executioncapability.ConnectivityCapabilityDemander;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.Builder;
import lombok.Getter;
import software.wings.delegatetasks.delegatecapability.CapabilityHelper;
import software.wings.settings.SettingValue;

import java.util.Collection;
import java.util.List;

@Getter
@Builder
public class HostValidationTaskParameters implements ExecutionCapabilityDemander {
  List<String> hostNames;
  SettingAttribute connectionSetting;
  List<EncryptedDataDetail> encryptionDetails;
  ExecutionCredential executionCredential;

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    if (connectionSetting == null) {
      return CapabilityHelper.fetchExecutionCapabilitiesForEncryptedDataDetails(encryptionDetails);
    }
    SettingValue settingValue = connectionSetting.getValue();
    int port = 22;
    if (settingValue instanceof WinRmConnectionAttributes) {
      port = ((WinRmConnectionAttributes) settingValue).getPort();
    } else if (settingValue instanceof HostConnectionAttributes) {
      port = ((HostConnectionAttributes) settingValue).getSshPort();
    }
    final int portf = port;
    List<List<ExecutionCapability>> capabilityDemanders =
        hostNames.stream()
            .map(host -> new ConnectivityCapabilityDemander(host, portf).fetchRequiredExecutionCapabilities())
            .collect(toList());
    return capabilityDemanders.stream().flatMap(Collection::stream).collect(toList());
  }
}
