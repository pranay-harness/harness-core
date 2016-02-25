package software.wings.service.impl;

import org.mongodb.morphia.Datastore;
import software.wings.beans.Execution;
import software.wings.beans.HostInstanceMapping;
import software.wings.core.ssh.ExecutionLogs;
import software.wings.core.ssh.executors.SSHExecutor;
import software.wings.core.ssh.executors.SSHExecutor.ExecutionResult;
import software.wings.core.ssh.executors.SSHExecutorFactory;
import software.wings.core.ssh.executors.SSHSessionConfig;
import software.wings.service.intfc.SSHNodeSetExecutorService;

import java.util.concurrent.atomic.AtomicInteger;

import static software.wings.common.UUIDGenerator.getUUID;
import static software.wings.core.ssh.executors.SSHExecutor.ExecutorType.PASSWORD;

public class SSHNodeSetExecutorServiceImpl implements SSHNodeSetExecutorService {
  private AtomicInteger maxSSHConnection;
  private Datastore datastore;

  public SSHNodeSetExecutorServiceImpl(Datastore datastore, int maxSSHConnection) {
    this.datastore = datastore;
    this.maxSSHConnection = new AtomicInteger(maxSSHConnection);
  }
  @Override
  public void execute(Execution execution) {
    for (String hostInstanceMapping : execution.getHostInstanceMappings()) {
      HostInstanceMapping hiMapping = datastore.get(HostInstanceMapping.class, hostInstanceMapping);
      String hostName = hiMapping.getHost().getHostName();
      int sshPort = hiMapping.getHost().getSshPort();
      String sshUser = execution.getSshUser();
      String sshPassword = execution.getSshPassword();
      String command = execution.getCommand();

      SSHSessionConfig config = new SSHSessionConfig.SSHSessionConfigBuilder()
                                    .executionType(PASSWORD)
                                    .executionID(getUUID())
                                    .host(hostName)
                                    .port(sshPort)
                                    .user(sshUser)
                                    .password(sshPassword)
                                    .build();

      SSHExecutor executor = SSHExecutorFactory.getExecutor(config);
      ExecutionLogs.getInstance().appendLogs(config.getExecutionID(), "Executing command " + command);
      ExecutionResult result = executor.execute(command);
      ExecutionLogs.getInstance().appendLogs(config.getExecutionID(), "Deployement finished with " + result);
    }
  }
}