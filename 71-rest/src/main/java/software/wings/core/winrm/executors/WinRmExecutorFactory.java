package software.wings.core.winrm.executors;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import software.wings.delegatetasks.DelegateFileManager;
import software.wings.delegatetasks.DelegateLogService;

@Singleton
public class WinRmExecutorFactory {
  @Inject private DelegateLogService logService;
  @Inject private DelegateFileManager delegateFileManager;

  public WinRmExecutor getExecutor(WinRmSessionConfig config, boolean disableCommandEncoding) {
    return new DefaultWinRmExecutor(logService, delegateFileManager, config, disableCommandEncoding);
  }
}
