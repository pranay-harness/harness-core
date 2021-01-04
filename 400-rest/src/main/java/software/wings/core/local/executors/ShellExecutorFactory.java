package software.wings.core.local.executors;

import software.wings.beans.command.ExecutionLogCallback;
import software.wings.core.ssh.executors.FileBasedProcessScriptExecutor;
import software.wings.core.ssh.executors.FileBasedScriptExecutor;
import software.wings.core.ssh.executors.ScriptProcessExecutor;
import software.wings.delegatetasks.DelegateFileManager;
import software.wings.delegatetasks.DelegateLogService;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class ShellExecutorFactory {
  @Inject private DelegateLogService logService;
  @Inject private DelegateFileManager fileService;

  public ScriptProcessExecutor getExecutor(ShellExecutorConfig config) {
    return new ScriptProcessExecutor(fileService, getExecutionLogCallback(config), true, config);
  }

  public ScriptProcessExecutor getExecutor(ShellExecutorConfig config, boolean shouldSaveExecutionLogs) {
    return new ScriptProcessExecutor(fileService, getExecutionLogCallback(config), shouldSaveExecutionLogs, config);
  }

  public FileBasedScriptExecutor getFileBasedExecutor(ShellExecutorConfig config) {
    return new FileBasedProcessScriptExecutor(fileService, getExecutionLogCallback(config), true, config);
  }

  ExecutionLogCallback getExecutionLogCallback(ShellExecutorConfig config) {
    return new ExecutionLogCallback(
        logService, config.getAccountId(), config.getAppId(), config.getExecutionId(), config.getCommandUnitName());
  }
}
