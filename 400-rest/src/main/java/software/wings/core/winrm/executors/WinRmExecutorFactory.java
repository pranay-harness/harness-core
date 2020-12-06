package software.wings.core.winrm.executors;

import software.wings.delegatetasks.DelegateFileManager;
import software.wings.delegatetasks.DelegateLogService;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class WinRmExecutorFactory {
  @Inject private DelegateLogService logService;
  @Inject private DelegateFileManager delegateFileManager;

  public WinRmExecutor getExecutor(WinRmSessionConfig config, boolean disableCommandEncoding) {
    return new DefaultWinRmExecutor(logService, delegateFileManager, true, config, disableCommandEncoding);
  }

  public WinRmExecutor getExecutor(
      WinRmSessionConfig config, boolean disableCommandEncoding, boolean shouldSaveExecutionLogs) {
    return new DefaultWinRmExecutor(
        logService, delegateFileManager, shouldSaveExecutionLogs, config, disableCommandEncoding);
  }
}
