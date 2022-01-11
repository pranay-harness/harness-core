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
@JsonPropertyOrder({"mail_id", "mail_from", "mail_recipient", "mail_subject", "mail_excerpt", "mail_body",
    "mail_timestamp", "mail_date", "mail_read", "content_type", "source_id", "source_mail_id", "reply_to", "mail_size",
    "ver", "ref_mid", "sid_token", "auth"})
public class GuerillaIndividualEmail {
  @JsonProperty("mail_id") private String mailId;
  @JsonProperty("mail_from") private String mailFrom;
  @JsonProperty("mail_recipient") private String mailRecipient;
  @JsonProperty("mail_subject") private String mailSubject;
  @JsonProperty("mail_excerpt") private String mailExcerpt;
  @JsonProperty("mail_body") private String mailBody;
  @JsonProperty("mail_timestamp") private String mailTimestamp;
  @JsonProperty("mail_date") private String mailDate;
  @JsonProperty("mail_read") private String mailRead;
  @JsonProperty("content_type") private String contentType;
  @JsonProperty("source_id") private String sourceId;
  @JsonProperty("source_mail_id") private String sourceMailId;
  @JsonProperty("reply_to") private String replyTo;
  @JsonProperty("mail_size") private String mailSize;
  @JsonProperty("ver") private String ver;
  @JsonProperty("ref_mid") private String refMid;
  @JsonProperty("sid_token") private String sidToken;
  @JsonProperty("auth") private Auth auth;
  @JsonIgnore private Map<String, Object> additionalProperties = new HashMap<>();

  @JsonProperty("mail_id")
  public String getMailId() {
    return mailId;
  }

  @JsonProperty("mail_id")
  public void setMailId(String mailId) {
    this.mailId = mailId;
  }

  @JsonProperty("mail_from")
  public String getMailFrom() {
    return mailFrom;
  }

  @JsonProperty("mail_from")
  public void setMailFrom(String mailFrom) {
    this.mailFrom = mailFrom;
  }

  @JsonProperty("mail_recipient")
  public String getMailRecipient() {
    return mailRecipient;
  }

  @JsonProperty("mail_recipient")
  public void setMailRecipient(String mailRecipient) {
    this.mailRecipient = mailRecipient;
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

  @JsonProperty("mail_body")
  public String getMailBody() {
    return mailBody;
  }

  @JsonProperty("mail_body")
  public void setMailBody(String mailBody) {
    this.mailBody = mailBody;
  }

  @JsonProperty("mail_timestamp")
  public String getMailTimestamp() {
    return mailTimestamp;
  }

  @JsonProperty("mail_timestamp")
  public void setMailTimestamp(String mailTimestamp) {
    this.mailTimestamp = mailTimestamp;
  }

  @JsonProperty("mail_date")
  public String getMailDate() {
    return mailDate;
  }

  @JsonProperty("mail_date")
  public void setMailDate(String mailDate) {
    this.mailDate = mailDate;
  }

  @JsonProperty("mail_read")
  public String getMailRead() {
    return mailRead;
  }

  @JsonProperty("mail_read")
  public void setMailRead(String mailRead) {
    this.mailRead = mailRead;
  }

  @JsonProperty("content_type")
  public String getContentType() {
    return contentType;
  }

  @JsonProperty("content_type")
  public void setContentType(String contentType) {
    this.contentType = contentType;
  }

  @JsonProperty("source_id")
  public String getSourceId() {
    return sourceId;
  }

  @JsonProperty("source_id")
  public void setSourceId(String sourceId) {
    this.sourceId = sourceId;
  }

  @JsonProperty("source_mail_id")
  public String getSourceMailId() {
    return sourceMailId;
  }

  @JsonProperty("source_mail_id")
  public void setSourceMailId(String sourceMailId) {
    this.sourceMailId = sourceMailId;
  }

  @JsonProperty("reply_to")
  public String getReplyTo() {
    return replyTo;
  }

  @JsonProperty("reply_to")
  public void setReplyTo(String replyTo) {
    this.replyTo = replyTo;
  }

  @JsonProperty("mail_size")
  public String getMailSize() {
    return mailSize;
  }

  @JsonProperty("mail_size")
  public void setMailSize(String mailSize) {
    this.mailSize = mailSize;
  }

  @JsonProperty("ver")
  public String getVer() {
    return ver;
  }

  @JsonProperty("ver")
  public void setVer(String ver) {
    this.ver = ver;
  }

  @JsonProperty("ref_mid")
  public String getRefMid() {
    return refMid;
  }

  @JsonProperty("ref_mid")
  public void setRefMid(String refMid) {
    this.refMid = refMid;
  }

  @JsonProperty("sid_token")
  public String getSidToken() {
    return sidToken;
  }

  @JsonProperty("sid_token")
  public void setSidToken(String sidToken) {
    this.sidToken = sidToken;
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
