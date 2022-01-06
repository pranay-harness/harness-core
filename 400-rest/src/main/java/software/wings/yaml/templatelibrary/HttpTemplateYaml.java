/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.yaml.templatelibrary;

import static software.wings.common.TemplateConstants.HTTP;

import io.harness.beans.KeyValuePair;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonTypeName(HTTP)
@JsonPropertyOrder({"harnessApiVersion"})
public class HttpTemplateYaml extends TemplateLibraryYaml {
  private String url;
  private String method;
  private String header;
  private List<KeyValuePair> headers;
  private String body;
  private String assertion;
  private int timeoutMillis = 10000;

  @Builder
  public HttpTemplateYaml(String type, String harnessApiVersion, String description, String url, String body,
      String method, String header, List<KeyValuePair> headers, String assertion, int timeOutMillis,
      List<TemplateVariableYaml> templateVariableYamlList) {
    super(type, harnessApiVersion, description, templateVariableYamlList);
    setAssertion(assertion);
    setMethod(method);
    setUrl(url);
    setHeader(header);
    setHeaders(headers);
    setBody(body);
    setTimeoutMillis(timeOutMillis);
  }
}
