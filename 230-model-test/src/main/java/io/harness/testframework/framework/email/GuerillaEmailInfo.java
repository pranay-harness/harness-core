/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.testframework.framework.email;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.HashMap;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"email_addr", "email_timestamp", "alias", "sid_token"})
public class GuerillaEmailInfo {
  @JsonProperty("email_addr") private String emailAddr;
  @JsonProperty("email_timestamp") private long emailTimestamp;
  @JsonProperty("alias") private String alias;
  @JsonProperty("sid_token") private String sidToken;
  @JsonIgnore private Map<String, Object> additionalProperties = new HashMap<>();

  @JsonProperty("email_addr")
  public String getEmailAddr() {
    return emailAddr;
  }

  @JsonProperty("email_addr")
  public void setEmailAddr(String emailAddr) {
    this.emailAddr = emailAddr;
  }

  @JsonProperty("email_timestamp")
  public long getEmailTimestamp() {
    return emailTimestamp;
  }

  @JsonProperty("email_timestamp")
  public void setEmailTimestamp(long emailTimestamp) {
    this.emailTimestamp = emailTimestamp;
  }

  @JsonProperty("alias")
  public String getAlias() {
    return alias;
  }

  @JsonProperty("alias")
  public void setAlias(String alias) {
    this.alias = alias;
  }

  @JsonProperty("sid_token")
  public String getSidToken() {
    return sidToken;
  }

  @JsonProperty("sid_token")
  public void setSidToken(String sidToken) {
    this.sidToken = sidToken;
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
