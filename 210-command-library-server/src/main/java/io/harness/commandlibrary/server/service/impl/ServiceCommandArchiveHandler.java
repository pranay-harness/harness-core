/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.commandlibrary.server.service.impl;

import static io.harness.commandlibrary.server.beans.CommandType.SSH;
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
import software.wings.beans.yaml.ChangeContext;
import software.wings.beans.yaml.YamlType;
import software.wings.service.impl.yaml.handler.templatelibrary.CommandTemplateYamlHelper;
import software.wings.yaml.templatelibrary.TemplateLibraryYaml;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Collections;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class ServiceCommandArchiveHandler extends AbstractArchiveHandler implements CommandArchiveHandler {
  public static final String COMMAND_DETAIL_YAML = "content.yaml";

  private final CommandTemplateYamlHelper commandTemplateYamlHelper;

  @Inject
  public ServiceCommandArchiveHandler(CommandService commandService, CommandVersionService commandVersionService,
      CommandTemplateYamlHelper commandTemplateYamlHelper) {
    super(commandService, commandVersionService);
    this.commandTemplateYamlHelper = commandTemplateYamlHelper;
  }

  @Override
  public boolean supports(CommandArchiveContext commandArchiveContext) {
    return SSH.name().equals(commandArchiveContext.getCommandManifest().getType());
  }

  @Override
  protected void validateYaml(TemplateLibraryYaml baseYaml) {
    if (!commandTemplateYamlHelper.getYamlClass().isAssignableFrom(baseYaml.getClass())) {
      throw new InvalidRequestException(COMMAND_DETAIL_YAML + ": incorrect type");
    }
  }

  @Override
  protected TemplateLibraryYaml getBaseYaml(String commandYamlStr) {
    return YamlUtils.fromYaml(commandYamlStr, TemplateLibraryYaml.class);
  }

  @Override
  protected BaseTemplate getBaseTemplate(String commandName, TemplateLibraryYaml yaml) {
    return commandTemplateYamlHelper.getBaseTemplate(
        commandName, createChangeContext(commandName, yaml), Collections.emptyList());
  }

  private ChangeContext<TemplateLibraryYaml> createChangeContext(String commandName, TemplateLibraryYaml yaml) {
    return aChangeContext()
        .withYaml(yaml)
        .withChange(aFileChange().withFilePath(commandName).withChangeType(ADD).build())
        .withYamlType(YamlType.GLOBAL_TEMPLATE_LIBRARY)
        .build();
  }
}
