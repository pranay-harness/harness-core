package io.harness.commandlibrary.server.service.impl;

import static io.harness.commandlibrary.server.beans.CommandType.SSH;
import static software.wings.beans.yaml.Change.Builder.aFileChange;
import static software.wings.beans.yaml.Change.ChangeType.ADD;
import static software.wings.beans.yaml.ChangeContext.Builder.aChangeContext;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.commandlibrary.server.beans.CommandArchiveContext;
import io.harness.commandlibrary.server.beans.CommandManifest;
import io.harness.commandlibrary.server.service.intfc.CommandArchiveHandler;
import io.harness.commandlibrary.server.service.intfc.CommandService;
import io.harness.commandlibrary.server.service.intfc.CommandVersionService;
import io.harness.commandlibrary.server.utils.YamlUtils;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.UnexpectedException;
import io.harness.exception.UnsupportedOperationException;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.Variable;
import software.wings.beans.commandlibrary.CommandEntity;
import software.wings.beans.commandlibrary.CommandVersionEntity;
import software.wings.beans.commandlibrary.CommandVersionEntity.CommandVersionEntityBuilder;
import software.wings.beans.template.BaseTemplate;
import software.wings.beans.yaml.ChangeContext;
import software.wings.beans.yaml.YamlType;
import software.wings.service.impl.yaml.handler.templatelibrary.CommandTemplateYamlHelper;
import software.wings.service.impl.yaml.handler.templatelibrary.TemplateLibraryYamlHandler;
import software.wings.yaml.templatelibrary.TemplateLibraryYaml;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

@Singleton
@Slf4j
public class ServiceCommandArchiveHandler implements CommandArchiveHandler {
  public static final String COMMAND_DETAIL_YAML = "content.yaml";

  private final CommandService commandService;
  private final CommandVersionService commandVersionService;
  private final CommandTemplateYamlHelper commandTemplateYamlHelper;

  @Inject
  public ServiceCommandArchiveHandler(CommandService commandService, CommandVersionService commandVersionService,
      CommandTemplateYamlHelper commandTemplateYamlHelper) {
    this.commandService = commandService;
    this.commandVersionService = commandVersionService;
    this.commandTemplateYamlHelper = commandTemplateYamlHelper;
  }

  @Override
  public boolean supports(CommandArchiveContext commandArchiveContext) {
    return SSH.name().equals(commandArchiveContext.getCommandManifest().getType());
  }

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

  private CommandEntity updateCommandWithNewVersionDetails(
      CommandEntity commandEntity, CommandVersionEntity newCommandVersionEntity) {
    commandEntity.setLatestVersion(newCommandVersionEntity.getVersion());
    commandEntity.setTags(newCommandVersionEntity.getTags());
    commandEntity.setRepoUrl(newCommandVersionEntity.getRepoUrl());
    commandEntity.setDescription(newCommandVersionEntity.getDescription());
    commandEntity.setLastUpdatedByAccountId(newCommandVersionEntity.getLastUpdatedByAccountId());
    return commandService.saveAndGet(commandEntity);
  }

  private CommandVersionEntity createNewVersion(
      CommandEntity commandEntity, CommandManifest manifest, String commandYamlStr, String accountId) {
    final CommandVersionEntityBuilder versionBuilder =
        populateMetadataInVersion(CommandVersionEntity.builder(), commandYamlStr, commandEntity, manifest, accountId);

    processYamlAndPopulateBuilder(manifest.getName(), commandYamlStr, versionBuilder);

    final String versionId = commandVersionService.save(versionBuilder.build());

    return commandVersionService.getEntityById(versionId).orElseThrow(
        () -> new UnexpectedException("Command version not found"));
  }

  private CommandVersionEntityBuilder populateMetadataInVersion(CommandVersionEntityBuilder versionBuilder,
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

  private CommandVersionEntityBuilder processYamlAndPopulateBuilder(
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

  private void validateYaml(TemplateLibraryYaml baseYaml) {
    if (!commandTemplateYamlHelper.getYamlClass().isAssignableFrom(baseYaml.getClass())) {
      throw new InvalidRequestException(COMMAND_DETAIL_YAML + ": incorrect type");
    }
  }

  private List<Variable> getVariables(TemplateLibraryYaml baseYaml) {
    return TemplateLibraryYamlHandler.templateVariableYamlToVariable(baseYaml.getVariables());
  }

  private TemplateLibraryYaml getBaseYaml(String commandYamlStr) {
    return YamlUtils.fromYaml(commandYamlStr, TemplateLibraryYaml.class);
  }

  private BaseTemplate getBaseTemplate(String commandName, TemplateLibraryYaml yaml) {
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

  private void validateCommandYamlStr(String commandYamlStr) {
    if (commandYamlStr.isEmpty()) {
      throw new InvalidRequestException(COMMAND_DETAIL_YAML + " cannot be empty");
    }
  }

  private String getCommandYamlStr(CommandArchiveContext commandArchiveContext) {
    return commandArchiveContext.getArchiveFile()
        .getContent(COMMAND_DETAIL_YAML)
        .map(content -> content.string(StandardCharsets.UTF_8))
        .orElseThrow(() -> new InvalidRequestException(COMMAND_DETAIL_YAML + " file not found"));
  }

  private CommandEntity ensureCommandExists(CommandArchiveContext commandArchiveContext) {
    final String commandStoreName = commandArchiveContext.getCommandStoreName();
    final CommandManifest commandManifest = commandArchiveContext.getCommandManifest();
    final String commandName = commandManifest.getName();
    return commandService.getCommandEntity(commandStoreName, commandName)
        .orElseGet(()
                       -> commandService.createFromManifest(
                           commandStoreName, commandManifest, commandArchiveContext.getAccountId()));
  }
}
