package software.wings.beans;

import static java.util.stream.Collectors.toList;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.delegate.beans.executioncapability.ConnectivityCapabilityDemander;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.capability.EncryptedDataDetailsCapabilityHelper;
import io.harness.delegate.task.TaskParameters;
import io.harness.expression.ExpressionEvaluator;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.settings.SettingValue;

import java.util.Collection;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@TargetModule(HarnessModule._950_DELEGATE_TASKS_BEANS)
public class HostValidationTaskParameters implements ExecutionCapabilityDemander, TaskParameters {
  List<String> hostNames;
  SettingAttribute connectionSetting;
  List<EncryptedDataDetail> encryptionDetails;
  ExecutionCredential executionCredential;

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(ExpressionEvaluator maskingEvaluator) {
    if (connectionSetting == null) {
      return EncryptedDataDetailsCapabilityHelper.fetchExecutionCapabilitiesForEncryptedDataDetails(
          encryptionDetails, maskingEvaluator);
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
            .map(host
                -> new ConnectivityCapabilityDemander(host, portf).fetchRequiredExecutionCapabilities(maskingEvaluator))
            .collect(toList());
    return capabilityDemanders.stream().flatMap(Collection::stream).collect(toList());
  }
}
