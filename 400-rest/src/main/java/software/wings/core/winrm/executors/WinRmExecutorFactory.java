package software.wings.core.winrm.executors;

import software.wings.beans.command.ExecutionLogCallback;
import software.wings.core.ssh.executors.FileBasedWinRmExecutor;
import software.wings.delegatetasks.DelegateFileManager;
import software.wings.delegatetasks.DelegateLogService;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class WinRmExecutorFactory {
  @Inject private DelegateLogService logService;
  @Inject private DelegateFileManager delegateFileManager;

  public WinRmExecutor getExecutor(WinRmSessionConfig config, boolean disableCommandEncoding) {
    return new DefaultWinRmExecutor(
        getExecutionLogCallback(config), delegateFileManager, true, config, disableCommandEncoding);
  }

  public WinRmExecutor getExecutor(
      WinRmSessionConfig config, boolean disableCommandEncoding, boolean shouldSaveExecutionLogs) {
    return new DefaultWinRmExecutor(
        getExecutionLogCallback(config), delegateFileManager, shouldSaveExecutionLogs, config, disableCommandEncoding);
  }

  public FileBasedWinRmExecutor getFiledBasedWinRmExecutor(WinRmSessionConfig config, boolean disableCommandEncoding) {
    return new FileBasedWinRmExecutor(
        getExecutionLogCallback(config), delegateFileManager, true, config, disableCommandEncoding);
  }

  ExecutionLogCallback getExecutionLogCallback(WinRmSessionConfig config) {
    return new ExecutionLogCallback(
        logService, config.getAccountId(), config.getAppId(), config.getExecutionId(), config.getCommandUnitName());
  }
}
