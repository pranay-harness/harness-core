/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.yaml.handler.templatelibrary;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import static software.wings.beans.command.CommandUnitType.COMMAND;

import static java.util.stream.Collectors.toList;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;

import software.wings.beans.command.AbstractCommandUnit;
import software.wings.beans.command.CommandUnit;
import software.wings.beans.template.BaseTemplate;
import software.wings.beans.template.Template;
import software.wings.beans.template.command.SshCommandTemplate;
import software.wings.beans.yaml.ChangeContext;
import software.wings.beans.yaml.YamlType;
import software.wings.common.TemplateConstants;
import software.wings.service.impl.yaml.handler.YamlHandlerFactory;
import software.wings.service.impl.yaml.handler.command.CommandUnitYamlHandler;
import software.wings.utils.Utils;
import software.wings.yaml.templatelibrary.CommandTemplateYaml;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(CDC)
@Singleton
@Slf4j
public class CommandTemplateYamlHandler extends TemplateLibraryYamlHandler<CommandTemplateYaml> {
  @Inject YamlHandlerFactory yamlHandlerFactory;
  @Inject CommandTemplateYamlHelper commandTemplateYamlHelper;

  @Override
  protected void setBaseTemplate(
      Template template, ChangeContext<CommandTemplateYaml> changeContext, List<ChangeContext> changeSetContext) {
    try {
      template.setTemplateObject(getBaseTemplate(template.getName(), changeContext, changeSetContext));
    } catch (Exception e) {
      throw new InvalidRequestException("Invalid Yaml.", e);
    }
  }

  public BaseTemplate getBaseTemplate(
      String graphName, ChangeContext changeContext, List<ChangeContext> changeSetContext) {
    return commandTemplateYamlHelper.getBaseTemplate(graphName, changeContext, changeSetContext);
  }

  @Override
  public CommandTemplateYaml toYaml(Template bean, String appId) {
    SshCommandTemplate command = (SshCommandTemplate) bean.getTemplateObject();
    String commandUnitType = Utils.getStringFromEnum(command.getCommandType());
    // commmand units
    List<AbstractCommandUnit.Yaml> commandUnitYamlList = null;

    if (command.getCommandUnits() != null) {
      commandUnitYamlList = command.getCommandUnits()
                                .stream()
                                .map(commandUnit -> {
                                  CommandUnitYamlHandler commandUnitsYamlHandler = yamlHandlerFactory.getYamlHandler(
                                      YamlType.COMMAND_UNIT, getCommandUnitSubTypeFromBean(commandUnit));
                                  return (AbstractCommandUnit.Yaml) commandUnitsYamlHandler.toYaml(commandUnit, appId);
                                })
                                .collect(toList());
    }
    CommandTemplateYaml commandTemplateYaml =
        CommandTemplateYaml.builder().commandUnits(commandUnitYamlList).commandUnitType(commandUnitType).build();
    super.toYaml(commandTemplateYaml, bean);
    return commandTemplateYaml;
  }

  private String getCommandUnitSubTypeFromBean(CommandUnit commandUnit) {
    if (commandUnit.getCommandUnitType().equals(COMMAND)) {
      return TemplateConstants.TEMPLATE_REF_COMMAND;
    }
    return commandUnit.getCommandUnitType().name();
  }
}
