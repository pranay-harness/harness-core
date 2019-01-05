package software.wings.service.impl.template;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static software.wings.common.TemplateConstants.HTTP_HEALTH_CHECK;

import com.google.inject.Singleton;

import software.wings.beans.template.Template;
import software.wings.beans.template.TemplateType;
import software.wings.beans.template.command.HttpTemplate;

import java.util.Arrays;
import java.util.Map;

@Singleton
public class HttpTemplateProcessor extends StateTemplateProcessor {
  @Override
  public TemplateType getTemplateType() {
    return TemplateType.HTTP;
  }

  @Override
  public void loadDefaultTemplates(String accountId, String accountName) {
    super.loadDefaultTemplates(Arrays.asList(HTTP_HEALTH_CHECK), accountId, accountName);
  }

  @Override
  public void transform(Template template, Map<String, Object> properties) {
    HttpTemplate httpTemplate = (HttpTemplate) template.getTemplateObject();
    if (isNotEmpty(httpTemplate.getUrl())) {
      properties.put("url", httpTemplate.getUrl());
    }
    if (isNotEmpty(httpTemplate.getMethod())) {
      properties.put("method", httpTemplate.getMethod());
    }
    if (isNotEmpty(httpTemplate.getHeader())) {
      properties.put("header", httpTemplate.getHeader());
    }
    if (isNotEmpty(httpTemplate.getBody())) {
      properties.put("body", httpTemplate.getBody());
    }
    if (isNotEmpty(httpTemplate.getAssertion())) {
      properties.put("assertion", httpTemplate.getAssertion());
    }
    properties.put("socketTimeoutMillis", httpTemplate.getSocketTimeoutMillis());
  }
}
