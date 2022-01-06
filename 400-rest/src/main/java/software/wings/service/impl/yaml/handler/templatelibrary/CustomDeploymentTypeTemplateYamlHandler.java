/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.yaml.handler.templatelibrary;

import software.wings.beans.template.Template;
import software.wings.beans.template.deploymenttype.CustomDeploymentTypeTemplate;
import software.wings.beans.yaml.ChangeContext;
import software.wings.yaml.templatelibrary.CustomDeploymentTypeTemplateYaml;

import java.util.List;

public class CustomDeploymentTypeTemplateYamlHandler
    extends TemplateLibraryYamlHandler<CustomDeploymentTypeTemplateYaml> {
  @Override
  protected void setBaseTemplate(Template template, ChangeContext<CustomDeploymentTypeTemplateYaml> changeContext,
      List<ChangeContext> changeSetContext) {
    CustomDeploymentTypeTemplateYaml yaml = changeContext.getYaml();
    template.setTemplateObject(CustomDeploymentTypeTemplate.builder()
                                   .fetchInstanceScript(yaml.getFetchInstanceScript())
                                   .hostObjectArrayPath(yaml.getHostObjectArrayPath())
                                   .hostAttributes(yaml.getHostAttributes())
                                   .build());
  }

  @Override
  public CustomDeploymentTypeTemplateYaml toYaml(Template bean, String appId) {
    CustomDeploymentTypeTemplate template = (CustomDeploymentTypeTemplate) bean.getTemplateObject();
    CustomDeploymentTypeTemplateYaml yaml = CustomDeploymentTypeTemplateYaml.builder()
                                                .fetchInstanceScript(template.getFetchInstanceScript())
                                                .hostAttributes(template.getHostAttributes())
                                                .hostObjectArrayPath(template.getHostObjectArrayPath())
                                                .build();
    super.toYaml(yaml, bean);
    return yaml;
  }
}
