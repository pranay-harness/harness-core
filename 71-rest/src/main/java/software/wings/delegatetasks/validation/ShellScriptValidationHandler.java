package software.wings.delegatetasks.validation;

import static io.harness.govern.Switch.unhandled;
import static java.time.Duration.ofSeconds;
import static software.wings.common.Constants.HARNESS_KUBE_CONFIG_PATH;
import static software.wings.core.ssh.executors.SshSessionFactory.getSSHSession;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.delegate.beans.executioncapability.CapabilityResponse;
import io.harness.delegate.beans.executioncapability.CapabilityType;
import io.harness.delegate.beans.executioncapability.ProcessExecutorCapability;
import io.harness.delegate.task.executioncapability.ProcessExecutorCapabilityCheck;
import io.harness.delegate.task.shell.ScriptType;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.AzureConfig;
import software.wings.beans.GcpConfig;
import software.wings.beans.KubernetesClusterConfig;
import software.wings.beans.KubernetesConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.delegation.ShellScriptParameters;
import software.wings.core.ssh.executors.SshSessionConfig;
import software.wings.core.winrm.executors.WinRmSession;
import software.wings.core.winrm.executors.WinRmSessionConfig;
import software.wings.service.impl.ContainerServiceParams;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.settings.SettingValue;

import java.util.Arrays;

@Slf4j
@Singleton
public class ShellScriptValidationHandler {
  @Inject private EncryptionService encryptionService;
  @Inject private ContainerValidationHelper containerValidationHelper;

  public boolean handle(ShellScriptParameters parameters) {
    boolean validated = true;
    if (parameters.isExecuteOnDelegate()) {
      ContainerServiceParams containerServiceParams = parameters.getContainerServiceParams();
      if (containerServiceParams != null) {
        SettingAttribute settingAttribute = containerServiceParams.getSettingAttribute();
        if (settingAttribute != null) {
          SettingValue value = settingAttribute.getValue();
          boolean useKubernetesDelegate =
              value instanceof KubernetesClusterConfig && ((KubernetesClusterConfig) value).isUseKubernetesDelegate();
          boolean isKubernetes = value instanceof KubernetesConfig || value instanceof GcpConfig
              || value instanceof AzureConfig || value instanceof KubernetesClusterConfig;
          if (useKubernetesDelegate || (isKubernetes && parameters.getScript().contains(HARNESS_KUBE_CONFIG_PATH))) {
            validated = containerValidationHelper.validateContainerServiceParams(containerServiceParams);
          }
        }
      }
      if (validated && parameters.getScriptType() == ScriptType.POWERSHELL) {
        ProcessExecutorCapabilityCheck executorCapabilityCheck = new ProcessExecutorCapabilityCheck();
        CapabilityResponse response = executorCapabilityCheck.performCapabilityCheck(
            ProcessExecutorCapability.builder()
                .capabilityType(CapabilityType.POWERSHELL)
                .category("POWERSHELL_DELEGATE")
                .processExecutorArguments(Arrays.asList("/bin/sh", "-c", "pwsh -Version"))
                .build());
        validated = response.isValidated();
      }
      return validated;
    }

    int timeout = (int) ofSeconds(15L).toMillis();
    switch (parameters.getConnectionType()) {
      case SSH:
        try {
          SshSessionConfig expectedSshConfig = parameters.sshSessionConfig(encryptionService);
          expectedSshConfig.setSocketConnectTimeout(timeout);
          expectedSshConfig.setSshConnectionTimeout(timeout);
          expectedSshConfig.setSshSessionTimeout(timeout);
          getSSHSession(expectedSshConfig).disconnect();

          return true;
        } catch (Exception ex) {
          logger.info("Exception in sshSession Validation", ex);
          return false;
        }

      case WINRM:
        try {
          WinRmSessionConfig winrmConfig = parameters.winrmSessionConfig(encryptionService);
          winrmConfig.setTimeout(timeout);
          logger.info("Validating WinrmSession to Host: {}, Port: {}, useSsl: {}", winrmConfig.getHostname(),
              winrmConfig.getPort(), winrmConfig.isUseSSL());

          try (WinRmSession ignore = new WinRmSession(winrmConfig)) {
            return true;
          }
        } catch (Exception e) {
          logger.info("Exception in WinrmSession Validation", e);
          return false;
        }

      default:
        unhandled(parameters.getConnectionType());
        return false;
    }
  }
}
