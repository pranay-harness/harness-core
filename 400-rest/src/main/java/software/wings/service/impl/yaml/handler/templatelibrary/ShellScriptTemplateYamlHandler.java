/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.yaml.handler.templatelibrary;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.template.BaseTemplate;
import software.wings.beans.template.Template;
import software.wings.beans.template.command.ShellScriptTemplate;
import software.wings.beans.yaml.ChangeContext;
import software.wings.yaml.templatelibrary.ShellScriptTemplateYaml;

import com.google.inject.Singleton;
import java.util.List;

@OwnedBy(CDC)
@Singleton
public class ShellScriptTemplateYamlHandler extends TemplateLibraryYamlHandler<ShellScriptTemplateYaml> {
  @Override
  public ShellScriptTemplateYaml toYaml(Template bean, String appId) {
    ShellScriptTemplate shellScriptTemplateBean = (ShellScriptTemplate) bean.getTemplateObject();
    ShellScriptTemplateYaml shellScriptTemplateYaml =
        ShellScriptTemplateYaml.builder()
            .outputVars(shellScriptTemplateBean.getOutputVars())
            .scriptString(shellScriptTemplateBean.getScriptString())
            .scriptType(shellScriptTemplateBean.getScriptType())
            .timeOutMillis(shellScriptTemplateBean.getTimeoutMillis())
            .secretOutputVars(shellScriptTemplateBean.getSecretOutputVars())
            .build();
    super.toYaml(shellScriptTemplateYaml, bean);
    return shellScriptTemplateYaml;
  }

  @Override
  protected void setBaseTemplate(
      Template template, ChangeContext<ShellScriptTemplateYaml> changeContext, List<ChangeContext> changeSetContext) {
    ShellScriptTemplateYaml yaml = changeContext.getYaml();
    BaseTemplate baseTemplate = ShellScriptTemplate.builder()
                                    .scriptString(yaml.getScriptString())
                                    .outputVars(yaml.getOutputVars())
                                    .secretOutputVars(yaml.getSecretOutputVars())
                                    .scriptType(yaml.getScriptType())
                                    .timeoutMillis(yaml.getTimeoutMillis())
                                    .build();
    template.setTemplateObject(baseTemplate);
  }
}
