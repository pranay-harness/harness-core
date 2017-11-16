package software.wings.service.impl.yaml.handler.command;

import static software.wings.beans.yaml.YamlConstants.PATH_DELIMITER;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;

import software.wings.beans.EntityVersion;
import software.wings.beans.Environment;
import software.wings.beans.command.AbstractCommandUnit;
import software.wings.beans.command.AbstractCommandUnit.Yaml;
import software.wings.beans.command.Command;
import software.wings.beans.command.Command.Builder;
import software.wings.beans.command.CommandType;
import software.wings.beans.command.CommandUnit;
import software.wings.beans.command.ServiceCommand;
import software.wings.beans.yaml.ChangeContext;
import software.wings.beans.yaml.YamlType;
import software.wings.exception.HarnessException;
import software.wings.exception.WingsException;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;
import software.wings.service.impl.yaml.handler.YamlHandlerFactory;
import software.wings.service.impl.yaml.sync.YamlSyncHelper;
import software.wings.service.intfc.CommandService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.utils.Util;
import software.wings.utils.Validator;
import software.wings.yaml.command.CommandYaml;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 *  @author rktummala on 11/13/17
 */
public class CommandYamlHandler extends BaseYamlHandler<CommandYaml, ServiceCommand> {
  @Inject YamlHandlerFactory yamlHandlerFactory;
  @Inject YamlSyncHelper yamlSyncHelper;
  @Inject ServiceResourceService serviceResourceService;
  @Inject CommandService commandService;
  @Inject EnvironmentService environmentService;

  @Override
  public ServiceCommand createFromYaml(ChangeContext<CommandYaml> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    return setWithYamlValues(changeContext, changeSetContext, true);
  }

  private ServiceCommand setWithYamlValues(ChangeContext<CommandYaml> changeContext,
      List<ChangeContext> changeSetContext, boolean isCreate) throws HarnessException {
    String accountId = changeContext.getChange().getAccountId();
    String yamlFilePath = changeContext.getChange().getFilePath();
    String appId = yamlSyncHelper.getAppId(accountId, yamlFilePath);
    Validator.notNullCheck("appId is null for given yamlFilePath: " + yamlFilePath, appId);
    String serviceId = yamlSyncHelper.getServiceId(appId, yamlFilePath);
    Validator.notNullCheck("serviceId is null for given yamlFilePath: " + yamlFilePath, serviceId);
    CommandYaml commandYaml = changeContext.getYaml();

    List<Yaml> commandUnits = commandYaml.getCommandUnits();
    List<CommandUnit> commandUnitList = Lists.newArrayList();
    if (!Util.isEmpty(commandUnits)) {
      try {
        commandUnitList =
            commandUnits.stream()
                .map(commandUnitYaml -> {
                  try {
                    BaseYamlHandler phaseYamlHandler =
                        yamlHandlerFactory.getYamlHandler(YamlType.COMMAND, commandUnitYaml.getCommandUnitType());
                    ChangeContext.Builder clonedContext = cloneFileChangeContext(changeContext, commandUnitYaml);
                    CommandUnit commandUnit = (CommandUnit) createOrUpdateFromYaml(
                        isCreate, phaseYamlHandler, clonedContext.build(), changeSetContext);
                    return commandUnit;
                  } catch (HarnessException e) {
                    throw new WingsException(e);
                  }
                })
                .collect(Collectors.toList());
      } catch (WingsException ex) {
        throw new HarnessException(ex);
      }
    }

    CommandType commandType = Util.getEnumFromString(CommandType.class, commandYaml.getType());
    Command command = Builder.aCommand()
                          .withCommandType(commandType)
                          .withCommandUnits(commandUnitList)
                          .withName(commandYaml.getName())
                          .build();

    List<String> envNameList = commandYaml.getTargetEnvs();
    Map<String, EntityVersion> envIdMap = Maps.newHashMap();
    if (!Util.isEmpty(envNameList)) {
      envNameList.stream().forEach(envName -> {
        Environment environment = environmentService.getEnvironmentByName(appId, envName);
        if (environment != null) {
          envIdMap.put(environment.getUuid(), null);
        }
      });
    }

    ServiceCommand serviceCommand = ServiceCommand.Builder.aServiceCommand()
                                        .withAppId(appId)
                                        .withCommand(command)
                                        .withEnvIdVersionMap(envIdMap)
                                        .withName(commandYaml.getName())
                                        .withServiceId(serviceId)
                                        .withTargetToAllEnv(commandYaml.isTargetToAllEnv())
                                        .build();
    if (isCreate) {
      serviceResourceService.addCommand(appId, serviceId, serviceCommand, false);
    } else {
      serviceResourceService.updateCommand(appId, serviceId, serviceCommand);
    }
    return commandService.getServiceCommandByName(appId, serviceId, commandYaml.getName());
  }

  @Override
  public CommandYaml toYaml(ServiceCommand serviceCommand, String appId) {
    Command command = serviceCommand.getCommand();
    Validator.notNullCheck("command is null for serviceCommand:" + serviceCommand.getName(), command);

    String commandType = Util.getStringFromEnum(command.getCommandType());
    String commandUnitType = Util.getStringFromEnum(command.getCommandUnitType());

    // command units

    List<AbstractCommandUnit.Yaml> commandUnitYamlList =
        command.getCommandUnits()
            .stream()
            .map(commandUnit -> {
              BaseYamlHandler commandUnitsYamlHandler =
                  yamlHandlerFactory.getYamlHandler(YamlType.COMMAND_UNIT, commandUnit.getCommandUnitType().name());
              return (AbstractCommandUnit.Yaml) commandUnitsYamlHandler.toYaml(commandUnit, appId);
            })
            .collect(Collectors.toList());

    // target environments
    Map<String, EntityVersion> envIdVersionMap = serviceCommand.getEnvIdVersionMap();
    final List<String> envNameList = Lists.newArrayList();
    if (envIdVersionMap != null) {
      // Find all the envs that are configured to use the default version. If the env is configured to use default
      // version, the value is null.
      List<String> envIdList = envIdVersionMap.entrySet()
                                   .stream()
                                   .filter(entry -> entry.getValue() == null)
                                   .map(entry -> entry.getKey())
                                   .collect(Collectors.toList());
      if (!Util.isEmpty(envIdList)) {
        envIdList.stream().forEach(envId -> {
          Environment environment = environmentService.get(appId, envId, false);
          if (environment != null) {
            envNameList.add(environment.getName());
          }
        });
      }
    }

    return CommandYaml.Builder.aYaml()
        .withCommandUnits(commandUnitYamlList)
        .withCommandUnitType(commandUnitType)
        .withName(serviceCommand.getName())
        .withTargetEnvs(envNameList)
        .withTargetToAllEnv(serviceCommand.isTargetToAllEnv())
        .withType(commandType)
        .build();
  }

  @Override
  public ServiceCommand updateFromYaml(ChangeContext<CommandYaml> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    return setWithYamlValues(changeContext, changeSetContext, false);
  }

  @Override
  public boolean validate(ChangeContext<CommandYaml> changeContext, List<ChangeContext> changeSetContext) {
    CommandYaml yaml = changeContext.getYaml();
    return !(yaml == null || yaml.getCommandUnitType() == null || yaml.getType() == null);
  }

  @Override
  public Class getYamlClass() {
    return CommandYaml.class;
  }

  @Override
  public ServiceCommand get(String accountId, String yamlFilePath) {
    String appId = yamlSyncHelper.getAppId(accountId, yamlFilePath);
    Validator.notNullCheck("appId is null for given yamlFilePath: " + yamlFilePath, appId);
    String serviceId = yamlSyncHelper.getServiceId(appId, yamlFilePath);
    Validator.notNullCheck("serviceId is null for given yamlFilePath: " + yamlFilePath, serviceId);
    String commandName = yamlSyncHelper.extractEntityNameFromYamlPath(
        YamlType.COMMAND.getPathExpression(), yamlFilePath, PATH_DELIMITER);
    Validator.notNullCheck("commandName is null for given yamlFilePath: " + yamlFilePath, commandName);
    return serviceResourceService.getCommandByName(appId, serviceId, commandName);
  }
}
