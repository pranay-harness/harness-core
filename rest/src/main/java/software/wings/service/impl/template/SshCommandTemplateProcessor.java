package software.wings.service.impl.template;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static software.wings.beans.Base.ACCOUNT_ID_KEY;
import static software.wings.beans.command.Command.Builder.aCommand;
import static software.wings.beans.template.Template.FOLDER_ID_KEY;
import static software.wings.beans.template.Template.NAME_KEY;
import static software.wings.beans.template.Template.TYPE_KEY;
import static software.wings.common.TemplateConstants.GENERIC_JSON_PATH;
import static software.wings.common.TemplateConstants.JBOSS_WAR_INSTALL_PATH;
import static software.wings.common.TemplateConstants.JBOSS_WAR_START_PATH;
import static software.wings.common.TemplateConstants.JBOSS_WAR_STOP_PATH;
import static software.wings.common.TemplateConstants.LATEST_TAG;
import static software.wings.common.TemplateConstants.POWER_SHELL_IIS_INSTALL_PATH;
import static software.wings.common.TemplateConstants.TOMCAT_WAR_INSTALL_PATH;
import static software.wings.common.TemplateConstants.TOMCAT_WAR_START_PATH;
import static software.wings.common.TemplateConstants.TOMCAT_WAR_STOP_PATH;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.mongodb.morphia.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.command.AbstractCommandUnit.Yaml;
import software.wings.beans.command.Command;
import software.wings.beans.command.CommandUnit;
import software.wings.beans.command.ServiceCommand;
import software.wings.beans.template.Template;
import software.wings.beans.template.TemplateType;
import software.wings.beans.template.command.SshCommandTemplate;
import software.wings.beans.yaml.YamlType;
import software.wings.dl.HIterator;
import software.wings.service.impl.yaml.handler.YamlHandlerFactory;
import software.wings.service.impl.yaml.handler.command.CommandUnitYamlHandler;
import software.wings.service.intfc.ServiceResourceService;

import java.util.ArrayList;
import java.util.List;

@Singleton
public class SshCommandTemplateProcessor extends AbstractTemplateProcessor {
  private static final Logger logger = LoggerFactory.getLogger(SshCommandTemplateProcessor.class);

  @Inject YamlHandlerFactory yamlHandlerFactory;
  @Inject ServiceResourceService serviceResourceService;

  @Override
  public Template process(Template template) {
    template = super.process(template);
    SshCommandTemplate sshCommandTemplate = (SshCommandTemplate) template.getTemplateObject();
    if (isNotEmpty(sshCommandTemplate.getCommands())) {
      template.setTemplateObject(convertYamlCommandToCommandUnits(template));
    }
    return template;
  }

  @Override
  public TemplateType getTemplateType() {
    return TemplateType.SSH;
  }

  @Override
  public void loadDefaultTemplates(String accountId, String accountName) {
    super.loadDefaultTemplates(
        asList(TOMCAT_WAR_STOP_PATH, TOMCAT_WAR_START_PATH, TOMCAT_WAR_INSTALL_PATH, JBOSS_WAR_STOP_PATH,
            JBOSS_WAR_START_PATH, JBOSS_WAR_INSTALL_PATH, POWER_SHELL_IIS_INSTALL_PATH, GENERIC_JSON_PATH),
        accountId, accountName);
  }

  @Override
  public void updateLinkedEntities(Template template) {
    // Read all the service commands that references the given
    try (HIterator<ServiceCommand> iterator =
             new HIterator<>(wingsPersistence.createQuery(ServiceCommand.class)
                                 .filter(ServiceCommand.TEMPATE_UUID_KEY, template.getUuid())
                                 .fetch())) {
      while (iterator.hasNext()) {
        ServiceCommand serviceCommand = iterator.next();
        try {
          String templateVersion = serviceCommand.getTemplateVersion();
          if (templateVersion == null || templateVersion.equalsIgnoreCase(LATEST_TAG)) {
            logger.info("Updating the linked commands");
            serviceCommand.setSetAsDefault(true);
            setCommandFromTemplate(template, serviceCommand);
            serviceResourceService.updateCommand(
                serviceCommand.getAppId(), serviceCommand.getServiceId(), serviceCommand, true);
          } else {
            logger.info("The linked template is not the latest. So, not updating it");
          }
        } catch (Exception e) {
          logger.warn(format("Failed to update the linked Service Command %s", serviceCommand.getUuid()), e);
        }
      }
    }
  }

  @Override
  public Command constructEntityFromTemplate(Template template) {
    return transform(template);
  }

  private SshCommandTemplate convertYamlCommandToCommandUnits(Template template) {
    SshCommandTemplate sshCommandTemplate = (SshCommandTemplate) template.getTemplateObject();
    List<CommandUnit> commandUnits = new ArrayList<>();
    for (Yaml commandUnitYaml : sshCommandTemplate.getCommands()) {
      CommandUnitYamlHandler commandUnitYamlHandler =
          yamlHandlerFactory.getYamlHandler(YamlType.COMMAND_UNIT, commandUnitYaml.getCommandUnitType());
      CommandUnit commandUnit = commandUnitYamlHandler.toBean(commandUnitYaml);
      if (commandUnit instanceof Command) {
        Command command = (Command) commandUnit;
        String referenceId = command.getReferenceId();
        if (referenceId != null) {
          Query<Template> templateQuery = wingsPersistence.createQuery(Template.class)
                                              .project("name", true)
                                              .filter(ACCOUNT_ID_KEY, template.getAccountId())
                                              .filter(TYPE_KEY, template.getType())
                                              .filter(FOLDER_ID_KEY, template.getFolderId())
                                              .filter(NAME_KEY, referenceId);
          Template referencedTemplate = templateQuery.get();
          if (referencedTemplate != null) {
            command.setReferenceUuid(referencedTemplate.getUuid());
          }
        }
      }
      commandUnits.add(commandUnit);
    }
    return sshCommandTemplate.withCommandUnits(commandUnits);
  }

  private void setCommandFromTemplate(Template template, ServiceCommand serviceCommand) {
    Command command = transform(template);
    serviceCommand.setCommand(command);
    serviceCommand.setName(command.getName());
  }

  private Command transform(Template template) {
    SshCommandTemplate commandTemplate = (SshCommandTemplate) template.getTemplateObject();
    return aCommand()
        .withTemplateVariables(template.getVariables())
        .withCommandUnits(commandTemplate.getCommandUnits())
        .withName(template.getName())
        .withCommandType(commandTemplate.getCommandType())
        .build();
  }
}
