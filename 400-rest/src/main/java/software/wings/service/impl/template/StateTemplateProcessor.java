/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.template;

import static java.util.Arrays.asList;

import io.harness.exception.InvalidRequestException;

import software.wings.beans.EntityType;
import software.wings.beans.GraphNode;
import software.wings.beans.template.BaseTemplate;
import software.wings.beans.template.Template;

import de.danielbechler.diff.ObjectDifferBuilder;
import de.danielbechler.diff.node.DiffNode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class StateTemplateProcessor extends AbstractTemplateProcessor {
  private static final String TEMPLATE_UUID = "templateUuid";
  private static final String TEMPLATE_VERSION = "templateVersion";
  private static final String TEMPLATE_VARIABLES = "templateVariables";
  @Override
  public void updateLinkedEntities(Template template) {
    // Read steps that references the given template
    Template savedTemplate = templateService.get(template.getUuid());
    if (savedTemplate == null) {
      log.info("Template {} was deleted. Not updating linked entities", template.getUuid());
      return;
    }
    updateLinkedEntitiesInWorkflow(template);
  }

  @Override
  public List<String> fetchTemplateProperties() {
    List<String> templateProperties = new ArrayList<>();
    templateProperties.addAll(asList(TEMPLATE_UUID, TEMPLATE_VERSION, TEMPLATE_VARIABLES));
    return templateProperties;
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
