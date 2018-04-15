package software.wings.core.local.executors;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import software.wings.api.ScriptType;
import software.wings.delegatetasks.DelegateLogService;

@Singleton
public class ShellExecutorFactory {
  @Inject private DelegateLogService logService;

  public ShellExecutor getExecutor(ShellExecutorConfig config, ScriptType scriptType) {
    return new ShellExecutor(logService, config, scriptType);
  }
}
