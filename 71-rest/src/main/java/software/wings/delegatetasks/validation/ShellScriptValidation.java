package software.wings.delegatetasks.validation;

import static io.harness.govern.Switch.unhandled;
import static java.time.Duration.ofSeconds;
import static java.util.Collections.singletonList;
import static software.wings.common.Constants.HARNESS_KUBE_CONFIG_PATH;
import static software.wings.core.ssh.executors.SshSessionFactory.getSSHSession;

import com.google.inject.Inject;

import io.harness.beans.DelegateTask;
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
import software.wings.delegatetasks.validation.DelegateConnectionResult.DelegateConnectionResultBuilder;
import software.wings.service.impl.ContainerServiceParams;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.settings.SettingValue;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

@Slf4j
public class ShellScriptValidation extends AbstractDelegateValidateTask {
  @Inject private transient EncryptionService encryptionService;
  @Inject private transient ContainerValidationHelper containerValidationHelper;

  public ShellScriptValidation(
      String delegateId, DelegateTask delegateTask, Consumer<List<DelegateConnectionResult>> postExecute) {
    super(delegateId, delegateTask, postExecute);
  }

  @Override
  public List<DelegateConnectionResult> validate() {
    Object[] parameters = getParameters();
    return singletonList(validate((ShellScriptParameters) parameters[0]));
  }

  private DelegateConnectionResult validate(ShellScriptParameters parameters) {
    DelegateConnectionResultBuilder resultBuilder = DelegateConnectionResult.builder().criteria(getCriteria().get(0));

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
      return resultBuilder.validated(validated).build();
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

          resultBuilder.validated(true);
        } catch (Exception ex) {
          logger.info("Exception in sshSession Validation: {}", ex);
          resultBuilder.validated(false);
        }
        break;

      case WINRM:
        try {
          WinRmSessionConfig winrmConfig = parameters.winrmSessionConfig(encryptionService);
          winrmConfig.setTimeout(timeout);
          logger.info("Validating WinrmSession to Host: {}, Port: {}, useSsl: {}", winrmConfig.getHostname(),
              winrmConfig.getPort(), winrmConfig.isUseSSL());

          try (WinRmSession ignore = new WinRmSession(winrmConfig)) {
            resultBuilder.validated(true);
          }
        } catch (Exception e) {
          logger.info("Exception in WinrmSession Validation: {}", e);
          resultBuilder.validated(false);
        }
        break;

      default:
        unhandled(parameters.getConnectionType());
        resultBuilder.validated(false);
    }

    return resultBuilder.build();
  }

  @Override
  public List<String> getCriteria() {
    ShellScriptParameters parameters = (ShellScriptParameters) getParameters()[0];

    String criteria;
    if (parameters.isExecuteOnDelegate()) {
      criteria = "localhost";
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
            criteria = containerValidationHelper.getCriteria(containerServiceParams);
          }
        }
      }
    } else {
      criteria = parameters.getHost();
    }
    return singletonList(criteria);
  }
}
