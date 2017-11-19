package software.wings.service.impl.yaml.handler.command;

import com.google.inject.Inject;

import software.wings.beans.command.Command;
import software.wings.beans.command.CommandType;
import software.wings.beans.command.ServiceCommand;
import software.wings.beans.yaml.Change.ChangeType;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;
import software.wings.service.impl.yaml.sync.YamlSyncHelper;
import software.wings.service.intfc.CommandService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.utils.Util;
import software.wings.utils.Validator;
import software.wings.yaml.command.CommandRefYaml;
import software.wings.yaml.command.CommandRefYaml.Builder;

import java.util.List;

/**
 * @author rktummala on 11/13/17
 */
public class CommandRefCommandUnitYamlHandler extends CommandUnitYamlHandler<CommandRefYaml, Command, Builder> {
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private YamlSyncHelper yamlSyncHelper;
  @Inject private CommandService commandService;

  @Override
  public Class getYamlClass() {
    return CommandRefYaml.class;
  }

  @Override
  protected Builder getYamlBuilder() {
    return Builder.aYaml();
  }

  @Override
  protected Command getCommandUnit() {
    return new Command();
  }

  @Override
  public Command createFromYaml(ChangeContext<CommandRefYaml> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    Command commandRef = super.createFromYaml(changeContext, changeSetContext);
    return setWithYamlValues(commandRef, changeContext, changeSetContext);
  }

  @Override
  public Command updateFromYaml(ChangeContext<CommandRefYaml> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    Command commandRef = super.updateFromYaml(changeContext, changeSetContext);
    return setWithYamlValues(commandRef, changeContext, changeSetContext);
  }

  private Command setWithYamlValues(Command commandRef, ChangeContext<CommandRefYaml> changeContext,
      List<ChangeContext> changeSetContext) throws HarnessException {
    CommandRefYaml yaml = changeContext.getYaml();
    commandRef.setReferenceId(yaml.getName());
    String filePath = changeContext.getChange().getFilePath();

    String appId = yamlSyncHelper.getAppId(changeContext.getChange().getAccountId(), filePath);
    Validator.notNullCheck("Couldn't retrieve app from yaml:" + filePath, appId);
    String serviceId = yamlSyncHelper.getServiceId(appId, filePath);
    Validator.notNullCheck("Couldn't retrieve service from yaml:" + filePath, serviceId);

    ServiceCommand serviceCommand = serviceResourceService.getCommandByName(appId, serviceId, yaml.getName());
    Validator.notNullCheck("Couldn't retrieve service command with the given name: " + yaml.getName(), serviceCommand);

    Command commandFromDB =
        commandService.getCommand(appId, serviceCommand.getUuid(), serviceCommand.getDefaultVersion());
    Validator.notNullCheck("No command with the given service command id:" + serviceCommand.getUuid(), commandFromDB);

    // Setting the command type as OTHER based on the observation of the value in db in the commandUnit list.
    // To keep it consistent with the normal ui operation, setting it to OTHER. Could be a UI bug.
    commandRef.setCommandType(CommandType.OTHER);
    //    command.setCommandType(serviceCommand.getCommand().getCommandType());
    commandRef.setUuid(commandFromDB.getUuid());
    return commandRef;
  }

  @Override
  public CommandRefYaml toYaml(Command bean, String appId) {
    String commandUnitType = Util.getStringFromEnum(bean.getCommandUnitType());
    return getYamlBuilder()
        .withCommandUnitType(commandUnitType)
        .withDeploymentType(bean.getDeploymentType())
        .withName(bean.getReferenceId())
        .build();
  }

  @Override
  public Command upsertFromYaml(ChangeContext<CommandRefYaml> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    if (changeContext.getChange().getChangeType().equals(ChangeType.ADD)) {
      return createFromYaml(changeContext, changeSetContext);
    } else {
      return updateFromYaml(changeContext, changeSetContext);
    }
  }
}
