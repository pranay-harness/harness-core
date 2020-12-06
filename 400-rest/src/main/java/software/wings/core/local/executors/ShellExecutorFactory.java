package software.wings.core.local.executors;

import software.wings.core.ssh.executors.ScriptExecutionContext;
import software.wings.core.ssh.executors.ScriptProcessExecutor;
import software.wings.delegatetasks.DelegateFileManager;
import software.wings.delegatetasks.DelegateLogService;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class ShellExecutorFactory {
  @Inject private DelegateLogService logService;
  @Inject private DelegateFileManager fileService;

  public ScriptProcessExecutor getExecutor(ScriptExecutionContext config) {
    return new ScriptProcessExecutor(fileService, logService, true, config);
  }

  public ScriptProcessExecutor getExecutor(ScriptExecutionContext config, boolean shouldSaveExecutionLogs) {
    return new ScriptProcessExecutor(fileService, logService, shouldSaveExecutionLogs, config);
  }
}
