package software.wings.delegatetasks;

import static software.wings.beans.command.CommandExecutionResult.Builder.aCommandExecutionResult;
import static software.wings.core.ssh.executors.SshSessionConfig.Builder.aSshSessionConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.DelegateTask;
import software.wings.beans.command.CommandExecutionResult;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.beans.delegation.ScriptParameters;
import software.wings.core.ssh.executors.SshExecutor;
import software.wings.core.ssh.executors.SshExecutor.ExecutorType;
import software.wings.core.ssh.executors.SshExecutorFactory;
import software.wings.core.ssh.executors.SshSessionConfig;
import software.wings.exception.WingsException;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.waitnotify.NotifyResponseData;

import java.io.IOException;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.inject.Inject;

public class ScriptTask extends AbstractDelegateRunnableTask {
  private static final Logger logger = LoggerFactory.getLogger(CommandTask.class);

  @Inject private SshExecutorFactory sshExecutorFactory;
  @Inject private EncryptionService encryptionService;

  public ScriptTask(String delegateId, DelegateTask delegateTask, Consumer<NotifyResponseData> postExecute,
      Supplier<Boolean> preExecute) {
    super(delegateId, delegateTask, postExecute, preExecute);
  }

  @Override
  public CommandExecutionResult run(Object[] parameters) {
    return run((ScriptParameters) parameters[0]);
  }

  private CommandExecutionResult run(ScriptParameters parameters) {
    SshExecutor executor = sshExecutorFactory.getExecutor(ExecutorType.KEY_AUTH);

    try {
      char[] key = encryptionService.getDecryptedValue(parameters.getKeyEncryptedDataDetails().get(0));

      SshSessionConfig expectedSshConfig = aSshSessionConfig()
                                               .withAccountId(parameters.getAccountId())
                                               .withAppId(parameters.getAppId())
                                               .withExecutionId(parameters.getActivityId())
                                               .withHost(parameters.getHost())
                                               .withKey(key)
                                               .withCommandUnitName(ScriptParameters.CommandUnit)
                                               .withPort(22)
                                               .build();
      executor.init(expectedSshConfig);

      CommandExecutionStatus commandExecutionStatus = executor.executeCommandString(parameters.getScript());

      return aCommandExecutionResult().withStatus(commandExecutionStatus).build();
    } catch (IOException e) {
      throw new WingsException(e);
    }
  }
}
