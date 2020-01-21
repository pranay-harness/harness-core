package software.wings.service.impl.yaml.handler.templatelibrary;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.stream.Collectors.toList;
import static software.wings.beans.command.CommandUnitType.COMMAND;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.data.structure.UUIDGenerator;
import io.harness.exception.InvalidRequestException;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.Graph;
import software.wings.beans.GraphLink;
import software.wings.beans.GraphNode;
import software.wings.beans.command.AbstractCommandUnit;
import software.wings.beans.command.CommandType;
import software.wings.beans.command.CommandUnit;
import software.wings.beans.template.Template;
import software.wings.beans.template.command.SshCommandTemplate;
import software.wings.beans.yaml.ChangeContext;
import software.wings.beans.yaml.YamlConstants;
import software.wings.beans.yaml.YamlType;
import software.wings.common.TemplateConstants;
import software.wings.service.impl.yaml.handler.YamlHandlerFactory;
import software.wings.service.impl.yaml.handler.command.CommandUnitYamlHandler;
import software.wings.utils.Utils;
import software.wings.yaml.templatelibrary.CommandTemplateYaml;

import java.util.List;

@Singleton
@Slf4j
public class CommandTemplateYamlHandler extends TemplateLibraryYamlHandler<CommandTemplateYaml> {
  @Inject YamlHandlerFactory yamlHandlerFactory;

  @Override
  protected void setBaseTemplate(
      Template template, ChangeContext<CommandTemplateYaml> changeContext, List<ChangeContext> changeSetContext) {
    try {
      template.setTemplateObject(toBean(changeContext, template.getName(), changeSetContext));
    } catch (Exception e) {
      throw new InvalidRequestException("Invalid Yaml.", e);
    }
  }

  @Override
  public CommandTemplateYaml toYaml(Template bean, String appId) {
    SshCommandTemplate command = (SshCommandTemplate) bean.getTemplateObject();
    String commandUnitType = Utils.getStringFromEnum(command.getCommandType());
    // commmand units
    List<AbstractCommandUnit.Yaml> commandUnitYamlList =
        command.getCommandUnits()
            .stream()
            .map(commandUnit -> {
              CommandUnitYamlHandler commandUnitsYamlHandler =
                  yamlHandlerFactory.getYamlHandler(YamlType.COMMAND_UNIT, getCommandUnitSubTypeFromBean(commandUnit));
              return (AbstractCommandUnit.Yaml) commandUnitsYamlHandler.toYaml(commandUnit, appId);
            })
            .collect(toList());

    CommandTemplateYaml commandTemplateYaml =
        CommandTemplateYaml.builder().commandUnits(commandUnitYamlList).commandUnitType(commandUnitType).build();
    super.toYaml(commandTemplateYaml, bean);
    return commandTemplateYaml;
  }

  private SshCommandTemplate toBean(ChangeContext changeContext, String name, List<ChangeContext> changeSetContext) {
    CommandTemplateYaml commandYaml = (CommandTemplateYaml) changeContext.getYaml();
    List<GraphNode> nodeList = Lists.newArrayList();
    List<AbstractCommandUnit.Yaml> commandUnitYamlList = commandYaml.getCommandUnits();
    List<CommandUnit> commandUnitList = Lists.newArrayList();
    List<GraphLink> linkList = Lists.newArrayList();
    Graph.Builder graphBuilder = Graph.Builder.aGraph().withGraphName(name);
    if (isNotEmpty(commandUnitYamlList)) {
      GraphNode previousGraphNode = null;
      for (AbstractCommandUnit.Yaml commandUnitYaml : commandUnitYamlList) {
        CommandUnitYamlHandler commandUnitYamlHandler =
            yamlHandlerFactory.getYamlHandler(YamlType.COMMAND_UNIT, getCommandUnitSubTypeFromYaml(commandUnitYaml));
        ChangeContext commandUnitChangeContext = cloneFileChangeContext(changeContext, commandUnitYaml).build();
        CommandUnit commandUnit;
        try {
          commandUnit = commandUnitYamlHandler.upsertFromYaml(commandUnitChangeContext, changeSetContext);
        } catch (Exception e) {
          throw new InvalidRequestException("Invalid Yaml.", e);
        }
        commandUnitList.add(commandUnit);
        GraphNode graphNode = commandUnitYamlHandler.getGraphNode(commandUnitChangeContext, previousGraphNode);
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

    CommandType commandType = Utils.getEnumFromString(CommandType.class, commandYaml.getType());
    if (commandType == null) {
      commandType = CommandType.OTHER;
    }
    return SshCommandTemplate.builder().commandUnits(commandUnitList).commandType(commandType).build();
  }

  private String getLinkId() {
    return UUIDGenerator.graphIdGenerator(YamlConstants.LINK_PREFIX);
  }

  private String getCommandUnitSubTypeFromYaml(AbstractCommandUnit.Yaml commandUnitYaml) {
    if (commandUnitYaml.getCommandUnitType().equals(COMMAND.name())) {
      return TemplateConstants.TEMPLATE_REF_COMMAND;
    }
    return commandUnitYaml.getCommandUnitType();
  }

  private String getCommandUnitSubTypeFromBean(CommandUnit commandUnit) {
    if (commandUnit.getCommandUnitType().equals(COMMAND)) {
      return TemplateConstants.TEMPLATE_REF_COMMAND;
    }
    return commandUnit.getCommandUnitType().name();
  }
}
