package software.wings.delegatetasks;

import static io.harness.govern.Switch.unhandled;
import static io.harness.logging.CommandExecutionStatus.FAILURE;

import static java.lang.String.format;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.delegate.command.CommandExecutionResult;
import io.harness.delegate.command.CommandExecutionResultMapper;
import io.harness.delegate.service.ExecutionConfigOverrideFromFileOnDelegate;
import io.harness.exception.CommandExecutionException;
import io.harness.shell.BaseScriptExecutor;
import io.harness.shell.ScriptProcessExecutor;
import io.harness.shell.SshSessionConfig;
import io.harness.shell.SshSessionManager;

import software.wings.beans.delegation.ShellScriptParameters;
import software.wings.core.local.executors.ShellExecutorFactory;
import software.wings.core.ssh.executors.SshExecutorFactory;
import software.wings.core.winrm.executors.WinRmExecutor;
import software.wings.core.winrm.executors.WinRmExecutorFactory;
import software.wings.core.winrm.executors.WinRmSessionConfig;
import software.wings.helpers.ext.container.ContainerDeploymentDelegateHelper;
import software.wings.service.intfc.security.EncryptionService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import software.wings.service.intfc.security.SecretManagementDelegateService;

@Singleton
@TargetModule(Module._930_DELEGATE_TASKS)
public class ShellScriptTaskHandler {
  @Inject private SshExecutorFactory sshExecutorFactory;
  @Inject private WinRmExecutorFactory winrmExecutorFactory;
  @Inject private ShellExecutorFactory shellExecutorFactory;
  @Inject private EncryptionService encryptionService;
  @Inject private ContainerDeploymentDelegateHelper containerDeploymentDelegateHelper;
  @Inject private ExecutionConfigOverrideFromFileOnDelegate delegateLocalConfigService;
  @Inject private SecretManagementDelegateService secretManagementDelegateService;

  public CommandExecutionResult handle(ShellScriptParameters parameters) {
    // Define output variables and secret output variables together
    List<String> items = new ArrayList<>();
    List<String> secretItems = new ArrayList<>();
    if (parameters.getOutputVars() != null && StringUtils.isNotEmpty(parameters.getOutputVars().trim())) {
      items = Arrays.asList(parameters.getOutputVars().split("\\s*,\\s*"));
      items.replaceAll(String::trim);
    }
    if (parameters.getSecretOutputVars() != null && StringUtils.isNotEmpty(parameters.getSecretOutputVars().trim())) {
      secretItems = Arrays.asList(parameters.getSecretOutputVars().split("\\s*,\\s*"));
      secretItems.replaceAll(String::trim);
    }
    if (parameters.isExecuteOnDelegate()) {
      ScriptProcessExecutor executor = shellExecutorFactory.getExecutor(
          parameters.processExecutorConfig(containerDeploymentDelegateHelper), parameters.isSaveExecutionLogs());
      if (parameters.isLocalOverrideFeatureFlag()) {
        parameters.setScript(delegateLocalConfigService.replacePlaceholdersWithLocalConfig(parameters.getScript()));
      }
      return CommandExecutionResultMapper.from(
          executor.executeCommandString(parameters.getScript(), items, secretItems));
    }

    switch (parameters.getConnectionType()) {
      case SSH: {
        try {
          SshSessionConfig expectedSshConfig = parameters.sshSessionConfig(encryptionService, secretManagementDelegateService);
          BaseScriptExecutor executor =
              sshExecutorFactory.getExecutor(expectedSshConfig, parameters.isSaveExecutionLogs());
          return CommandExecutionResultMapper.from(
              executor.executeCommandString(parameters.getScript(), items, secretItems));
        } catch (Exception e) {
          throw new CommandExecutionException("Bash Script Failed to execute", e);
        } finally {
          SshSessionManager.evictAndDisconnectCachedSession(parameters.getActivityId(), parameters.getHost());
        }
      }
      case WINRM: {
        try {
          WinRmSessionConfig winRmSessionConfig = parameters.winrmSessionConfig(encryptionService);
          WinRmExecutor executor = winrmExecutorFactory.getExecutor(
              winRmSessionConfig, parameters.isDisableWinRMCommandEncodingFFSet(), parameters.isSaveExecutionLogs());
          return CommandExecutionResultMapper.from(
              executor.executeCommandString(parameters.getScript(), items, secretItems));
        } catch (Exception e) {
          throw new CommandExecutionException("Powershell script Failed to execute", e);
        }
      }
      default:
        unhandled(parameters.getConnectionType());
        return CommandExecutionResult.builder()
            .status(FAILURE)
            .errorMessage(format("Unsupported ConnectionType %s", parameters.getConnectionType()))
            .build();
    }
  }
}
