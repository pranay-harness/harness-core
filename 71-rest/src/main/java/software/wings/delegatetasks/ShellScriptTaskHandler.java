package software.wings.delegatetasks;

import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.FAILURE;
import static io.harness.govern.Switch.unhandled;
import static java.lang.String.format;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.delegate.command.CommandExecutionResult;
import io.harness.delegate.service.ExecutionConfigOverrideFromFileOnDelegate;
import io.harness.delegate.task.shell.ScriptType;
import io.harness.exception.CommandExecutionException;
import org.apache.commons.lang3.StringUtils;
import software.wings.beans.delegation.ShellScriptParameters;
import software.wings.core.local.executors.ShellExecutorFactory;
import software.wings.core.ssh.executors.ScriptExecutor;
import software.wings.core.ssh.executors.ScriptProcessExecutor;
import software.wings.core.ssh.executors.ScriptSshExecutor;
import software.wings.core.ssh.executors.SshExecutorFactory;
import software.wings.core.ssh.executors.SshSessionConfig;
import software.wings.core.winrm.executors.WinRmExecutor;
import software.wings.core.winrm.executors.WinRmExecutorFactory;
import software.wings.core.winrm.executors.WinRmSessionConfig;
import software.wings.helpers.ext.container.ContainerDeploymentDelegateHelper;
import software.wings.service.intfc.security.EncryptionService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Singleton
public class ShellScriptTaskHandler {
  @Inject private SshExecutorFactory sshExecutorFactory;
  @Inject private WinRmExecutorFactory winrmExecutorFactory;
  @Inject private ShellExecutorFactory shellExecutorFactory;
  @Inject private EncryptionService encryptionService;
  @Inject private ContainerDeploymentDelegateHelper containerDeploymentDelegateHelper;
  @Inject private ExecutionConfigOverrideFromFileOnDelegate delegateLocalConfigService;

  public CommandExecutionResult handle(ShellScriptParameters parameters) {
    if (parameters.isExecuteOnDelegate()) {
      ScriptProcessExecutor executor =
          shellExecutorFactory.getExecutor(parameters.processExecutorConfig(containerDeploymentDelegateHelper));
      List<String> items = new ArrayList<>();
      if (parameters.getOutputVars() != null && StringUtils.isNotEmpty(parameters.getOutputVars().trim())) {
        items = Arrays.asList(parameters.getOutputVars().split("\\s*,\\s*"));
        items.replaceAll(String::trim);
      }
      if (parameters.isLocalOverrideFeatureFlag()) {
        parameters.setScript(delegateLocalConfigService.replacePlaceholdersWithLocalConfig(parameters.getScript()));
      }
      if (parameters.getScriptType() == ScriptType.POWERSHELL) {
        parameters.setScript(processPowerShellScript(parameters.getScript()));
      }
      return executor.executeCommandString(parameters.getScript(), items);
    }

    switch (parameters.getConnectionType()) {
      case SSH: {
        try {
          SshSessionConfig expectedSshConfig = parameters.sshSessionConfig(encryptionService);
          ScriptExecutor executor = sshExecutorFactory.getExecutor(expectedSshConfig);
          List<String> items = new ArrayList<>();
          if (parameters.getOutputVars() != null && StringUtils.isNotEmpty(parameters.getOutputVars().trim())) {
            items = Arrays.asList(parameters.getOutputVars().split("\\s*,\\s*"));
            items.replaceAll(String::trim);
          }
          return executor.executeCommandString(parameters.getScript(), items);
        } catch (Exception e) {
          throw new CommandExecutionException("Bash Script Failed to execute", e);
        } finally {
          ScriptSshExecutor.evictAndDisconnectCachedSession(parameters.getActivityId(), parameters.getHost());
        }
      }
      case WINRM: {
        try {
          WinRmSessionConfig winRmSessionConfig = parameters.winrmSessionConfig(encryptionService);
          WinRmExecutor executor = winrmExecutorFactory.getExecutor(winRmSessionConfig);
          List<String> items = new ArrayList<>();
          if (parameters.getOutputVars() != null && StringUtils.isNotEmpty(parameters.getOutputVars().trim())) {
            items = Arrays.asList(parameters.getOutputVars().split("\\s*,\\s*"));
            items.replaceAll(String::trim);
          }
          return executor.executeCommandString(parameters.getScript(), items);
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

  private String processPowerShellScript(String scriptString) {
    StringBuilder scriptStringBuilder =
        new StringBuilder("pwsh -ExecutionPolicy \"$ErrorActionPreference=Stop\" -Command \" & {");
    scriptStringBuilder.append(scriptString.replace("$", "\\$").replace("\"", "\\\"")).append("}\"");
    return scriptStringBuilder.toString();
  }
}
