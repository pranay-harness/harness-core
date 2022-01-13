/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.commandlibrary.server.service.impl;

import static io.harness.commandlibrary.server.beans.CommandType.CUSTOM_DEPLOYMENT_TYPE;
import static io.harness.git.model.ChangeType.ADD;

import static software.wings.beans.yaml.Change.Builder.aFileChange;
import static software.wings.beans.yaml.ChangeContext.Builder.aChangeContext;

import io.harness.commandlibrary.server.beans.CommandArchiveContext;
import io.harness.commandlibrary.server.service.intfc.CommandArchiveHandler;
import io.harness.commandlibrary.server.service.intfc.CommandService;
import io.harness.commandlibrary.server.service.intfc.CommandVersionService;
import io.harness.commandlibrary.server.utils.YamlUtils;
import io.harness.exception.InvalidRequestException;

import software.wings.beans.template.BaseTemplate;
import software.wings.beans.template.deploymenttype.CustomDeploymentTypeTemplate;
import software.wings.beans.yaml.ChangeContext;
import software.wings.beans.yaml.YamlType;
import software.wings.yaml.templatelibrary.CustomDeploymentTypeTemplateYaml;
import software.wings.yaml.templatelibrary.TemplateLibraryYaml;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class CustomDeploymentTypeArchiveHandler extends AbstractArchiveHandler implements CommandArchiveHandler {
  public static final String COMMAND_DETAIL_YAML = "content.yaml";

  @Inject
  public CustomDeploymentTypeArchiveHandler(
      CommandService commandService, CommandVersionService commandVersionService) {
    super(commandService, commandVersionService);
  }

  @Override
  public boolean supports(CommandArchiveContext commandArchiveContext) {
    return CUSTOM_DEPLOYMENT_TYPE.name().equals(commandArchiveContext.getCommandManifest().getType());
  }

  @Override
  protected void validateYaml(TemplateLibraryYaml baseYaml) {
    if (!CustomDeploymentTypeTemplateYaml.class.isAssignableFrom(baseYaml.getClass())) {
      throw new InvalidRequestException(COMMAND_DETAIL_YAML + ": incorrect type");
    }
  }

  @Override
  protected CustomDeploymentTypeTemplateYaml getBaseYaml(String commandYamlStr) {
    return YamlUtils.fromYaml(commandYamlStr, CustomDeploymentTypeTemplateYaml.class);
  }

  @Override
  protected BaseTemplate getBaseTemplate(String commandName, TemplateLibraryYaml yaml) {
    ChangeContext<CustomDeploymentTypeTemplateYaml> changeContext = createChangeContext(commandName, yaml);
    CustomDeploymentTypeTemplateYaml customDeploymentTypeTemplateYaml = changeContext.getYaml();
    return CustomDeploymentTypeTemplate.builder()
        .fetchInstanceScript(customDeploymentTypeTemplateYaml.getFetchInstanceScript())
        .hostObjectArrayPath(customDeploymentTypeTemplateYaml.getHostObjectArrayPath())
        .hostAttributes(customDeploymentTypeTemplateYaml.getHostAttributes())
        .build();
  }

  private ChangeContext<CustomDeploymentTypeTemplateYaml> createChangeContext(
      String commandName, TemplateLibraryYaml yaml) {
    return aChangeContext()
        .withYaml(yaml)
        .withChange(aFileChange().withFilePath(commandName).withChangeType(ADD).build())
        .withYamlType(YamlType.GLOBAL_TEMPLATE_LIBRARY)
        .build();
  }
}
