package software.wings.delegatetasks;

import static software.wings.beans.command.CommandExecutionResult.Builder.aCommandExecutionResult;

import com.google.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.DelegateTask;
import software.wings.beans.command.CommandExecutionResult;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.beans.delegation.ShellScriptParameters;
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

public class ShellScriptTask extends AbstractDelegateRunnableTask {
  private static final Logger logger = LoggerFactory.getLogger(CommandTask.class);

  @Inject private SshExecutorFactory sshExecutorFactory;
  @Inject private EncryptionService encryptionService;

  public ShellScriptTask(String delegateId, DelegateTask delegateTask, Consumer<NotifyResponseData> postExecute,
      Supplier<Boolean> preExecute) {
    super(delegateId, delegateTask, postExecute, preExecute);
  }

  @Override
  public CommandExecutionResult run(Object[] parameters) {
    return run((ShellScriptParameters) parameters[0]);
  }

  private CommandExecutionResult run(ShellScriptParameters parameters) {
    SshExecutor executor = sshExecutorFactory.getExecutor(ExecutorType.KEY_AUTH);

    try {
      SshSessionConfig expectedSshConfig = parameters.sshSessionConfig(encryptionService);
      executor.init(expectedSshConfig);

      CommandExecutionStatus commandExecutionStatus = executor.executeCommandString(parameters.getScript());

      return aCommandExecutionResult().withStatus(commandExecutionStatus).build();
    } catch (IOException e) {
      throw new WingsException(e);
    }
  }
}
