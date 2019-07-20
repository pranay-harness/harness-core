package migrations.seedata;

import static io.harness.exception.WingsException.ExecutionContext.MANAGER;
import static java.util.Arrays.asList;
import static org.slf4j.LoggerFactory.getLogger;
import static software.wings.beans.Account.GLOBAL_ACCOUNT_ID;
import static software.wings.beans.Variable.VariableBuilder.aVariable;
import static software.wings.common.TemplateConstants.HARNESS_GALLERY;

import com.google.inject.Inject;

import io.harness.exception.WingsException;
import io.harness.logging.ExceptionLogger;
import migrations.SeedDataMigration;
import org.slf4j.Logger;
import software.wings.beans.Variable;
import software.wings.beans.VariableType;
import software.wings.beans.command.Command;
import software.wings.beans.command.CommandUnit;
import software.wings.beans.template.Template;
import software.wings.beans.template.TemplateGallery;
import software.wings.beans.template.TemplateReference;
import software.wings.beans.template.command.SshCommandTemplate;
import software.wings.service.intfc.template.TemplateGalleryService;
import software.wings.service.intfc.template.TemplateService;

import java.util.HashSet;
import java.util.List;

public class TomcatInstallCommandMigration implements SeedDataMigration {
  private static final Logger logger = getLogger(TomcatInstallCommandMigration.class);

  @Inject private TemplateService templateService;
  @Inject private TemplateGalleryService templateGalleryService;

  @Override
  public void migrate() {
    // Get Tomcat Install template for global account
    // validate Harness global gallery exists
    try {
      TemplateGallery harnessTemplateGallery = templateGalleryService.get(GLOBAL_ACCOUNT_ID, HARNESS_GALLERY);
      if (harnessTemplateGallery == null) {
        logger.info("Harness global gallery does not exist. Not updating Tomcat Install Command templates");
        return;
      }

      Template globalInstallTemplate = templateService.fetchTemplateByKeywords(
          GLOBAL_ACCOUNT_ID, new HashSet<>(asList("ssh", "war", "install", "tomcat")));
      if (globalInstallTemplate != null) {
        logger.info("Migrating Default Tomcat Install Command to new format in account [{}]", GLOBAL_ACCOUNT_ID);
        // Update Tomcat Install template for global account
        updateTemplate(globalInstallTemplate);
        logger.info("Migrated Default Tomcat Install Command to new format in account [{}]", GLOBAL_ACCOUNT_ID);
        // get it again after update to fetch latest version
        globalInstallTemplate = templateService.get(globalInstallTemplate.getUuid());
        // Get all templates that have referencedTemplateId = global_install_template_id
        List<Template> templates =
            templateService.fetchTemplatesWithReferencedTemplateId(globalInstallTemplate.getUuid());
        // Update each template
        for (Template template : templates) {
          logger.info(
              "Migrating Default Tomcat Install Command to new format in account [{}]", template.getAccountId());
          template.setReferencedTemplateVersion(globalInstallTemplate.getVersion());
          logger.info("Migrated Default Tomcat Install Command to new format in account [{}]", template.getAccountId());
          updateTemplate(template);
        }
        logger.info("Done migrating Default Tomcat Install Command to new format in all accounts...");
      } else {
        logger.error("Tomcat Install Command template not found in Global account");
      }
    } catch (WingsException e) {
      ExceptionLogger.logProcessedMessages(e, MANAGER, logger);
      logger.error("Tomcat Install Command Migration failed: ", e);
    } catch (Exception e) {
      logger.error("Tomcat Install Command Migration failed: ", e);
    }
  }

  private void updateTemplate(Template existingTemplate) {
    if (existingTemplate != null) {
      logger.info("Tomcat Install Command template found in [{}] account", existingTemplate.getAccountId());
      for (CommandUnit commandUnit : ((SshCommandTemplate) existingTemplate.getTemplateObject()).getCommandUnits()) {
        if (commandUnit instanceof Command) { // updating linked start and stop commands
          // fetch linked template
          Template existingSubTemplate = templateService.get(((Command) commandUnit).getReferenceUuid());
          if (existingSubTemplate != null) {
            // set TemplateReference since its required in new format
            ((Command) commandUnit)
                .setTemplateReference(TemplateReference.builder()
                                          .templateUuid(existingSubTemplate.getUuid())
                                          .templateVersion(existingSubTemplate.getVersion())
                                          .build());
          }
          // set Template variables for command unit
          Variable variable = aVariable().name("RuntimePath").type(VariableType.TEXT).value("${RuntimePath}").build();
          ((Command) commandUnit).setTemplateVariables(asList(variable));
        }
      }
      existingTemplate = templateService.update(existingTemplate);
      logger.info("Tomcat Install Command template updated in account [{}]", existingTemplate.getAccountId());
    } else {
      logger.error("Tomcat Install Command template not found");
    }
  }
}
