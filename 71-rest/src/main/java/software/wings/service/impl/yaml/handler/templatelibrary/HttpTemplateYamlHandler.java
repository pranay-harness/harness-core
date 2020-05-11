package software.wings.service.impl.yaml.handler.templatelibrary;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.google.inject.Singleton;

import io.harness.annotations.dev.OwnedBy;
import software.wings.beans.template.BaseTemplate;
import software.wings.beans.template.Template;
import software.wings.beans.template.command.HttpTemplate;
import software.wings.beans.yaml.ChangeContext;
import software.wings.yaml.templatelibrary.HttpTemplateYaml;

import java.util.List;

@OwnedBy(CDC)
@Singleton

public class HttpTemplateYamlHandler extends TemplateLibraryYamlHandler<HttpTemplateYaml> {
  @Override
  protected void setBaseTemplate(
      Template template, ChangeContext<HttpTemplateYaml> changeContext, List<ChangeContext> changeSetContext) {
    HttpTemplateYaml yaml = changeContext.getYaml();
    BaseTemplate baseTemplate = HttpTemplate.builder()
                                    .assertion(yaml.getAssertion())
                                    .url(yaml.getUrl())
                                    .header(yaml.getHeader())
                                    .method(yaml.getMethod())
                                    .body(yaml.getBody())
                                    .socketTimeoutMillis(yaml.getTimeoutMillis())
                                    .build();
    template.setTemplateObject(baseTemplate);
  }

  @Override
  public HttpTemplateYaml toYaml(Template bean, String appId) {
    HttpTemplate httpTemplateBean = (HttpTemplate) bean.getTemplateObject();
    HttpTemplateYaml httpTemplateYaml = HttpTemplateYaml.builder()
                                            .assertion(httpTemplateBean.getAssertion())
                                            .header(httpTemplateBean.getHeader())
                                            .method(httpTemplateBean.getMethod())
                                            .timeOutMillis(httpTemplateBean.getSocketTimeoutMillis())
                                            .url(httpTemplateBean.getUrl())
                                            .body(httpTemplateBean.getBody())
                                            .build();
    super.toYaml(httpTemplateYaml, bean);
    return httpTemplateYaml;
  }
}
