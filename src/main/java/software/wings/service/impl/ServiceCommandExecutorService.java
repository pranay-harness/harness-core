package software.wings.service.impl;

import software.wings.beans.Command;
import software.wings.beans.CommandUnit.ExecutionResult;
import software.wings.beans.ServiceInstance;

/**
 * Created by anubhaw on 6/2/16.
 */
public interface ServiceCommandExecutorService {
  ExecutionResult execute(ServiceInstance serviceInstance, Command command);
}
