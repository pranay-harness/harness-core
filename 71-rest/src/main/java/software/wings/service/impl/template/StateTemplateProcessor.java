package software.wings.service.impl.template;

import de.danielbechler.diff.ObjectDifferBuilder;
import de.danielbechler.diff.node.DiffNode;
import io.harness.exception.InvalidRequestException;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.EntityType;
import software.wings.beans.GraphNode;
import software.wings.beans.template.BaseTemplate;
import software.wings.beans.template.Template;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public abstract class StateTemplateProcessor extends AbstractTemplateProcessor {
  @Override
  public void updateLinkedEntities(Template template) {
    // Read steps that references the given template
    Template savedTemplate = templateService.get(template.getUuid());
    if (savedTemplate == null) {
      logger.info("Template {} was deleted. Not updating linked entities", template.getUuid());
      return;
    }
    updateLinkedEntitiesInWorkflow(template);
  }

  @Override
  public GraphNode constructEntityFromTemplate(Template template, EntityType entityType) {
    switch (entityType) {
      case WORKFLOW:
        Map<String, Object> properties = new HashMap<>();
        transform(template, properties);
        return GraphNode.builder()
            .templateVariables(template.getVariables())
            .properties(properties)
            .templateUuid(template.getUuid())
            .type(getTemplateType().name())
            .build();
      default:
        throw new InvalidRequestException("Unsupported Entity Type");
    }
  }

  public abstract void transform(Template template, Map<String, Object> properties);

  @Override
  public boolean checkTemplateDetailsChanged(BaseTemplate oldTemplate, BaseTemplate newTemplate) {
    DiffNode templateDetailsDiff = ObjectDifferBuilder.buildDefault().compare(newTemplate, oldTemplate);
    return templateDetailsDiff.hasChanges();
  }
}
