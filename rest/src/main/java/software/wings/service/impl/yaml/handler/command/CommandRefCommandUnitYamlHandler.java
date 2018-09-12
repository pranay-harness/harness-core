package software.wings.service.impl.yaml.handler.command;

import static io.harness.exception.WingsException.USER;
import static software.wings.beans.yaml.YamlConstants.NODE_PROPERTY_REFERENCEID;
import static software.wings.utils.Validator.notNullCheck;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import software.wings.beans.command.AbstractCommandUnit;
import software.wings.beans.command.Command;
import software.wings.beans.command.CommandType;
import software.wings.beans.command.ServiceCommand;
import software.wings.beans.yaml.ChangeContext;
import software.wings.beans.yaml.YamlType;
import software.wings.exception.HarnessException;
import software.wings.service.impl.yaml.handler.YamlHandlerFactory;
import software.wings.service.impl.yaml.service.YamlHelper;
import software.wings.service.intfc.CommandService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.yaml.command.CommandRefYaml;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author rktummala on 11/13/17
 */
@Singleton
public class CommandRefCommandUnitYamlHandler extends CommandUnitYamlHandler<CommandRefYaml, Command> {
  @Inject private ServiceResourceService serviceResourceService;
  @Inject YamlHandlerFactory yamlHandlerFactory;
  @Inject private YamlHelper yamlHelper;
  @Inject private CommandService commandService;

  @Override
  public Class getYamlClass() {
    return CommandRefYaml.class;
  }

  @Override
  protected Command getCommandUnit() {
    return new Command();
  }

  @Override
  public Command upsertFromYaml(ChangeContext<CommandRefYaml> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    Command commandRef = super.toBean(changeContext);
    CommandRefYaml yaml = changeContext.getYaml();
    commandRef.setReferenceId(yaml.getName());
    String filePath = changeContext.getChange().getFilePath();

    String appId = yamlHelper.getAppId(changeContext.getChange().getAccountId(), filePath);
    notNullCheck("Couldn't retrieve app from yaml:" + filePath, appId, USER);
    String serviceId = yamlHelper.getServiceId(appId, filePath);
    notNullCheck("Couldn't retrieve service from yaml:" + filePath, serviceId, USER);

    String commandName = yaml.getName();
    ServiceCommand serviceCommand = serviceResourceService.getCommandByName(appId, serviceId, commandName);

    // This can happen when the command is the same changeset but not yet processed. So we extract it from the changeSet
    // and process it first.
    if (serviceCommand == null) {
      // Get all the commands for the current service
      Optional<ChangeContext> commandContextOptional =
          changeSetContext.stream()
              .filter(context -> {
                if (!context.getYamlType().equals(YamlType.COMMAND)) {
                  return false;
                }

                String commandFilePath = context.getChange().getFilePath();
                String appIdOfCommand = yamlHelper.getAppId(context.getChange().getAccountId(), commandFilePath);
                String serviceIdOfCommand = yamlHelper.getServiceId(appIdOfCommand, commandFilePath);

                if (!(appId.equals(appIdOfCommand) && serviceId.equals(serviceIdOfCommand))) {
                  return false;
                }

                String commandNameFromYamlPath = yamlHelper.getNameFromYamlFilePath(commandFilePath);
                return commandNameFromYamlPath.equals(commandName);
              })
              .findFirst();

      if (commandContextOptional.isPresent()) {
        ChangeContext commandContext = commandContextOptional.get();
        CommandYamlHandler commandYamlHandler = yamlHandlerFactory.getYamlHandler(YamlType.COMMAND);
        commandYamlHandler.upsertFromYaml(commandContext, changeSetContext);
        serviceCommand = serviceResourceService.getCommandByName(appId, serviceId, commandName);
        notNullCheck("No command found with the given name:" + commandName, serviceCommand, USER);

      } else {
        throw new HarnessException("No command with the given name: " + yaml.getName());
      }
    }

    Command commandFromDB =
        commandService.getCommand(appId, serviceCommand.getUuid(), serviceCommand.getDefaultVersion());
    notNullCheck("No command with the given service command id:" + serviceCommand.getUuid(), commandFromDB, USER);

    // Setting the command type as OTHER based on the observation of the value in db in the commandUnit list.
    // To keep it consistent with the normal ui operation, setting it to OTHER. Could be a UI bug.
    commandRef.setCommandType(CommandType.OTHER);
    //    command.setCommandType(serviceCommand.getCommand().getCommandType());
    commandRef.setUuid(commandFromDB.getUuid());
    return commandRef;
  }

  @Override
  public CommandRefYaml toYaml(Command bean, String appId) {
    CommandRefYaml yaml = CommandRefYaml.builder().build();
    super.toYaml(yaml, bean);
    yaml.setName(bean.getReferenceId());
    return yaml;
  }

  @Override
  public Command toBean(AbstractCommandUnit.Yaml yaml) {
    Command commandRef = super.toBean(yaml);
    commandRef.setReferenceId(yaml.getName());
    return commandRef;
  }

  @Override
  protected Map<String, Object> getNodeProperties(ChangeContext<CommandRefYaml> changeContext) {
    Map<String, Object> nodeProperties = super.getNodeProperties(changeContext);
    nodeProperties.put(NODE_PROPERTY_REFERENCEID, changeContext.getYaml().getName());
    return nodeProperties;
  }
}
