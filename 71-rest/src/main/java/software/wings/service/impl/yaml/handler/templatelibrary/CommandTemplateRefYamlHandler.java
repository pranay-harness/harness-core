package software.wings.service.impl.yaml.handler.templatelibrary;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;
import static io.harness.validation.Validator.notNullCheck;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.common.TemplateConstants.SSH;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.data.structure.EmptyPredicate;
import io.harness.eraro.ErrorCode;
import io.harness.eraro.Level;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.NoResultFoundException;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.Application;
import software.wings.beans.command.Command;
import software.wings.beans.command.CommandType;
import software.wings.beans.template.Template;
import software.wings.beans.template.TemplateHelper;
import software.wings.beans.template.TemplateReference;
import software.wings.beans.template.TemplateReference.TemplateReferenceBuilder;
import software.wings.beans.yaml.ChangeContext;
import software.wings.beans.yaml.YamlType;
import software.wings.service.impl.yaml.handler.YamlHandlerFactory;
import software.wings.service.impl.yaml.handler.command.CommandUnitYamlHandler;
import software.wings.service.impl.yaml.service.YamlHelper;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.template.TemplateService;
import software.wings.yaml.command.CommandRefYaml;

import java.util.List;
import java.util.Optional;

@Singleton
@Slf4j
public class CommandTemplateRefYamlHandler extends CommandUnitYamlHandler<CommandRefYaml, Command> {
  private final String APP_PREFIX = "App/";
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

    String templateUri = null;
    final String templateUuid = bean.getReferenceUuid();
    if (templateUuid != null) {
      // Command is linked
      templateUri = templateService.fetchTemplateUri(templateUuid);
      if (templateUri == null) {
        logger.error("Linked template for service command {} was deleted ", bean.getUuid());
      }
      if (bean.getTemplateReference() != null && bean.getTemplateReference().getTemplateVersion() != null) {
        templateUri = templateUri + ":" + bean.getTemplateReference().getTemplateVersion();
      }
      Template template = templateService.get(templateUuid);
      if (template != null) {
        if (!template.getAppId().equals(GLOBAL_APP_ID)) {
          templateUri = APP_PREFIX + templateUri;
        }
      }
    }
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

    String commandName = yaml.getName();
    Template template =
        templateService.fetchTemplateFromUri(yaml.getTemplateUri(), changeContext.getChange().getAccountId(), appId);
    // This can happen when the template command is in the same changeset but not yet processed. So we extract it from
    // the changeSet and process it first.
    if (template == null) {
      processDependentTemplate(changeSetContext, filePath, yaml);
      template = templateService.fetchTemplateFromUri(
          changeContext.getYaml().getTemplateUri(), changeContext.getChange().getAccountId(), appId);
      notNullCheck("No command found with the given name:" + commandName, template, USER);
    }

    commandRef.setCommandType(CommandType.OTHER);
    String templateUri = yaml.getTemplateUri();
    TemplateReferenceBuilder templateReferenceBuilder = TemplateReference.builder();
    if (isNotEmpty(templateUri)) {
      if (templateUri.startsWith(APP_PREFIX)) {
        templateUri = templateUri.substring(APP_PREFIX.length());
        templateReferenceBuilder.templateUuid(
            templateService.fetchTemplateIdFromUri(changeContext.getChange().getAccountId(), appId, templateUri));
      } else {
        templateReferenceBuilder.templateUuid(
            templateService.fetchTemplateIdFromUri(changeContext.getChange().getAccountId(), templateUri));
      }
      String templateVersion = TemplateHelper.obtainTemplateVersion(templateUri);
      try {
        templateReferenceBuilder.templateVersion(Long.valueOf(templateVersion));
      } catch (Exception e) {
        throw new InvalidRequestException(
            "Please provide an exact version. Version provided is" + templateVersion + ".", e);
      }
    }
    commandRef.setTemplateReference(templateReferenceBuilder.build());
    commandRef.setReferenceUuid(template.getUuid());
    commandRef.setTemplateVariables(TemplateLibraryYamlHandler.templateVariableYamlToVariable(yaml.getVariables()));
    commandRef.setCommandType(CommandType.OTHER);

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
    CommandTemplateYamlHandler commandYamlHandler;
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