package software.wings.service.impl.yaml.handler.templatelibrary;

import com.google.inject.Singleton;

import software.wings.beans.template.BaseTemplate;
import software.wings.beans.template.Template;
import software.wings.beans.template.command.PcfCommandTemplate;
import software.wings.beans.yaml.ChangeContext;
import software.wings.yaml.templatelibrary.PcfCommandTemplateYaml;

import java.util.List;

@Singleton
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
