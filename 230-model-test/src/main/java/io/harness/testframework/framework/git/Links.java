/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.testframework.framework.git;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.HashMap;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"self", "git", "html"})
public class Links {
  @JsonProperty("self") private String self;
  @JsonProperty("git") private String git;
  @JsonProperty("html") private String html;
  @JsonIgnore private Map<String, Object> additionalProperties = new HashMap<>();

  @JsonProperty("self")
  public String getSelf() {
    return self;
  }

  @JsonProperty("self")
  public void setSelf(String self) {
    this.self = self;
  }

  @JsonProperty("git")
  public String getGit() {
    return git;
  }

  @JsonProperty("git")
  public void setGit(String git) {
    this.git = git;
  }

  @JsonProperty("html")
  public String getHtml() {
    return html;
  }

  @JsonProperty("html")
  public void setHtml(String html) {
    this.html = html;
  }

  @JsonAnyGetter
  public Map<String, Object> getAdditionalProperties() {
    return this.additionalProperties;
  }

  @JsonAnySetter
  public void setAdditionalProperty(String name, Object value) {
    this.additionalProperties.put(name, value);
  }
}
