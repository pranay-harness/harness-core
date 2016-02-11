package software.wings.core.executors;

import software.wings.core.executors.SSHExecutor.ExecutorType;

public class ExecutorFactory {
  public static SSHExecutor getExectorByType(String authType) {
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
