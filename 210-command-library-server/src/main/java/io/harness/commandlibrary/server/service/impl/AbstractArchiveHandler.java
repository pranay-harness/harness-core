/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.commandlibrary.server.service.impl;

import io.harness.commandlibrary.server.beans.CommandArchiveContext;
import io.harness.commandlibrary.server.beans.CommandManifest;
import io.harness.commandlibrary.server.service.intfc.CommandArchiveHandler;
import io.harness.commandlibrary.server.service.intfc.CommandService;
import io.harness.commandlibrary.server.service.intfc.CommandVersionService;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.UnexpectedException;
import io.harness.exception.UnsupportedOperationException;

import software.wings.beans.Variable;
import software.wings.beans.commandlibrary.CommandEntity;
import software.wings.beans.commandlibrary.CommandVersionEntity;
import software.wings.beans.commandlibrary.CommandVersionEntity.CommandVersionEntityBuilder;
import software.wings.beans.template.BaseTemplate;
import software.wings.service.impl.yaml.handler.templatelibrary.TemplateLibraryYamlHandler;
import software.wings.yaml.templatelibrary.TemplateLibraryYaml;

import com.google.inject.Inject;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.AllArgsConstructor;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
public abstract class AbstractArchiveHandler implements CommandArchiveHandler {
  public static final String COMMAND_DETAIL_YAML = "content.yaml";

  private final CommandService commandService;
  private final CommandVersionService commandVersionService;

  @Override
  public String createNewCommandVersion(CommandArchiveContext commandArchiveContext) {
    if (!supports(commandArchiveContext)) {
      throw new UnsupportedOperationException("archive not supported by the handler");
    }
    //    create manifest object
    final CommandManifest manifest = commandArchiveContext.getCommandManifest();

    final String commandYamlStr = getCommandYamlStr(commandArchiveContext);

    validateCommandYamlStr(commandYamlStr);

    final CommandEntity commandEntity = ensureCommandExists(commandArchiveContext);

    final CommandVersionEntity newCommandVersionEntity =
        createNewVersion(commandEntity, manifest, commandYamlStr, commandArchiveContext.getAccountId());

    updateCommandWithNewVersionDetails(commandEntity, newCommandVersionEntity);

    return newCommandVersionEntity.getUuid();
  }
  protected CommandEntity updateCommandWithNewVersionDetails(
      CommandEntity commandEntity, CommandVersionEntity newCommandVersionEntity) {
    commandEntity.setLatestVersion(newCommandVersionEntity.getVersion());
    commandEntity.setTags(newCommandVersionEntity.getTags());
    commandEntity.setRepoUrl(newCommandVersionEntity.getRepoUrl());
    commandEntity.setDescription(newCommandVersionEntity.getDescription());
    commandEntity.setLastUpdatedByAccountId(newCommandVersionEntity.getLastUpdatedByAccountId());
    return commandService.saveAndGet(commandEntity);
  }

  protected String getCommandYamlStr(CommandArchiveContext commandArchiveContext) {
    return commandArchiveContext.getArchiveFile()
        .getContent(COMMAND_DETAIL_YAML)
        .map(content -> content.string(StandardCharsets.UTF_8))
        .orElseThrow(() -> new InvalidRequestException(COMMAND_DETAIL_YAML + " file not found"));
  }

  protected void validateCommandYamlStr(String commandYamlStr) {
    if (commandYamlStr.isEmpty()) {
      throw new InvalidRequestException(COMMAND_DETAIL_YAML + " cannot be empty");
    }
  }

  protected CommandEntity ensureCommandExists(CommandArchiveContext commandArchiveContext) {
    final String commandStoreName = commandArchiveContext.getCommandStoreName();
    final CommandManifest commandManifest = commandArchiveContext.getCommandManifest();
    final String commandName = commandManifest.getName();
    return commandService.getCommandEntity(commandStoreName, commandName)
        .orElseGet(()
                       -> commandService.createFromManifest(
                           commandStoreName, commandManifest, commandArchiveContext.getAccountId()));
  }

  protected CommandVersionEntityBuilder populateMetadataInVersion(CommandVersionEntityBuilder versionBuilder,
      String commandYamlStr, CommandEntity commandEntity, CommandManifest manifest, String accountId) {
    return versionBuilder.yamlContent(commandYamlStr)
        .version(manifest.getVersion())
        .description(manifest.getDescription())
        .tags(manifest.getTags())
        .repoUrl(manifest.getRepoUrl())
        .commandName(commandEntity.getName())
        .commandStoreName(commandEntity.getCommandStoreName())
        .createdByAccountId(accountId)
        .lastUpdatedByAccountId(accountId)
        .commandId(commandEntity.getUuid());
  }

  protected CommandVersionEntity createNewVersion(
      CommandEntity commandEntity, CommandManifest manifest, String commandYamlStr, String accountId) {
    final CommandVersionEntityBuilder versionBuilder =
        populateMetadataInVersion(CommandVersionEntity.builder(), commandYamlStr, commandEntity, manifest, accountId);

    processYamlAndPopulateBuilder(manifest.getName(), commandYamlStr, versionBuilder);

    final String versionId = commandVersionService.save(versionBuilder.build());

    return commandVersionService.getEntityById(versionId).orElseThrow(
        () -> new UnexpectedException("Command version not found"));
  }

  protected CommandVersionEntityBuilder processYamlAndPopulateBuilder(
      String commandName, String commandYamlStr, CommandVersionEntityBuilder builder) {
    final TemplateLibraryYaml baseYaml = getBaseYaml(commandYamlStr);
    validateYaml(baseYaml);
    final List<Variable> variables = getVariables(baseYaml);
    final BaseTemplate baseTemplate = getBaseTemplate(commandName, baseYaml);

    builder.variables(variables);
    builder.templateObject(baseTemplate);
    builder.yamlContent(commandYamlStr);
    return builder;
  }

  protected List<Variable> getVariables(TemplateLibraryYaml baseYaml) {
    return TemplateLibraryYamlHandler.templateVariableYamlToVariable(baseYaml.getVariables());
  }

  protected abstract void validateYaml(TemplateLibraryYaml baseYaml);

  protected abstract BaseTemplate getBaseTemplate(String commandName, TemplateLibraryYaml baseYaml);

  protected abstract TemplateLibraryYaml getBaseYaml(String commandYamlStr);
}
