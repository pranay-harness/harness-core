/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.testframework.framework.email.mailinator;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"fromfull", "headers", "subject", "parts", "from", "origfrom", "to", "id", "time", "seconds_ago"})
public class Data {
  @JsonProperty("fromfull") private String fromfull;
  @JsonProperty("headers") private Headers headers;
  @JsonProperty("subject") private String subject;
  @JsonProperty("parts") private List<Part> parts;
  @JsonProperty("from") private String from;
  @JsonProperty("origfrom") private String origfrom;
  @JsonProperty("to") private String to;
  @JsonProperty("id") private String id;
  @JsonProperty("time") private Long time;
  @JsonProperty("seconds_ago") private Long secondsAgo;
  @JsonIgnore private Map<String, Object> additionalProperties = new HashMap<>();

  @JsonProperty("fromfull")
  public String getFromfull() {
    return fromfull;
  }

  @JsonProperty("fromfull")
  public void setFromfull(String fromfull) {
    this.fromfull = fromfull;
  }

  @JsonProperty("headers")
  public Headers getHeaders() {
    return headers;
  }

  @JsonProperty("headers")
  public void setHeaders(Headers headers) {
    this.headers = headers;
  }

  @JsonProperty("subject")
  public String getSubject() {
    return subject;
  }

  @JsonProperty("subject")
  public void setSubject(String subject) {
    this.subject = subject;
  }

  @JsonProperty("parts")
  public List<Part> getParts() {
    return parts;
  }

  @JsonProperty("parts")
  public void setParts(List<Part> parts) {
    this.parts = parts;
  }

  @JsonProperty("from")
  public String getFrom() {
    return from;
  }

  @JsonProperty("from")
  public void setFrom(String from) {
    this.from = from;
  }

  @JsonProperty("origfrom")
  public String getOrigfrom() {
    return origfrom;
  }

  @JsonProperty("origfrom")
  public void setOrigfrom(String origfrom) {
    this.origfrom = origfrom;
  }

  @JsonProperty("to")
  public String getTo() {
    return to;
  }

  @JsonProperty("to")
  public void setTo(String to) {
    this.to = to;
  }

  @JsonProperty("id")
  public String getId() {
    return id;
  }

  @JsonProperty("id")
  public void setId(String id) {
    this.id = id;
  }

  @JsonProperty("time")
  public Long getTime() {
    return time;
  }

  @JsonProperty("time")
  public void setTime(Long time) {
    this.time = time;
  }

  @JsonProperty("seconds_ago")
  public Long getSecondsAgo() {
    return secondsAgo;
  }

  @JsonProperty("seconds_ago")
  public void setSecondsAgo(Long secondsAgo) {
    this.secondsAgo = secondsAgo;
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
