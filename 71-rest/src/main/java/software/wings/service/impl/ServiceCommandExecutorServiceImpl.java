package software.wings.service.impl;

import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.FAILURE;
import static java.util.stream.Collectors.toList;
import static software.wings.beans.command.CommandUnitType.COMMAND;

import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.delegate.task.shell.ScriptType;
import lombok.extern.slf4j.Slf4j;
import software.wings.annotation.EncryptableSetting;
import software.wings.api.DeploymentType;
import software.wings.beans.command.CleanupPowerShellCommandUnit;
import software.wings.beans.command.CleanupSshCommandUnit;
import software.wings.beans.command.Command;
import software.wings.beans.command.CommandExecutionContext;
import software.wings.beans.command.CommandUnit;
import software.wings.beans.command.CommandUnitType;
import software.wings.beans.command.ExecCommandUnit;
import software.wings.beans.command.InitPowerShellCommandUnit;
import software.wings.beans.command.InitSshCommandUnit;
import software.wings.beans.command.InitSshCommandUnitV2;
import software.wings.service.intfc.CommandUnitExecutorService;
import software.wings.service.intfc.ServiceCommandExecutorService;
import software.wings.service.intfc.security.EncryptionService;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import javax.validation.executable.ValidateOnExecution;

/**
 * Created by anubhaw on 6/2/16.
 */
@ValidateOnExecution
@Singleton
@Slf4j
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
    if (!nonSshDeploymentType.contains(context.getDeploymentType())) {
      return executeShellCommand(command, context, context.getDeploymentType());
    } else {
      return executeNonSshCommand(command, context, commandUnitExecutorServiceMap.get(context.getDeploymentType()));
    }
  }

  private CommandExecutionStatus executeNonSshCommand(
      Command command, CommandExecutionContext context, CommandUnitExecutorService commandUnitExecutorService) {
    try {
      CommandExecutionStatus commandExecutionStatus =
          commandUnitExecutorService.execute(command.getCommandUnits().get(0), context); // TODO:: do it recursively
      commandUnitExecutorService.cleanup(context.getActivityId(), context.getHost());
      return commandExecutionStatus;
    } catch (Exception ex) {
      commandUnitExecutorService.cleanup(context.getActivityId(), context.getHost());
      throw ex;
    }
  }

  private CommandExecutionStatus executeShellCommand(
      Command command, CommandExecutionContext context, String deploymentType) {
    CommandUnitExecutorService commandUnitExecutorService = commandUnitExecutorServiceMap.get(deploymentType);
    List<CommandUnit> flattenedCommandUnits = getFlattenCommandUnitList(command);
    ScriptType scriptType = getScriptType(flattenedCommandUnits);
    try {
      if (scriptType == ScriptType.BASH) {
        if (context.isInlineSshCommand()) {
          InitSshCommandUnitV2 initCommandUnit = new InitSshCommandUnitV2();
          initCommandUnit.setCommand(command);
          command.getCommandUnits().add(0, initCommandUnit);
        } else {
          InitSshCommandUnit initCommandUnit = new InitSshCommandUnit();
          initCommandUnit.setCommand(command);
          command.getCommandUnits().add(0, initCommandUnit);
        }
        command.getCommandUnits().add(new CleanupSshCommandUnit());
      } else if (scriptType == ScriptType.POWERSHELL) {
        InitPowerShellCommandUnit initPowerShellCommandUnit = new InitPowerShellCommandUnit();
        initPowerShellCommandUnit.setCommand(command);
        command.getCommandUnits().add(0, initPowerShellCommandUnit);
        command.getCommandUnits().add(new CleanupPowerShellCommandUnit());
      }

      return executeShellCommand(commandUnitExecutorService, command, context);
    } finally {
      if (!context.isExecuteOnDelegate()) {
        commandUnitExecutorService.cleanup(context.getActivityId(), context.getHost());
      }
    }
  }
  private List<CommandUnit> getFlattenCommandUnitList(Command command) {
    return command.getCommandUnits()
        .stream()
        .flatMap(commandUnit -> {
          if (CommandUnitType.COMMAND == commandUnit.getCommandUnitType()) {
            return getFlattenCommandUnitList((Command) commandUnit).stream();
          } else {
            return Stream.of(commandUnit);
          }
        })
        .collect(toList());
  }

  private ScriptType getScriptType(List<CommandUnit> commandUnits) {
    if (commandUnits.stream().anyMatch(unit
            -> (unit.getCommandUnitType() == CommandUnitType.EXEC
                   || unit.getCommandUnitType() == CommandUnitType.DOWNLOAD_ARTIFACT)
                && ((ExecCommandUnit) unit).getScriptType() == ScriptType.POWERSHELL)) {
      return ScriptType.POWERSHELL;
    } else {
      return ScriptType.BASH;
    }
  }

  private CommandExecutionStatus executeShellCommand(
      CommandUnitExecutorService commandUnitExecutorService, Command command, CommandExecutionContext context) {
    List<CommandUnit> commandUnits = command.getCommandUnits();

    CommandExecutionStatus commandExecutionStatus = CommandExecutionStatus.FAILURE;

    for (CommandUnit commandUnit : commandUnits) {
      commandExecutionStatus = COMMAND == commandUnit.getCommandUnitType()
          ? executeShellCommand(commandUnitExecutorService, (Command) commandUnit, context)
          : commandUnitExecutorService.execute(commandUnit, context);
      if (FAILURE == commandExecutionStatus) {
        break;
      }
    }

    return commandExecutionStatus;
  }

  private void decryptCredentials(CommandExecutionContext context) {
    if (context.getHostConnectionAttributes() != null) {
      encryptionService.decrypt((EncryptableSetting) context.getHostConnectionAttributes().getValue(),
          context.getHostConnectionCredentials());
    }
    if (context.getBastionConnectionAttributes() != null) {
      encryptionService.decrypt((EncryptableSetting) context.getBastionConnectionAttributes().getValue(),
          context.getBastionConnectionCredentials());
    }
    if (context.getWinrmConnectionAttributes() != null) {
      encryptionService.decrypt(
          context.getWinrmConnectionAttributes(), context.getWinrmConnectionEncryptedDataDetails());
    }
    if (context.getCloudProviderSetting() != null) {
      encryptionService.decrypt(
          (EncryptableSetting) context.getCloudProviderSetting().getValue(), context.getCloudProviderCredentials());
    }
  }
}
