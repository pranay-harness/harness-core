package software.wings.service.impl.template;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.persistence.HQuery.excludeAuthority;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
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

import de.danielbechler.diff.ObjectDifferBuilder;
import de.danielbechler.diff.node.DiffNode;
import io.harness.persistence.HIterator;
import org.mongodb.morphia.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.command.AbstractCommandUnit.Yaml;
import software.wings.beans.command.Command;
import software.wings.beans.command.CommandUnit;
import software.wings.beans.command.ServiceCommand;
import software.wings.beans.template.BaseTemplate;
import software.wings.beans.template.Template;
import software.wings.beans.template.TemplateType;
import software.wings.beans.template.command.SshCommandTemplate;
import software.wings.beans.yaml.YamlType;
import software.wings.service.impl.yaml.handler.YamlHandlerFactory;
import software.wings.service.impl.yaml.handler.command.CommandUnitYamlHandler;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.template.TemplateService;

import java.util.ArrayList;
import java.util.List;

@Singleton
public class SshCommandTemplateProcessor extends AbstractTemplateProcessor {
  private static final Logger logger = LoggerFactory.getLogger(SshCommandTemplateProcessor.class);

  @Inject YamlHandlerFactory yamlHandlerFactory;
  @Inject ServiceResourceService serviceResourceService;
  @Inject TemplateService templateService;

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
             new HIterator<>(wingsPersistence.createQuery(ServiceCommand.class, excludeAuthority)
                                 .filter(ServiceCommand.TEMPLATE_UUID_KEY, template.getUuid())
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
        .withTemplateId(template.getUuid())
        .build();
  }

  public Command fetchEntityFromTemplate(Template template) {
    Command command = null;
    if (template != null) {
      command = (Command) templateService.constructEntityFromTemplate(
          template.getUuid(), String.valueOf(template.getVersion()));
      command.setTemplateVersion("latest");
    }
    return command;
  }

  @Override
  public List<String> fetchTemplateProperties() {
    return asList();
  }

  @Override
  public boolean checkTemplateDetailsChanged(BaseTemplate oldTemplate, BaseTemplate newTemplate) {
    // check if command units changed
    List<CommandUnit> newCommandUnits = ((SshCommandTemplate) newTemplate).getCommandUnits();
    List<CommandUnit> oldCommandUnits = ((SshCommandTemplate) oldTemplate).getCommandUnits();
    DiffNode commandUnitDiff = ObjectDifferBuilder.buildDefault().compare(newCommandUnits, oldCommandUnits);
    if (isCommandUnitsSizeChanged(newCommandUnits, oldCommandUnits)
        || isCommandUnitsOrderChanged(newCommandUnits, oldCommandUnits) || commandUnitDiff.hasChanges()) {
      return true;
    }
    return false;
  }

  private boolean isCommandUnitsOrderChanged(List<CommandUnit> commandUnits, List<CommandUnit> oldCommandUnits) {
    if (commandUnits != null && oldCommandUnits != null) {
      if (commandUnits.size() == oldCommandUnits.size()) {
        List<String> commandNames = commandUnits.stream().map(commandUnit -> commandUnit.getName()).collect(toList());
        List<String> oldCommandNames =
            oldCommandUnits.stream().map(oldCommandUnit -> oldCommandUnit.getName()).collect(toList());
        return !commandNames.equals(oldCommandNames);
      }
    }
    return false;
  }

  private boolean isCommandUnitsSizeChanged(List<CommandUnit> commandUnits, List<CommandUnit> oldCommandUnits) {
    if (commandUnits != null && oldCommandUnits != null) {
      if (commandUnits.size() != oldCommandUnits.size()) {
        return true;
      }
    }
    return false;
  }
}
