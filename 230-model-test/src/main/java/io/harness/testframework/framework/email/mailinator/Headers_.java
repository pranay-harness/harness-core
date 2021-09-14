/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */


package io.harness.testframework.framework.email.mailinator;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.HashMap;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"content-transfer-encoding", "content-type"})
public class Headers_ {
  @JsonProperty("content-transfer-encoding") private String contentTransferEncoding;
  @JsonProperty("content-type") private String contentType;
  @JsonIgnore private Map<String, Object> additionalProperties = new HashMap<>();

  @JsonProperty("content-transfer-encoding")
  public String getContentTransferEncoding() {
    return contentTransferEncoding;
  }

  @JsonProperty("content-transfer-encoding")
  public void setContentTransferEncoding(String contentTransferEncoding) {
    this.contentTransferEncoding = contentTransferEncoding;
  }

  @JsonProperty("content-type")
  public String getContentType() {
    return contentType;
  }

  @JsonProperty("content-type")
  public void setContentType(String contentType) {
    this.contentType = contentType;
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
