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
import software.wings.core.ssh.executors.SshExecutor;
import software.wings.core.ssh.executors.SshExecutor.ExecutionResult;
import software.wings.core.ssh.executors.SshExecutorFactory;
import software.wings.core.ssh.executors.SshSessionConfig;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.SshNodeSetExecutorService;

import javax.inject.Inject;

@javax.inject.Singleton
public class SshNodeSetExecutorServiceImpl implements SshNodeSetExecutorService {
  private final Logger logger = LoggerFactory.getLogger(getClass());

  @Inject private WingsPersistence wingsPersistence;
  @Inject private ExecutionLogs executionLogs;

  @Override
  public void execute(Execution execution) {
    Deployment deployment = (Deployment) execution;
    for (String hostInstanceMapping : deployment.getHostInstanceMappings()) {
      //			Host host = wingsPersistence.get(HostInstanceMapping.class,
      //hostInstanceMapping).getHost(); 			ExecutionResult result = deploy(deployment,
      //getSshSessionConfig(deployment, host)); 			logger.info(String.format("Deployment [%s] on
      //Host [id: %s, ip:%s] finished with status [%S]", deployment.getUuid(), host.getUuid(), host.getIpAddress(),
      //result));
    }
  }

  public ExecutionResult deploy(Deployment deployment, SshSessionConfig config) {
    SshExecutor executor = SshExecutorFactory.getExecutor(config);
    ExecutionResult result = executor.execute(deployment.getSetupCommand());
    executionLogs.appendLogs(config.getExecutionId(), String.format("Setup finished with %s\n", result));

    if (SUCCESS == result) {
      executor = SshExecutorFactory.getExecutor(config);
      result = executor.transferFile(deployment.getArtifact().getArtifactFile().getFileUuid(), "wings/downloads/");
      executionLogs.appendLogs(config.getExecutionId(), String.format("File transfer finished with %s\n", result));

      if (SUCCESS == result) {
        executor = SshExecutorFactory.getExecutor(config);
        result = executor.execute(deployment.getDeployCommand());
      }
    }
    executionLogs.appendLogs(config.getExecutionId(), String.format("Deploy command finished with %s\n", result));
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
