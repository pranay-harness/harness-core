package software.wings.core.local.executors;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import software.wings.core.ssh.executors.ScriptExecutionContext;
import software.wings.core.ssh.executors.ScriptProcessExecutor;
import software.wings.delegatetasks.DelegateFileManager;
import software.wings.delegatetasks.DelegateLogService;

@Singleton
public class ShellExecutorFactory {
  @Inject private DelegateLogService logService;
  @Inject private DelegateFileManager fileService;

  public ScriptProcessExecutor getExecutor(ScriptExecutionContext config) {
    return new ScriptProcessExecutor(fileService, logService, config);
  }
}
