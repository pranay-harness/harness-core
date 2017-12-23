package software.wings.service.impl;

import static software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus.FAILURE;
import static software.wings.beans.command.CommandUnitType.COMMAND;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import software.wings.annotation.Encryptable;
import software.wings.api.DeploymentType;
import software.wings.beans.command.CleanupSshCommandUnit;
import software.wings.beans.command.Command;
import software.wings.beans.command.CommandExecutionContext;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.beans.command.CommandUnit;
import software.wings.beans.command.InitSshCommandUnit;
import software.wings.service.intfc.CommandUnitExecutorService;
import software.wings.service.intfc.ServiceCommandExecutorService;
import software.wings.service.intfc.security.EncryptionService;

import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.validation.executable.ValidateOnExecution;

/**
 * Created by anubhaw on 6/2/16.
 */
@ValidateOnExecution
@Singleton
public class ServiceCommandExecutorServiceImpl implements ServiceCommandExecutorService {
  @Inject private Map<String, CommandUnitExecutorService> commandUnitExecutorServiceMap;
  @Inject private EncryptionService encryptionService;

  /* (non-Javadoc)
   * @see software.wings.service.intfc.ServiceCommandExecutorService#execute(software.wings.beans.ServiceInstance,
   * software.wings.beans.command.Command)
   */
  @Override
  public CommandExecutionStatus execute(Command command, CommandExecutionContext context) {
    Set<String> nonSshDeploymentType = Sets.newHashSet(
        DeploymentType.AWS_CODEDEPLOY.name(), DeploymentType.ECS.name(), DeploymentType.KUBERNETES.name());
    decryptCredentials(context);
    if (!nonSshDeploymentType.contains(command.getDeploymentType())) {
      return executeSshCommand(command, context);
    } else {
      return executeNonSshCommand(command, context, commandUnitExecutorServiceMap.get(command.getDeploymentType()));
    }
  }

  private CommandExecutionStatus executeNonSshCommand(
      Command command, CommandExecutionContext context, CommandUnitExecutorService commandUnitExecutorService) {
    try {
      CommandExecutionStatus commandExecutionStatus = commandUnitExecutorService.execute(
          context.getHost(), command.getCommandUnits().get(0), context); // TODO:: do it recursively
      commandUnitExecutorService.cleanup(context.getActivityId(), context.getHost());
      return commandExecutionStatus;
    } catch (Exception ex) {
      ex.printStackTrace();
      commandUnitExecutorService.cleanup(context.getActivityId(), context.getHost());
      throw ex;
    }
  }

  private CommandExecutionStatus executeSshCommand(Command command, CommandExecutionContext context) {
    CommandUnitExecutorService commandUnitExecutorService =
        commandUnitExecutorServiceMap.get(DeploymentType.SSH.name());
    try {
      InitSshCommandUnit initCommandUnit = new InitSshCommandUnit();
      initCommandUnit.setCommand(command);
      command.getCommandUnits().add(0, initCommandUnit);
      command.getCommandUnits().add(new CleanupSshCommandUnit());
      CommandExecutionStatus commandExecutionStatus = executeSshCommand(commandUnitExecutorService, command, context);
      commandUnitExecutorService.cleanup(context.getActivityId(), context.getHost());
      return commandExecutionStatus;
    } catch (Exception ex) {
      ex.printStackTrace();
      commandUnitExecutorService.cleanup(context.getActivityId(), context.getHost());
      throw ex;
    }
  }

  private CommandExecutionStatus executeSshCommand(
      CommandUnitExecutorService commandUnitExecutorService, Command command, CommandExecutionContext context) {
    List<CommandUnit> commandUnits = command.getCommandUnits();

    CommandExecutionStatus commandExecutionStatus = CommandExecutionStatus.FAILURE;

    for (CommandUnit commandUnit : commandUnits) {
      commandExecutionStatus = COMMAND.equals(commandUnit.getCommandUnitType())
          ? executeSshCommand(commandUnitExecutorService, (Command) commandUnit, context)
          : commandUnitExecutorService.execute(context.getHost(), commandUnit, context);
      if (FAILURE == commandExecutionStatus) {
        break;
      }
    }

    return commandExecutionStatus;
  }

  private void decryptCredentials(CommandExecutionContext context) {
    if (context.getHostConnectionAttributes() != null) {
      encryptionService.decrypt(
          (Encryptable) context.getHostConnectionAttributes().getValue(), context.getHostConnectionCredentials());
    }
    if (context.getBastionConnectionAttributes() != null) {
      encryptionService.decrypt(
          (Encryptable) context.getBastionConnectionAttributes().getValue(), context.getBastionConnectionCredentials());
    }
    if (context.getCloudProviderSetting() != null) {
      encryptionService.decrypt(
          (Encryptable) context.getCloudProviderSetting().getValue(), context.getCloudProviderCredentials());
    }
  }
}
