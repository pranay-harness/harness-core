package software.wings.service.impl.template;

import static software.wings.beans.Base.GLOBAL_APP_ID;
import static software.wings.exception.WingsException.ExecutionContext.MANAGER;

import com.google.inject.Inject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.template.Template;
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
  public void loadDefaultTemplates(String accountId) {}

  public void loadDefaultTemplates(List<String> templateFiles, String accountId) {
    // First
    templateFiles.forEach(templatePath -> {
      try {
        logger.info("Loading url file {}", templatePath);
        loadAndSaveTemplate(templatePath, accountId);
      } catch (WingsException e) {
        String msg = "Failed to save template from file [" + templatePath + "]. Reason:" + e.getMessage();
        e.logProcessedMessages(MANAGER, logger);
        throw new WingsException(msg, WingsException.USER);
      } catch (IOException e) {
        String msg = "Failed to save template from file [" + templatePath + "]. Reason:" + e.getMessage();
        throw new WingsException(msg, WingsException.USER);
      }
    });
  }

  /**
   * Loads Yaml file and returns Template
   * @param templatePath
   * @return
   */
  public Template loadYaml(String templatePath, String accountId) {
    try {
      return loadAndSaveTemplate(templatePath, accountId);
    } catch (IOException e) {
      logger.warn("Failed to load Yaml from path {}", templatePath, e);
      throw new WingsException("Failed to load template from path " + templatePath, WingsException.SRE);
    }
  }

  private Template loadAndSaveTemplate(String templatePath, String accountId) throws IOException {
    URL url = this.getClass().getClassLoader().getResource(templatePath);
    Template template = mapper.readValue(url, Template.class);

    template.setAppId(GLOBAL_APP_ID);
    template.setAccountId(accountId);
    return templateService.save(template);
  }

  public abstract void updateLinkedEntities(Template template);

  public abstract Object constructEntityFromTemplate(Template template);
}
