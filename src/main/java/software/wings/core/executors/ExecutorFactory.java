package software.wings.core.executors;

import software.wings.core.executors.Executor.ExecutorType;

public class ExecutorFactory {
  public static Executor getExectorByType(String authType) {
    ExecutorType executorType = ExecutorType.valueOf(authType);
    switch (executorType) {
      case PASSWORD:
        return new SSHPwdAuthExecutor();
      default:
        // TODO: throw exception
        return null;
    }
  }
}
