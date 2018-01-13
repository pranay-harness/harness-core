package software.wings.service.impl.yaml.handler.command;

import static software.wings.beans.yaml.YamlConstants.PATH_DELIMITER;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;

import software.wings.beans.EntityVersion;
import software.wings.beans.Environment;
import software.wings.beans.Graph;
import software.wings.beans.Graph.Link;
import software.wings.beans.Graph.Node;
import software.wings.beans.command.AbstractCommandUnit;
import software.wings.beans.command.AbstractCommandUnit.Yaml;
import software.wings.beans.command.Command;
import software.wings.beans.command.Command.Builder;
import software.wings.beans.command.CommandType;
import software.wings.beans.command.CommandUnit;
import software.wings.beans.command.ServiceCommand;
import software.wings.beans.yaml.ChangeContext;
import software.wings.beans.yaml.YamlConstants;
import software.wings.beans.yaml.YamlType;
import software.wings.common.UUIDGenerator;
import software.wings.exception.HarnessException;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;
import software.wings.service.impl.yaml.handler.YamlHandlerFactory;
import software.wings.service.impl.yaml.service.YamlHelper;
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
  @Inject YamlHelper yamlHelper;
  @Inject ServiceResourceService serviceResourceService;
  @Inject CommandService commandService;
  @Inject EnvironmentService environmentService;

  private ServiceCommand toBean(ChangeContext<CommandYaml> changeContext, List<ChangeContext> changeSetContext,
      boolean isCreate) throws HarnessException {
    String accountId = changeContext.getChange().getAccountId();
    String yamlFilePath = changeContext.getChange().getFilePath();
    String appId = yamlHelper.getAppId(accountId, yamlFilePath);
    Validator.notNullCheck("appId is null for given yamlFilePath: " + yamlFilePath, appId);
    String serviceId = yamlHelper.getServiceId(appId, yamlFilePath);
    Validator.notNullCheck("serviceId is null for given yamlFilePath: " + yamlFilePath, serviceId);
    CommandYaml commandYaml = changeContext.getYaml();
    List<Node> nodeList = Lists.newArrayList();
    List<Yaml> commandUnitYamlList = commandYaml.getCommandUnits();
    List<CommandUnit> commandUnitList = Lists.newArrayList();
    List<Link> linkList = Lists.newArrayList();
    String name = yamlHelper.getNameFromYamlFilePath(yamlFilePath);
    Graph.Builder graphBuilder = Graph.Builder.aGraph().withGraphName(name);

    if (!Util.isEmpty(commandUnitYamlList)) {
      Node previousGraphNode = null;

      for (Yaml commandUnitYaml : commandUnitYamlList) {
        CommandUnitYamlHandler commandUnitYamlHandler = (CommandUnitYamlHandler) yamlHandlerFactory.getYamlHandler(
            YamlType.COMMAND_UNIT, commandUnitYaml.getCommandUnitType());
        ChangeContext.Builder clonedContext = cloneFileChangeContext(changeContext, commandUnitYaml);
        CommandUnit commandUnit = commandUnitYamlHandler.upsertFromYaml(clonedContext.build(), changeSetContext);
        commandUnitList.add(commandUnit);
        Node graphNode = commandUnitYamlHandler.getGraphNode(clonedContext.build(), previousGraphNode);
        if (previousGraphNode != null) {
          Link link = Link.Builder.aLink()
                          .withType("SUCCESS")
                          .withFrom(previousGraphNode.getId())
                          .withTo(graphNode.getId())
                          .withId(getLinkId())
                          .build();
          linkList.add(link);
        }
        previousGraphNode = graphNode;
        nodeList.add(graphNode);
      }

      if (!Util.isEmpty(linkList)) {
        graphBuilder.withLinks(linkList);
      }
      graphBuilder.withNodes(nodeList);
    }

    CommandType commandType = Util.getEnumFromString(CommandType.class, commandYaml.getType());

    Command command = Builder.aCommand()
                          .withCommandType(commandType)
                          //                          .withCommandUnits(commandUnitList)
                          .withName(name)
                          .withGraph(graphBuilder.build())
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

    ServiceCommand serviceCommand;
    ServiceCommand.Builder builder = ServiceCommand.Builder.aServiceCommand();
    if (!isCreate) {
      ServiceCommand existingSvcCommand = serviceResourceService.getCommandByName(appId, serviceId, name);
      Validator.notNullCheck("Service command with the given name doesn't exist: " + name, existingSvcCommand);
      builder.withUuid(existingSvcCommand.getUuid());
    }

    serviceCommand = builder.withAppId(appId)
                         .withCommand(command)
                         .withEnvIdVersionMap(envIdMap)
                         .withName(name)
                         .withServiceId(serviceId)
                         .withTargetToAllEnv(commandYaml.isTargetToAllEnv())
                         .withSetAsDefault(true)
                         .build();

    if (isCreate) {
      serviceResourceService.addCommand(appId, serviceId, serviceCommand, false);
    } else {
      serviceResourceService.updateCommand(appId, serviceId, serviceCommand);
    }

    return commandService.getServiceCommandByName(appId, serviceId, name);
  }

  private String getLinkId() {
    return UUIDGenerator.graphIdGenerator(YamlConstants.LINK_PREFIX);
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

    return CommandYaml.builder()
        .commandUnits(commandUnitYamlList)
        .commandUnitType(commandUnitType)
        .targetEnvs(envNameList)
        .targetToAllEnv(serviceCommand.isTargetToAllEnv())
        .type(commandType)
        .harnessApiVersion(getHarnessApiVersion())
        .build();
  }

  @Override
  public ServiceCommand upsertFromYaml(ChangeContext<CommandYaml> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    String accountId = changeContext.getChange().getAccountId();

    ServiceCommand previous = get(accountId, changeContext.getChange().getFilePath());
    if (previous != null) {
      return toBean(changeContext, changeSetContext, false);
    } else {
      return toBean(changeContext, changeSetContext, true);
    }
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
    String appId = yamlHelper.getAppId(accountId, yamlFilePath);
    Validator.notNullCheck("appId is null for given yamlFilePath: " + yamlFilePath, appId);
    String serviceId = yamlHelper.getServiceId(appId, yamlFilePath);
    Validator.notNullCheck("serviceId is null for given yamlFilePath: " + yamlFilePath, serviceId);
    String commandName =
        yamlHelper.extractEntityNameFromYamlPath(YamlType.COMMAND.getPathExpression(), yamlFilePath, PATH_DELIMITER);
    Validator.notNullCheck("commandName is null for given yamlFilePath: " + yamlFilePath, commandName);
    return serviceResourceService.getCommandByName(appId, serviceId, commandName);
  }

  @Override
  public void delete(ChangeContext<CommandYaml> changeContext) throws HarnessException {
    ServiceCommand serviceCommand =
        get(changeContext.getChange().getAccountId(), changeContext.getChange().getFilePath());
    if (serviceCommand != null) {
      serviceResourceService.deleteCommand(
          serviceCommand.getAppId(), serviceCommand.getServiceId(), serviceCommand.getUuid());
    }
  }
}
