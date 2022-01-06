/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.yaml.handler.templatelibrary;

import static io.harness.annotations.dev.HarnessTeam.CDC;

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
                                      .headers(yaml.getHeaders())
                                      .timeoutMillis(yaml.getTimeoutMillis());
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
                                          .headers(httpTemplateBean.getHeaders())
                                          .body(httpTemplateBean.getBody());
    HttpTemplateYaml httpTemplateYaml = builder.build();
    super.toYaml(httpTemplateYaml, bean);
    return httpTemplateYaml;
  }
}
