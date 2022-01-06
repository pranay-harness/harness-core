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
@JsonPropertyOrder({"date", "mime-version", "subject", "x-sg-eid", "message-id", "received", "from", "content-type",
    "to", "reply-to", "dkim-signature"})
public class Headers {
  @JsonProperty("date") private String date;
  @JsonProperty("mime-version") private String mimeVersion;
  @JsonProperty("subject") private String subject;
  @JsonProperty("x-sg-eid") private String xSgEid;
  @JsonProperty("message-id") private String messageId;
  @JsonProperty("received") private List<String> received;
  @JsonProperty("from") private String from;
  @JsonProperty("content-type") private String contentType;
  @JsonProperty("to") private String to;
  @JsonProperty("reply-to") private String replyTo;
  @JsonProperty("dkim-signature") private String dkimSignature;
  @JsonIgnore private Map<String, Object> additionalProperties = new HashMap<>();

  @JsonProperty("date")
  public String getDate() {
    return date;
  }

  @JsonProperty("date")
  public void setDate(String date) {
    this.date = date;
  }

  @JsonProperty("mime-version")
  public String getMimeVersion() {
    return mimeVersion;
  }

  @JsonProperty("mime-version")
  public void setMimeVersion(String mimeVersion) {
    this.mimeVersion = mimeVersion;
  }

  @JsonProperty("subject")
  public String getSubject() {
    return subject;
  }

  @JsonProperty("subject")
  public void setSubject(String subject) {
    this.subject = subject;
  }

  @JsonProperty("x-sg-eid")
  public String getXSgEid() {
    return xSgEid;
  }

  @JsonProperty("x-sg-eid")
  public void setXSgEid(String xSgEid) {
    this.xSgEid = xSgEid;
  }

  @JsonProperty("message-id")
  public String getMessageId() {
    return messageId;
  }

  @JsonProperty("message-id")
  public void setMessageId(String messageId) {
    this.messageId = messageId;
  }

  @JsonProperty("received")
  public List<String> getReceived() {
    return received;
  }

  @JsonProperty("received")
  public void setReceived(List<String> received) {
    this.received = received;
  }

  @JsonProperty("from")
  public String getFrom() {
    return from;
  }

  @JsonProperty("from")
  public void setFrom(String from) {
    this.from = from;
  }

  @JsonProperty("content-type")
  public String getContentType() {
    return contentType;
  }

  @JsonProperty("content-type")
  public void setContentType(String contentType) {
    this.contentType = contentType;
  }

  @JsonProperty("to")
  public String getTo() {
    return to;
  }

  @JsonProperty("to")
  public void setTo(String to) {
    this.to = to;
  }

  @JsonProperty("reply-to")
  public String getReplyTo() {
    return replyTo;
  }

  @JsonProperty("reply-to")
  public void setReplyTo(String replyTo) {
    this.replyTo = replyTo;
  }

  @JsonProperty("dkim-signature")
  public String getDkimSignature() {
    return dkimSignature;
  }

  @JsonProperty("dkim-signature")
  public void setDkimSignature(String dkimSignature) {
    this.dkimSignature = dkimSignature;
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
