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
@JsonPropertyOrder({"list", "count", "email", "alias", "ts", "sid_token", "stats", "auth"})
public class GuerillaEmailDetails {
  @JsonProperty("count") private String count;
  @JsonProperty("email") private String email;
  @JsonProperty("alias") private String alias;
  @JsonProperty("ts") private long ts;
  @JsonProperty("sid_token") private String sidToken;
  @JsonProperty("stats") private Stats stats;
  @JsonProperty("auth") private Auth auth;
  @JsonIgnore private Map<String, Object> additionalProperties = new HashMap<>();

  @JsonProperty("count")
  public String getCount() {
    return count;
  }

  @JsonProperty("count")
  public void setCount(String count) {
    this.count = count;
  }

  @JsonProperty("email")
  public String getEmail() {
    return email;
  }

  @JsonProperty("email")
  public void setEmail(String email) {
    this.email = email;
  }

  @JsonProperty("alias")
  public String getAlias() {
    return alias;
  }

  @JsonProperty("alias")
  public void setAlias(String alias) {
    this.alias = alias;
  }

  @JsonProperty("ts")
  public long getTs() {
    return ts;
  }

  @JsonProperty("ts")
  public void setTs(long ts) {
    this.ts = ts;
  }

  @JsonProperty("sid_token")
  public String getSidToken() {
    return sidToken;
  }

  @JsonProperty("sid_token")
  public void setSidToken(String sidToken) {
    this.sidToken = sidToken;
  }

  @JsonProperty("stats")
  public Stats getStats() {
    return stats;
  }

  @JsonProperty("stats")
  public void setStats(Stats stats) {
    this.stats = stats;
  }

  @JsonProperty("auth")
  public Auth getAuth() {
    return auth;
  }

  @JsonProperty("auth")
  public void setAuth(Auth auth) {
    this.auth = auth;
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
