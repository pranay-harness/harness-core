/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.yaml.handler.templatelibrary;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.exception.WingsException.USER;
import static io.harness.validation.Validator.notNullCheck;

import static software.wings.beans.CGConstants.GLOBAL_APP_ID;
import static software.wings.common.TemplateConstants.APP_PREFIX;
import static software.wings.common.TemplateConstants.SSH;

import static java.lang.String.format;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.eraro.ErrorCode;
import io.harness.eraro.Level;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.NoResultFoundException;

import software.wings.beans.Application;
import software.wings.beans.command.Command;
import software.wings.beans.command.CommandType;
import software.wings.beans.template.Template;
import software.wings.beans.template.TemplateReference;
import software.wings.beans.template.TemplateReference.TemplateReferenceBuilder;
import software.wings.beans.yaml.ChangeContext;
import software.wings.beans.yaml.YamlType;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;
import software.wings.service.impl.yaml.handler.YamlHandlerFactory;
import software.wings.service.impl.yaml.handler.command.CommandUnitYamlHandler;
import software.wings.service.impl.yaml.service.YamlHelper;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.template.TemplateService;
import software.wings.yaml.command.CommandRefYaml;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(CDC)
@Singleton
@Slf4j
public class CommandTemplateRefYamlHandler extends CommandUnitYamlHandler<CommandRefYaml, Command> {
  @Inject TemplateService templateService;
  @Inject YamlHelper yamlHelper;
  @Inject YamlHandlerFactory yamlHandlerFactory;
  @Inject AppService appService;
  static final String LATEST_TAG = "latest";

  @Override
  protected Command getCommandUnit() {
    return new Command();
  }

  @Override
  public CommandRefYaml toYaml(Command bean, String appId) {
    CommandRefYaml yaml = CommandRefYaml.builder().build();
    super.toYaml(yaml, bean);
    yaml.setVariables(TemplateLibraryYamlHandler.variablesToTemplateVariableYaml(bean.getTemplateVariables()));

    final String templateUuid = bean.getReferenceUuid();
    String version = null;
    if (bean.getTemplateReference() != null && bean.getTemplateReference().getTemplateVersion() != null) {
      version = String.valueOf(bean.getTemplateReference().getTemplateVersion());
    }
    String templateUri = templateService.makeNamespacedTemplareUri(templateUuid, version);

    yaml.setTemplateUri(templateUri);
    return yaml;
  }

  @Override
  public Class getYamlClass() {
    return CommandRefYaml.class;
  }

  @Override
  public Command upsertFromYaml(ChangeContext<CommandRefYaml> changeContext, List<ChangeContext> changeSetContext) {
    Command commandRef = getCommandUnit();
    commandRef.setName(changeContext.getYaml().getName());

    CommandRefYaml yaml = changeContext.getYaml();
    String filePath = changeContext.getChange().getFilePath();

    String appId = getAppId(changeContext);
    String templateUri = yaml.getTemplateUri();
    notNullCheck("templateUri field missing.", templateUri);
    if (templateUri.startsWith(APP_PREFIX)) {
      templateUri = templateUri.substring(APP_PREFIX.length());
    }
    Template template =
        templateService.fetchTemplateFromUri(templateUri, changeContext.getChange().getAccountId(), appId);
    // This can happen when the template command is in the same changeset but not yet processed. So we extract it from
    // the changeSet and process it first.
    if (template == null) {
      processDependentTemplate(changeSetContext, filePath, yaml);
      template = templateService.fetchTemplateFromUri(templateUri, changeContext.getChange().getAccountId(), appId);
      notNullCheck(format("No template can be found with URI:[%s] , appId:[%s]", templateUri, appId), template, USER);
    }

    commandRef.setCommandType(CommandType.OTHER);
    TemplateReferenceBuilder templateReferenceBuilder = TemplateReference.builder();
    String templateUuid = template.getUuid();
    templateReferenceBuilder.templateUuid(templateUuid);
    String templateVersion = templateService.fetchTemplateVersionFromUri(templateUuid, templateUri);
    try {
      templateReferenceBuilder.templateVersion(Long.valueOf(templateVersion));
    } catch (Exception e) {
      throw new InvalidRequestException(
          "Please provide an exact version. Version provided is" + templateVersion + ".", e);
    }
    if (Long.parseLong(templateVersion) > template.getVersion()) {
      throw new InvalidRequestException(
          format("The referenced template %s with version %s does not exist. The latest version is: %s.",
              template.getName(), templateVersion, template.getVersion()),
          USER);
    }
    commandRef.setTemplateReference(templateReferenceBuilder.build());
    commandRef.setReferenceUuid(template.getUuid());
    commandRef.setTemplateVariables(TemplateLibraryYamlHandler.templateVariableYamlToVariable(yaml.getVariables()));
    commandRef.setCommandType(CommandType.OTHER);
    commandRef.setVersion(Long.valueOf(templateVersion));
    return commandRef;
  }

  private String getAppId(ChangeContext changeContext) {
    String appId = GLOBAL_APP_ID;
    String filePath = changeContext.getChange().getFilePath();
    final String appName = yamlHelper.getAppName(filePath);
    if (EmptyPredicate.isNotEmpty(appName)) {
      final Application application = appService.getAppByName(changeContext.getChange().getAccountId(), appName);
      if (application == null) {
        throw NoResultFoundException.newBuilder()
            .message("Cannot find application by name :" + appName)
            .level(Level.ERROR)
            .code(ErrorCode.INVALID_ARGUMENT)
            .build();
      }
      appId = application.getUuid();
    }
    return appId;
  }

  private void processDependentTemplate(List<ChangeContext> changeSetContext, String filePath, CommandRefYaml yaml) {
    Optional<ChangeContext> commandContextOptional =
        changeSetContext.stream()
            .filter(context -> {
              if (!(context.getYamlType() == YamlType.GLOBAL_TEMPLATE_LIBRARY
                      || context.getYamlType() == YamlType.APPLICATION_TEMPLATE_LIBRARY)) {
                return false;
              }
              String commandFilePath = context.getChange().getFilePath();
              return !commandFilePath.equals(filePath);
            })
            .findFirst();

    if (commandContextOptional.isPresent()) {
      getHandlerAndUpsert(commandContextOptional.get(), changeSetContext);
    } else {
      throw new InvalidRequestException("No command with the given name: " + yaml.getName());
    }
  }

  private void getHandlerAndUpsert(ChangeContext commandContext, List<ChangeContext> changeSetContext) {
    BaseYamlHandler commandYamlHandler;
    if (getAppId(commandContext).equals(GLOBAL_APP_ID)) {
      commandYamlHandler = yamlHandlerFactory.getYamlHandler(YamlType.GLOBAL_TEMPLATE_LIBRARY, SSH);
    } else {
      commandYamlHandler = yamlHandlerFactory.getYamlHandler(YamlType.APPLICATION_TEMPLATE_LIBRARY, SSH);
    }
    try {
      commandYamlHandler.upsertFromYaml(commandContext, changeSetContext);
    } catch (Exception e) {
      throw new InvalidRequestException("Invalid Yaml", e);
    }
  }
}
