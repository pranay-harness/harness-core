package software.wings.service.impl.yaml.handler.command;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.stream.Collectors.toList;
import static software.wings.beans.yaml.YamlConstants.PATH_DELIMITER;
import static software.wings.exception.WingsException.USER;
import static software.wings.utils.Validator.notNullCheck;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.data.structure.UUIDGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Application;
import software.wings.beans.EntityVersion;
import software.wings.beans.Environment;
import software.wings.beans.Graph;
import software.wings.beans.GraphLink;
import software.wings.beans.GraphNode;
import software.wings.beans.Service;
import software.wings.beans.command.AbstractCommandUnit;
import software.wings.beans.command.AbstractCommandUnit.Yaml;
import software.wings.beans.command.Command;
import software.wings.beans.command.Command.Builder;
import software.wings.beans.command.CommandType;
import software.wings.beans.command.CommandUnit;
import software.wings.beans.command.ServiceCommand;
import software.wings.beans.template.TemplateHelper;
import software.wings.beans.yaml.ChangeContext;
import software.wings.beans.yaml.YamlConstants;
import software.wings.beans.yaml.YamlType;
import software.wings.exception.HarnessException;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;
import software.wings.service.impl.yaml.handler.YamlHandlerFactory;
import software.wings.service.impl.yaml.service.YamlHelper;
import software.wings.service.intfc.CommandService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.template.TemplateService;
import software.wings.utils.Util;
import software.wings.yaml.command.CommandYaml;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 *  @author rktummala on 11/13/17
 */
@Singleton
public class CommandYamlHandler extends BaseYamlHandler<CommandYaml, ServiceCommand> {
  private static final Logger logger = LoggerFactory.getLogger(CommandYamlHandler.class);
  @Inject YamlHandlerFactory yamlHandlerFactory;
  @Inject YamlHelper yamlHelper;
  @Inject ServiceResourceService serviceResourceService;
  @Inject CommandService commandService;
  @Inject EnvironmentService environmentService;
  @Inject TemplateService templateService;

  private ServiceCommand toBean(ChangeContext<CommandYaml> changeContext, List<ChangeContext> changeSetContext,
      boolean isCreate) throws HarnessException {
    String accountId = changeContext.getChange().getAccountId();
    String yamlFilePath = changeContext.getChange().getFilePath();

    String appId = yamlHelper.getAppId(accountId, yamlFilePath);
    notNullCheck("appId is null for given yamlFilePath: " + yamlFilePath, appId, USER);
    String serviceId = yamlHelper.getServiceId(appId, yamlFilePath);
    notNullCheck("serviceId is null for given yamlFilePath: " + yamlFilePath, serviceId, USER);
    CommandYaml commandYaml = changeContext.getYaml();
    List<GraphNode> nodeList = Lists.newArrayList();
    List<Yaml> commandUnitYamlList = commandYaml.getCommandUnits();
    List<CommandUnit> commandUnitList = Lists.newArrayList();
    List<GraphLink> linkList = Lists.newArrayList();
    String name = yamlHelper.getNameFromYamlFilePath(yamlFilePath);
    Graph.Builder graphBuilder = Graph.Builder.aGraph().withGraphName(name);

    if (isNotEmpty(commandUnitYamlList)) {
      GraphNode previousGraphNode = null;
      for (Yaml commandUnitYaml : commandUnitYamlList) {
        CommandUnitYamlHandler commandUnitYamlHandler =
            yamlHandlerFactory.getYamlHandler(YamlType.COMMAND_UNIT, commandUnitYaml.getCommandUnitType());
        ChangeContext.Builder clonedContext = cloneFileChangeContext(changeContext, commandUnitYaml);
        CommandUnit commandUnit = commandUnitYamlHandler.upsertFromYaml(clonedContext.build(), changeSetContext);
        commandUnitList.add(commandUnit);
        GraphNode graphNode = commandUnitYamlHandler.getGraphNode(clonedContext.build(), previousGraphNode);
        if (previousGraphNode != null) {
          GraphLink link = GraphLink.Builder.aLink()
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

      if (isNotEmpty(linkList)) {
        graphBuilder.withLinks(linkList);
      }
      graphBuilder.withNodes(nodeList);
    }

    CommandType commandType = Util.getEnumFromString(CommandType.class, commandYaml.getType());

    Command command =
        Builder.aCommand().withCommandType(commandType).withCommandUnits(commandUnitList).withName(name).build();

    List<String> envNameList = commandYaml.getTargetEnvs();
    Map<String, EntityVersion> envIdMap = Maps.newHashMap();
    if (isNotEmpty(envNameList)) {
      envNameList.forEach(envName -> {
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
      notNullCheck("Service command with the given name doesn't exist: " + name, existingSvcCommand, USER);
      builder.withUuid(existingSvcCommand.getUuid());
      builder.withTemplateUuid(existingSvcCommand.getTemplateUuid());
      builder.withTemplateVersion(existingSvcCommand.getTemplateVersion());
      if (isNotEmpty(commandYaml.getTemplateUri())) {
        builder.withTemplateVersion(TemplateHelper.obtainTemplateVersion(commandYaml.getTemplateUri()));
      }
    } else {
      if (isNotEmpty(commandYaml.getTemplateUri())) {
        builder.withTemplateUuid(templateService.fetchTemplateIdFromUri(accountId, commandYaml.getTemplateUri()));
      }
    }

    command.setTemplateVariables(TemplateHelper.convertToEntityVariables(commandYaml.getTemplateVariables()));

    serviceCommand = builder.withAppId(appId)
                         .withCommand(command)
                         .withEnvIdVersionMap(envIdMap)
                         .withName(name)
                         .withServiceId(serviceId)
                         .withTargetToAllEnv(commandYaml.isTargetToAllEnv())
                         .withSetAsDefault(true)
                         .build();

    if (isCreate) {
      serviceResourceService.addCommand(appId, serviceId, serviceCommand, true);
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
    notNullCheck("command is null for serviceCommand:" + serviceCommand.getName(), command, USER);

    String commandType = Util.getStringFromEnum(command.getCommandType());
    String commandUnitType = Util.getStringFromEnum(command.getCommandUnitType());

    // command units
    List<AbstractCommandUnit.Yaml> commandUnitYamlList =
        command.getCommandUnits()
            .stream()
            .map(commandUnit -> {
              CommandUnitYamlHandler commandUnitsYamlHandler =
                  yamlHandlerFactory.getYamlHandler(YamlType.COMMAND_UNIT, commandUnit.getCommandUnitType().name());
              return (AbstractCommandUnit.Yaml) commandUnitsYamlHandler.toYaml(commandUnit, appId);
            })
            .collect(toList());

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
                                   .collect(toList());
      if (isNotEmpty(envIdList)) {
        envIdList.forEach(envId -> {
          Environment environment = environmentService.get(appId, envId, false);
          if (environment != null) {
            envNameList.add(environment.getName());
          }
        });
      }
    }
    String templateUri = null;
    final String templateUuid = serviceCommand.getTemplateUuid();
    if (templateUuid != null) {
      // Command is linked
      templateUri = templateService.fetchTemplateUri(templateUuid);
      if (templateUri == null) {
        logger.error("Linked template for service command {} was deleted ", serviceCommand.getUuid());
      }
      if (serviceCommand.getTemplateVersion() != null) {
        templateUri = templateUri + ":" + serviceCommand.getTemplateVersion();
      }
    }

    return CommandYaml.builder()
        .commandUnits(commandUnitYamlList)
        .commandUnitType(commandUnitType)
        .targetEnvs(envNameList)
        .targetToAllEnv(serviceCommand.isTargetToAllEnv())
        .type(commandType)
        .templateUri(templateUri)
        .templateVariables(TemplateHelper.convertToTemplateVariables(command.getTemplateVariables()))
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
  public Class getYamlClass() {
    return CommandYaml.class;
  }

  @Override
  public ServiceCommand get(String accountId, String yamlFilePath) {
    String appId = yamlHelper.getAppId(accountId, yamlFilePath);
    notNullCheck("appId is null for given yamlFilePath: " + yamlFilePath, appId, USER);
    String serviceId = yamlHelper.getServiceId(appId, yamlFilePath);
    notNullCheck("serviceId is null for given yamlFilePath: " + yamlFilePath, serviceId, USER);
    String commandName =
        yamlHelper.extractEntityNameFromYamlPath(YamlType.COMMAND.getPathExpression(), yamlFilePath, PATH_DELIMITER);
    notNullCheck("commandName is null for given yamlFilePath: " + yamlFilePath, commandName, USER);
    return serviceResourceService.getCommandByName(appId, serviceId, commandName);
  }

  private ServiceCommand getServiceCommand(String applicationId, String serviceId, String yamlFilePath) {
    String commandName =
        yamlHelper.extractEntityNameFromYamlPath(YamlType.COMMAND.getPathExpression(), yamlFilePath, PATH_DELIMITER);
    notNullCheck("commandName is null for given yamlFilePath: " + yamlFilePath, commandName, USER);
    return serviceResourceService.getCommandByName(applicationId, serviceId, commandName);
  }

  @Override
  public void delete(ChangeContext<CommandYaml> changeContext) throws HarnessException {
    String yamlFilePath = changeContext.getChange().getFilePath();
    String accountId = changeContext.getChange().getAccountId();
    Optional<Application> optionalApplication = yamlHelper.getApplicationIfPresent(accountId, yamlFilePath);
    if (!optionalApplication.isPresent()) {
      return;
    }

    Optional<Service> serviceOptional =
        yamlHelper.getServiceIfPresent(optionalApplication.get().getUuid(), yamlFilePath);
    if (!serviceOptional.isPresent()) {
      return;
    }

    ServiceCommand serviceCommand =
        getServiceCommand(optionalApplication.get().getUuid(), serviceOptional.get().getUuid(), yamlFilePath);
    if (serviceCommand != null) {
      serviceResourceService.deleteCommand(
          serviceCommand.getAppId(), serviceCommand.getServiceId(), serviceCommand.getUuid());
    }
  }
}
