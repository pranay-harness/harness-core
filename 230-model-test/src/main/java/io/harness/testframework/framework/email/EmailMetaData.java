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
@JsonPropertyOrder({"mail_from", "mail_timestamp", "mail_read", "mail_date", "reply_to", "mail_subject", "mail_excerpt",
    "mail_id", "att", "content_type", "mail_recipient", "source_id", "source_mail_id", "mail_body", "size"})
public class EmailMetaData {
  @JsonProperty("mail_from") private String mailFrom;
  @JsonProperty("mail_timestamp") private long mailTimestamp;
  @JsonProperty("mail_read") private long mailRead;
  @JsonProperty("mail_date") private String mailDate;
  @JsonProperty("reply_to") private String replyTo;
  @JsonProperty("mail_subject") private String mailSubject;
  @JsonProperty("mail_excerpt") private String mailExcerpt;
  @JsonProperty("mail_id") private long mailId;
  @JsonProperty("att") private long att;
  @JsonProperty("content_type") private String contentType;
  @JsonProperty("mail_recipient") private String mailRecipient;
  @JsonProperty("source_id") private long sourceId;
  @JsonProperty("source_mail_id") private long sourceMailId;
  @JsonProperty("mail_body") private String mailBody;
  @JsonProperty("size") private long size;
  @JsonIgnore private Map<String, Object> additionalProperties = new HashMap<>();

  @JsonProperty("mail_from")
  public String getMailFrom() {
    return mailFrom;
  }

  @JsonProperty("mail_from")
  public void setMailFrom(String mailFrom) {
    this.mailFrom = mailFrom;
  }

  @JsonProperty("mail_timestamp")
  public long getMailTimestamp() {
    return mailTimestamp;
  }

  @JsonProperty("mail_timestamp")
  public void setMailTimestamp(long mailTimestamp) {
    this.mailTimestamp = mailTimestamp;
  }

  @JsonProperty("mail_read")
  public long getMailRead() {
    return mailRead;
  }

  @JsonProperty("mail_read")
  public void setMailRead(long mailRead) {
    this.mailRead = mailRead;
  }

  @JsonProperty("mail_date")
  public String getMailDate() {
    return mailDate;
  }

  @JsonProperty("mail_date")
  public void setMailDate(String mailDate) {
    this.mailDate = mailDate;
  }

  @JsonProperty("reply_to")
  public String getReplyTo() {
    return replyTo;
  }

  @JsonProperty("reply_to")
  public void setReplyTo(String replyTo) {
    this.replyTo = replyTo;
  }

  @JsonProperty("mail_subject")
  public String getMailSubject() {
    return mailSubject;
  }

  @JsonProperty("mail_subject")
  public void setMailSubject(String mailSubject) {
    this.mailSubject = mailSubject;
  }

  @JsonProperty("mail_excerpt")
  public String getMailExcerpt() {
    return mailExcerpt;
  }

  @JsonProperty("mail_excerpt")
  public void setMailExcerpt(String mailExcerpt) {
    this.mailExcerpt = mailExcerpt;
  }

  @JsonProperty("mail_id")
  public long getMailId() {
    return mailId;
  }

  @JsonProperty("mail_id")
  public void setMailId(long mailId) {
    this.mailId = mailId;
  }

  @JsonProperty("att")
  public long getAtt() {
    return att;
  }

  @JsonProperty("att")
  public void setAtt(long att) {
    this.att = att;
  }

  @JsonProperty("content_type")
  public String getContentType() {
    return contentType;
  }

  @JsonProperty("content_type")
  public void setContentType(String contentType) {
    this.contentType = contentType;
  }

  @JsonProperty("mail_recipient")
  public String getMailRecipient() {
    return mailRecipient;
  }

  @JsonProperty("mail_recipient")
  public void setMailRecipient(String mailRecipient) {
    this.mailRecipient = mailRecipient;
  }

  @JsonProperty("source_id")
  public long getSourceId() {
    return sourceId;
  }

  @JsonProperty("source_id")
  public void setSourceId(long sourceId) {
    this.sourceId = sourceId;
  }

  @JsonProperty("source_mail_id")
  public long getSourceMailId() {
    return sourceMailId;
  }

  @JsonProperty("source_mail_id")
  public void setSourceMailId(long sourceMailId) {
    this.sourceMailId = sourceMailId;
  }

  @JsonProperty("mail_body")
  public String getMailBody() {
    return mailBody;
  }

  @JsonProperty("mail_body")
  public void setMailBody(String mailBody) {
    this.mailBody = mailBody;
  }

  @JsonProperty("size")
  public long getSize() {
    return size;
  }

  @JsonProperty("size")
  public void setSize(long size) {
    this.size = size;
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
