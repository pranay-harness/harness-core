package software.wings.service.impl;

import static software.wings.common.UUIDGenerator.getUuid;
import static software.wings.core.ssh.executors.SshExecutor.ExecutionResult.SUCCESS;
import static software.wings.core.ssh.executors.SshExecutor.ExecutorType.PASSWORD;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Deployment;
import software.wings.beans.Execution;
import software.wings.beans.Host;
import software.wings.core.ssh.ExecutionLogs;
import software.wings.core.ssh.executors.SSHExecutorFactory;
import software.wings.core.ssh.executors.SshExecutor;
import software.wings.core.ssh.executors.SshExecutor.ExecutionResult;
import software.wings.core.ssh.executors.SshSessionConfig;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.SSHNodeSetExecutorService;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class SSHNodeSetExecutorServiceImpl implements SSHNodeSetExecutorService {
  private Logger LOGGER = LoggerFactory.getLogger(getClass());

  @Inject private WingsPersistence wingsPersistence;
  @Inject private ExecutionLogs executionLogs;

  @Override
  public void execute(Execution execution) {
    Deployment deployment = (Deployment) execution;
    for (String hostInstanceMapping : deployment.getHostInstanceMappings()) {
      //			Host host = wingsPersistence.get(HostInstanceMapping.class,
      //hostInstanceMapping).getHost(); 			ExecutionResult result = deploy(deployment,
      //getSshSessionConfig(deployment, host)); 			LOGGER.info(String.format("Deployment [%s] on
      //Host [id: %s, ip:%s] finished with status [%S]", deployment.getUuid(), host.getUuid(), host.getIpAddress(),
      //result));
    }
  }

  public ExecutionResult deploy(Deployment deployment, SshSessionConfig config) {
    SshExecutor executor = SSHExecutorFactory.getExecutor(config);
    ExecutionResult result = executor.execute(deployment.getSetupCommand());
    executionLogs.appendLogs(config.getExecutionID(), String.format("Setup finished with %s\n", result));

    if (SUCCESS == result) {
      executor = SSHExecutorFactory.getExecutor(config);
      result = executor.transferFile(deployment.getArtifact().getArtifactFile().getFileUUID(), "wings/downloads/");
      executionLogs.appendLogs(config.getExecutionID(), String.format("File transfer finished with %s\n", result));

      if (SUCCESS == result) {
        executor = SSHExecutorFactory.getExecutor(config);
        result = executor.execute(deployment.getDeployCommand());
      }
    }
    executionLogs.appendLogs(config.getExecutionID(), String.format("Deploy command finished with %s\n", result));
    return result;
  }

  private SshSessionConfig getSshSessionConfig(Deployment deployment, Host host) {
    String hostName = host.getHostName();
    int sshPort = host.getSshPort();
    String sshUser = deployment.getSshUser();
    String sshPassword = deployment.getSshPassword();

    return new SshSessionConfig.SshSessionConfigBuilder()
        .executionType(PASSWORD)
        .executionId(getUuid())
        .host(hostName)
        .port(sshPort)
        .user(sshUser)
        .password(sshPassword)
        .build();
  }
}
