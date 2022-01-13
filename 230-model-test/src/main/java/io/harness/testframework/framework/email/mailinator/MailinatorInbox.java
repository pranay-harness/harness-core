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
@JsonPropertyOrder({"messages", "to"})
public class MailinatorInbox {
  @JsonProperty("messages") private List<MailinatorMetaMessage> messages;
  @JsonProperty("to") private String to;
  @JsonIgnore private Map<String, Object> additionalProperties = new HashMap<>();

  @JsonProperty("messages")
  public List<MailinatorMetaMessage> getMessages() {
    return messages;
  }

  @JsonProperty("messages")
  public void setMessages(List<MailinatorMetaMessage> messages) {
    this.messages = messages;
  }

  @JsonProperty("to")
  public String getTo() {
    return to;
  }

  @JsonProperty("to")
  public void setTo(String to) {
    this.to = to;
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
