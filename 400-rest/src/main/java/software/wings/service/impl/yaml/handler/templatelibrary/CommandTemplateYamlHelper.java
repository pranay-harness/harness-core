/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.yaml.handler.templatelibrary;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static software.wings.beans.Graph.graphIdGenerator;
import static software.wings.beans.command.CommandUnitType.COMMAND;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.yaml.BaseYaml;

import software.wings.beans.Graph;
import software.wings.beans.GraphLink;
import software.wings.beans.GraphNode;
import software.wings.beans.command.AbstractCommandUnit;
import software.wings.beans.command.CommandType;
import software.wings.beans.command.CommandUnit;
import software.wings.beans.template.BaseTemplate;
import software.wings.beans.template.command.SshCommandTemplate;
import software.wings.beans.yaml.Change;
import software.wings.beans.yaml.ChangeContext;
import software.wings.beans.yaml.YamlConstants;
import software.wings.common.TemplateConstants;
import software.wings.service.impl.yaml.handler.command.CommandUnitYamlHandler;
import software.wings.utils.Utils;
import software.wings.yaml.YamlHelper;
import software.wings.yaml.templatelibrary.CommandTemplateYaml;
import software.wings.yaml.templatelibrary.TemplateLibraryYaml;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(CDC)
@Singleton
@Slf4j
public class CommandTemplateYamlHelper {
  private Map<String, CommandUnitYamlHandler> commandUnitYamlHandlerMap;

  @Inject
  public CommandTemplateYamlHelper(Map<String, CommandUnitYamlHandler> commandUnitYamlHandlerMap) {
    this.commandUnitYamlHandlerMap = commandUnitYamlHandlerMap;
  }

  public BaseTemplate getBaseTemplate(
      String graphName, ChangeContext changeContext, List<ChangeContext> changeSetContext) {
    return toBean(changeContext, graphName, changeSetContext);
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
        CommandUnitYamlHandler commandUnitYamlHandler = getCommandUnitYamlHandler(commandUnitYaml);
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

  private CommandUnitYamlHandler getCommandUnitYamlHandler(AbstractCommandUnit.Yaml commandUnitYaml) {
    return commandUnitYamlHandlerMap.get(getCommandUnitSubTypeFromYaml(commandUnitYaml));
  }

  protected <Y extends TemplateLibraryYaml> ChangeContext.Builder cloneFileChangeContext(
      ChangeContext<Y> context, BaseYaml yaml) {
    Change change = context.getChange();
    Change.Builder clonedChange = change.toBuilder();
    clonedChange.withFileContent(YamlHelper.toYamlString(yaml));

    ChangeContext.Builder clonedContext = context.toBuilder();
    clonedContext.withChange(clonedChange.build());
    clonedContext.withYaml(yaml);
    return clonedContext;
  }

  private String getLinkId() {
    return graphIdGenerator(YamlConstants.LINK_PREFIX);
  }
  private String getCommandUnitSubTypeFromYaml(AbstractCommandUnit.Yaml commandUnitYaml) {
    if (commandUnitYaml.getCommandUnitType().equals(COMMAND.name())) {
      return TemplateConstants.TEMPLATE_REF_COMMAND;
    }
    return commandUnitYaml.getCommandUnitType();
  }

  public Class<? extends TemplateLibraryYaml> getYamlClass() {
    return CommandTemplateYaml.class;
  }
}
