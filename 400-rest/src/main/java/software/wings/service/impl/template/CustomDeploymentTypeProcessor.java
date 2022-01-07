/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.template;

import static software.wings.beans.template.TemplateType.CUSTOM_DEPLOYMENT_TYPE;

import static org.apache.commons.lang3.StringUtils.trim;

import software.wings.beans.EntityType;
import software.wings.beans.template.BaseTemplate;
import software.wings.beans.template.Template;
import software.wings.beans.template.TemplateType;
import software.wings.beans.template.deploymenttype.CustomDeploymentTypeTemplate;

import de.danielbechler.diff.ObjectDifferBuilder;
import de.danielbechler.diff.node.DiffNode;
import java.util.List;
import java.util.Optional;

public class CustomDeploymentTypeProcessor extends AbstractTemplateProcessor {
  /**
   * Process the template
   *
   * @param template
   */
  @Override
  public Template process(Template template) {
    CustomDeploymentTypeTemplate customDeploymentTypeTemplate =
        (CustomDeploymentTypeTemplate) template.getTemplateObject();
    if (customDeploymentTypeTemplate != null) {
      final CustomDeploymentTypeTemplate processedTemplateObject =
          customDeploymentTypeTemplate.but()
              .hostObjectArrayPath(trim(customDeploymentTypeTemplate.getHostObjectArrayPath()))
              .build();
      Optional.ofNullable(processedTemplateObject.getHostAttributes())
          .ifPresent(map -> map.replaceAll((k, v) -> v.trim()));
      template.setTemplateObject(processedTemplateObject);
    }

    template.setType(getTemplateType().name());
    return template;
  }

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
