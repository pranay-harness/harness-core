package software.wings.service.impl.template;

import static software.wings.beans.template.TemplateType.CUSTOM_DEPLOYMENT_TYPE;

import de.danielbechler.diff.ObjectDifferBuilder;
import de.danielbechler.diff.node.DiffNode;
import software.wings.beans.EntityType;
import software.wings.beans.template.BaseTemplate;
import software.wings.beans.template.Template;
import software.wings.beans.template.TemplateType;

import java.util.List;

public class CustomDeploymentTypeProcessor extends AbstractTemplateProcessor {
  @Override
  public TemplateType getTemplateType() {
    return CUSTOM_DEPLOYMENT_TYPE;
  }

  @Override
  public void updateLinkedEntities(Template template) {
    /*
    Not implementing as of now
     */
  }

  @Override
  public Object constructEntityFromTemplate(Template template, EntityType entityType) {
    return null;
  }

  @Override
  public List<String> fetchTemplateProperties() {
    return null;
  }

  @Override
  public boolean checkTemplateDetailsChanged(BaseTemplate oldTemplate, BaseTemplate newTemplate) {
    DiffNode templateDetailsDiff = ObjectDifferBuilder.buildDefault().compare(newTemplate, oldTemplate);
    return templateDetailsDiff.hasChanges();
  }
}
