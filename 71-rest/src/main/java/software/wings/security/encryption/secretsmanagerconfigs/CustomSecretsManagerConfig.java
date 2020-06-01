package software.wings.security.encryption.secretsmanagerconfigs;

import static io.harness.govern.Switch.unhandled;
import static software.wings.security.encryption.secretsmanagerconfigs.CustomSecretsManagerShellScript.ScriptType.POWERSHELL;

import com.github.reinert.jjschema.Attributes;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.task.mixin.ProcessExecutorCapabilityGenerator;
import io.harness.delegate.task.mixin.SSHConnectionExecutionCapabilityGenerator;
import io.harness.delegate.task.mixin.SocketConnectivityCapabilityGenerator;
import io.harness.security.encryption.EncryptedDataParams;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Transient;
import software.wings.annotation.EncryptableSetting;
import software.wings.beans.HostConnectionAttributes;
import software.wings.beans.SecretManagerConfig;
import software.wings.beans.WinRmConnectionAttributes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@FieldNameConstants(innerTypeName = "CustomSecretsManagerConfigKeys")
public class CustomSecretsManagerConfig extends SecretManagerConfig implements ExecutionCapabilityDemander {
  @NonNull @NotEmpty @Attributes(title = "Name") private String name;
  @NonNull @NotEmpty @Attributes(title = "Template Shell Script") private String templateId;
  @NonNull @Attributes(title = "Delegate Selectors") private List<String> delegateSelectors;
  @NonNull @Attributes(title = "Test Parameters") private Set<EncryptedDataParams> testVariables;
  @Attributes(title = "Execute on Delegate") private boolean executeOnDelegate;
  @Attributes(title = "Templatize Connector") private boolean isConnectorTemplatized;
  @Attributes(title = "Target Host") private String host;
  @Attributes(title = "Command Path") private String commandPath;
  @Attributes(title = "Connection Attributes Reference Id") private String connectorId;
  @Transient private CustomSecretsManagerShellScript customSecretsManagerShellScript;
  @Transient private EncryptableSetting remoteHostConnector;

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    if (executeOnDelegate) {
      if (customSecretsManagerShellScript.getScriptType() == POWERSHELL) {
        return Collections.singletonList(ProcessExecutorCapabilityGenerator.buildProcessExecutorCapability(
            "DELEGATE_POWERSHELL", Arrays.asList("/bin/sh", "-c", "pwsh -Version")));
      }
      return new ArrayList<>();
    }

    switch (remoteHostConnector.getSettingType()) {
      case HOST_CONNECTION_ATTRIBUTES:
        return Collections.singletonList(
            SSHConnectionExecutionCapabilityGenerator.buildSSHConnectionExecutionCapability(
                "ssh://" + host + ":" + ((HostConnectionAttributes) remoteHostConnector).getSshPort()));
      case WINRM_CONNECTION_ATTRIBUTES:
        return Collections.singletonList(SocketConnectivityCapabilityGenerator.buildSocketConnectivityCapability(
            host, Integer.toString(((WinRmConnectionAttributes) remoteHostConnector).getPort())));
      default:
        unhandled(remoteHostConnector.getSettingType());
        return null;
    }
  }

  @Override
  public void maskSecrets() {
    // Nothing to mask
  }

  @Override
  public String getEncryptionServiceUrl() {
    return null;
  }

  @Override
  public String getValidationCriteria() {
    if (executeOnDelegate) {
      return "localhost";
    } else {
      return host;
    }
  }
}
