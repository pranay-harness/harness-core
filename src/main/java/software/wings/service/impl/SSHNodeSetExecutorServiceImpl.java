package software.wings.service.impl;

import org.mongodb.morphia.Datastore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.*;
import software.wings.core.ssh.ExecutionLogs;
import software.wings.core.ssh.executors.SSHExecutor;
import software.wings.core.ssh.executors.SSHExecutor.ExecutionResult;
import software.wings.core.ssh.executors.SSHExecutorFactory;
import software.wings.core.ssh.executors.SSHSessionConfig;
import software.wings.service.intfc.SSHNodeSetExecutorService;

import java.util.concurrent.atomic.AtomicInteger;

import static software.wings.common.UUIDGenerator.getUUID;
import static software.wings.core.ssh.executors.SSHExecutor.ExecutionResult.SUCCESS;
import static software.wings.core.ssh.executors.SSHExecutor.ExecutorType.PASSWORD;

public class SSHNodeSetExecutorServiceImpl implements SSHNodeSetExecutorService {
  private AtomicInteger maxSSHConnection;
  private Datastore datastore;
  private Logger LOGGER = LoggerFactory.getLogger(getClass());

  public SSHNodeSetExecutorServiceImpl(Datastore datastore, int maxSSHConnection) {
    this.datastore = datastore;
    this.maxSSHConnection = new AtomicInteger(maxSSHConnection);
  }
  @Override
  public void execute(Execution execution) {
    Deployment deployment = (Deployment) execution;
    for (String hostInstanceMapping : deployment.getHostInstanceMappings()) {
      Host host = datastore.get(HostInstanceMapping.class, hostInstanceMapping).getHost();
      ExecutionResult result = deploy(deployment, getSshSessionConfig(deployment, host));
      LOGGER.info(String.format("Deployment [%s] on Host [id: %s, ip:%s] finished with status [%S]",
          deployment.getUuid(), host.getUuid(), host.getIpAddress(), result));
    }
  }

  public ExecutionResult deploy(Deployment deployment, SSHSessionConfig config) {
    SSHExecutor executor = SSHExecutorFactory.getExecutor(config);
    ExecutionResult result = executor.execute(deployment.getSetupCommand());
    ExecutionLogs.getInstance().appendLogs(config.getExecutionID(), String.format("Setup finished with %s\n", result));

    if (SUCCESS == result) {
      executor = SSHExecutorFactory.getExecutor(config);
      result = executor.transferFile(deployment.getArtifact().getArtifactFile().getFileUUID(), "wings/downloads/");
      ExecutionLogs.getInstance().appendLogs(
          config.getExecutionID(), String.format("File transfer finished with %s\n", result));

      if (SUCCESS == result) {
        executor = SSHExecutorFactory.getExecutor(config);
        result = executor.execute(deployment.getDeployCommand());
      }
    }
    ExecutionLogs.getInstance().appendLogs(
        config.getExecutionID(), String.format("Deploy command finished with %s\n", result));
    return result;
  }

  private SSHSessionConfig getSshSessionConfig(Deployment deployment, Host host) {
    String hostName = host.getHostName();
    int sshPort = host.getSshPort();
    String sshUser = deployment.getSshUser();
    String sshPassword = deployment.getSshPassword();

    return new SSHSessionConfig.SSHSessionConfigBuilder()
        .executionType(PASSWORD)
        .executionID(getUUID())
        .host(hostName)
        .port(sshPort)
        .user(sshUser)
        .password(sshPassword)
        .build();
  }
}