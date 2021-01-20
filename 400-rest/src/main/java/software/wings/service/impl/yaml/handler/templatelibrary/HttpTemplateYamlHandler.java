package software.wings.service.impl.yaml.handler.templatelibrary;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.template.Template;
import software.wings.beans.template.command.HttpTemplate;
import software.wings.beans.template.command.HttpTemplate.HttpTemplateBuilder;
import software.wings.beans.yaml.ChangeContext;
import software.wings.yaml.templatelibrary.HttpTemplateYaml;
import software.wings.yaml.templatelibrary.HttpTemplateYaml.HttpTemplateYamlBuilder;

import com.google.inject.Singleton;
import java.util.List;

@OwnedBy(CDC)
@Singleton

public class HttpTemplateYamlHandler extends TemplateLibraryYamlHandler<HttpTemplateYaml> {
  @Override
  protected void setBaseTemplate(
      Template template, ChangeContext<HttpTemplateYaml> changeContext, List<ChangeContext> changeSetContext) {
    HttpTemplateYaml yaml = changeContext.getYaml();
    HttpTemplateBuilder builder = HttpTemplate.builder()
                                      .assertion(yaml.getAssertion())
                                      .url(yaml.getUrl())
                                      .method(yaml.getMethod())
                                      .body(yaml.getBody())
                                      .timeoutMillis(yaml.getTimeoutMillis());

    if (isNotEmpty(yaml.getHeaders())) {
      builder.headers(yaml.getHeaders());
    } else {
      builder.header(yaml.getHeader());
    }
    template.setTemplateObject(builder.build());
  }

  @Override
  public HttpTemplateYaml toYaml(Template bean, String appId) {
    HttpTemplate httpTemplateBean = (HttpTemplate) bean.getTemplateObject();
    HttpTemplateYamlBuilder builder = HttpTemplateYaml.builder()
                                          .assertion(httpTemplateBean.getAssertion())
                                          .method(httpTemplateBean.getMethod())
                                          .timeOutMillis(httpTemplateBean.getTimeoutMillis())
                                          .url(httpTemplateBean.getUrl())
                                          .body(httpTemplateBean.getBody());
    if (isNotEmpty(httpTemplateBean.getHeaders())) {
      builder.headers(httpTemplateBean.getHeaders());
    } else {
      builder.header(httpTemplateBean.getHeader());
    }
    HttpTemplateYaml httpTemplateYaml = builder.build();
    super.toYaml(httpTemplateYaml, bean);
    return httpTemplateYaml;
  }
}
