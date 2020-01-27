package software.wings.service.impl.template;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.Arrays.asList;
import static software.wings.common.TemplateConstants.HTTP_HEALTH_CHECK;

import com.google.inject.Singleton;

import software.wings.beans.template.Template;
import software.wings.beans.template.TemplateType;
import software.wings.beans.template.command.HttpTemplate;

import java.util.List;
import java.util.Map;

@Singleton
public class HttpTemplateProcessor extends StateTemplateProcessor {
  private static final String URL = "url";
  private static final String METHOD = "method";
  private static final String HEADER = "header";
  private static final String BODY = "body";
  private static final String ASSERTION = "assertion";
  private static final String SOCKET_TIMEOUT_MILLIS = "socketTimeoutMillis";
  private static final String VARIABLES = "variables";

  @Override
  public TemplateType getTemplateType() {
    return TemplateType.HTTP;
  }

  @Override
  public void loadDefaultTemplates(String accountId, String accountName) {
    super.loadDefaultTemplates(asList(HTTP_HEALTH_CHECK), accountId, accountName);
  }

  @Override
  public void transform(Template template, Map<String, Object> properties) {
    HttpTemplate httpTemplate = (HttpTemplate) template.getTemplateObject();
    if (isNotEmpty(httpTemplate.getUrl())) {
      properties.put(URL, httpTemplate.getUrl());
    }
    if (isNotEmpty(httpTemplate.getMethod())) {
      properties.put(METHOD, httpTemplate.getMethod());
    }
    if (isNotEmpty(httpTemplate.getHeader())) {
      properties.put(HEADER, httpTemplate.getHeader());
    }
    if (isNotEmpty(httpTemplate.getBody())) {
      properties.put(BODY, httpTemplate.getBody());
    }
    if (isNotEmpty(httpTemplate.getAssertion())) {
      properties.put(ASSERTION, httpTemplate.getAssertion());
    }
    properties.put(SOCKET_TIMEOUT_MILLIS, httpTemplate.getSocketTimeoutMillis());
  }

  @Override
  public List<String> fetchTemplateProperties() {
    List<String> templateProperties = super.fetchTemplateProperties();
    templateProperties.addAll(asList(URL, METHOD, HEADER, BODY, ASSERTION, VARIABLES));
    return templateProperties;
  }
}
