package software.wings.service.impl.template;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.lang.String.format;
import static software.wings.beans.Base.GLOBAL_ACCOUNT_ID;
import static software.wings.beans.Base.GLOBAL_APP_ID;
import static software.wings.common.TemplateConstants.HARNESS_GALLERY;
import static software.wings.common.TemplateConstants.LATEST_TAG;

import com.google.inject.Inject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.template.Template;
import software.wings.beans.template.TemplateHelper;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.service.intfc.template.TemplateService;

import java.io.IOException;
import java.net.URL;
import java.util.List;

public abstract class AbstractTemplateProcessor {
  private static final Logger logger = LoggerFactory.getLogger(AbstractTemplateProcessor.class);
  @Inject protected TemplateService templateService;
  @Inject protected WingsPersistence wingsPersistence;

  ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

  /**
   * Process the template
   *
   * @param template
   */
  public Template process(Template template) {
    template.setType(getTemplateType().name());
    return template;
  }

  public abstract software.wings.beans.template.TemplateType getTemplateType();

  /**
   * Loads Harness default command templates
   *
   * @param accountId
   */
  public void loadDefaultTemplates(String accountId, String accountName) {}

  public void loadDefaultTemplates(List<String> templateFiles, String accountId, String accountName) {
    // First
    templateFiles.forEach(templatePath -> {
      try {
        logger.info("Loading url file {} for the account {} ", templatePath, accountId);
        loadAndSaveTemplate(templatePath, accountId, accountName);
      } catch (WingsException exception) {
        String msg = "Failed to save template from file [" + templatePath + "] for the account [" + accountId
            + "] . Reason:" + exception.getMessage();
        throw new WingsException(msg, exception, WingsException.USER);
      } catch (IOException exception) {
        String msg = "Failed to save template from file [" + templatePath + "]. Reason:" + exception.getMessage();
        throw new WingsException(msg, exception, WingsException.USER);
      }
    });
  }

  /**
   * Loads Yaml file and returns Template
   *
   * @param templatePath
   * @return
   */
  public Template loadYaml(String templatePath, String accountId, String accountName) {
    try {
      return loadAndSaveTemplate(templatePath, accountId, accountName);
    } catch (IOException e) {
      logger.warn(format("Failed to load Yaml from path %s", templatePath), e);
      throw new WingsException("Failed to load template from path " + templatePath, WingsException.SRE);
    }
  }

  private Template loadAndSaveTemplate(String templatePath, String accountId, String accountName) throws IOException {
    URL url = this.getClass().getClassLoader().getResource(templatePath);
    Template template = mapper.readValue(url, Template.class);

    if (!GLOBAL_ACCOUNT_ID.equals(accountId)) {
      String referencedTemplateUri = template.getReferencedTemplateUri();
      if (isNotEmpty(referencedTemplateUri)) {
        String referencedTemplateVersion = TemplateHelper.obtainTemplateVersion(referencedTemplateUri);
        template.setReferencedTemplateId(
            templateService.fetchTemplateIdFromUri(GLOBAL_ACCOUNT_ID, referencedTemplateUri));
        if (!LATEST_TAG.equals(referencedTemplateVersion)) {
          if (referencedTemplateVersion != null) {
            template.setReferencedTemplateVersion(Long.parseLong(referencedTemplateVersion));
          }
        }
      }
      if (isNotEmpty(template.getFolderPath())) {
        template.setFolderPath(template.getFolderPath().replace(HARNESS_GALLERY, accountName));
      }
    }
    template.setAppId(GLOBAL_APP_ID);
    template.setAccountId(accountId);
    return templateService.save(template);
  }

  public abstract void updateLinkedEntities(Template template);

  public abstract Object constructEntityFromTemplate(Template template);
}
