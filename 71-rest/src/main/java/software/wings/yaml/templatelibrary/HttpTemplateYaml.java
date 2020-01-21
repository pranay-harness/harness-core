package software.wings.yaml.templatelibrary;

import static software.wings.common.TemplateConstants.HTTP;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonTypeName(HTTP)
@JsonPropertyOrder({"harnessApiVersion"})
public class HttpTemplateYaml extends TemplateLibraryYaml {
  private String url;
  private String method;
  private String header;
  private String body;
  private String assertion;
  private int timeoutMillis = 10000;

  @Builder
  public HttpTemplateYaml(String type, String harnessApiVersion, String description, String url, String body,
      String method, String header, String assertion, int timeOutMillis,
      List<TemplateVariableYaml> templateVariableYamlList) {
    super(type, harnessApiVersion, description, templateVariableYamlList);
    setAssertion(assertion);
    setMethod(method);
    setUrl(url);
    setHeader(header);
    setBody(body);
    setTimeoutMillis(timeOutMillis);
  }
}
