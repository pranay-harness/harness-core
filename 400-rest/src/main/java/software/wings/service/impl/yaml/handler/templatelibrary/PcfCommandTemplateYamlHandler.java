/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.service.impl.yaml.handler.templatelibrary;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.template.BaseTemplate;
import software.wings.beans.template.Template;
import software.wings.beans.template.command.PcfCommandTemplate;
import software.wings.beans.yaml.ChangeContext;
import software.wings.yaml.templatelibrary.PcfCommandTemplateYaml;

import com.google.inject.Singleton;
import java.util.List;

@Singleton
@OwnedBy(CDP)
public class PcfCommandTemplateYamlHandler extends TemplateLibraryYamlHandler<PcfCommandTemplateYaml> {
  @Override
  public PcfCommandTemplateYaml toYaml(Template bean, String appId) {
    PcfCommandTemplate pcfCommandTemplateBean = (PcfCommandTemplate) bean.getTemplateObject();
    PcfCommandTemplateYaml pcfCommandTemplateYaml =
        PcfCommandTemplateYaml.builder()
            .scriptString(pcfCommandTemplateBean.getScriptString())
            .timeoutIntervalInMinutes(pcfCommandTemplateBean.getTimeoutIntervalInMinutes())
            .build();
    super.toYaml(pcfCommandTemplateYaml, bean);
    return pcfCommandTemplateYaml;
  }

  @Override
  protected void setBaseTemplate(
      Template template, ChangeContext<PcfCommandTemplateYaml> changeContext, List<ChangeContext> changeSetContext) {
    PcfCommandTemplateYaml yaml = changeContext.getYaml();
    BaseTemplate baseTemplate = PcfCommandTemplate.builder()
                                    .scriptString(yaml.getScriptString())
                                    .timeoutIntervalInMinutes(yaml.getTimeoutIntervalInMinutes())
                                    .build();
    template.setTemplateObject(baseTemplate);
  }
}
