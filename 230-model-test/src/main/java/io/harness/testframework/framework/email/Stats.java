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
@JsonPropertyOrder({"sequence_mail", "created_addresses", "received_emails", "total", "total_per_hour"})
public class Stats {
  @JsonProperty("sequence_mail") private String sequenceMail;
  @JsonProperty("created_addresses") private long createdAddresses;
  @JsonProperty("received_emails") private String receivedEmails;
  @JsonProperty("total") private String total;
  @JsonProperty("total_per_hour") private String totalPerHour;
  @JsonIgnore private Map<String, Object> additionalProperties = new HashMap<>();

  @JsonProperty("sequence_mail")
  public String getSequenceMail() {
    return sequenceMail;
  }

  @JsonProperty("sequence_mail")
  public void setSequenceMail(String sequenceMail) {
    this.sequenceMail = sequenceMail;
  }

  @JsonProperty("created_addresses")
  public long getCreatedAddresses() {
    return createdAddresses;
  }

  @JsonProperty("created_addresses")
  public void setCreatedAddresses(long createdAddresses) {
    this.createdAddresses = createdAddresses;
  }

  @JsonProperty("received_emails")
  public String getReceivedEmails() {
    return receivedEmails;
  }

  @JsonProperty("received_emails")
  public void setReceivedEmails(String receivedEmails) {
    this.receivedEmails = receivedEmails;
  }

  @JsonProperty("total")
  public String getTotal() {
    return total;
  }

  @JsonProperty("total")
  public void setTotal(String total) {
    this.total = total;
  }

  @JsonProperty("total_per_hour")
  public String getTotalPerHour() {
    return totalPerHour;
  }

  @JsonProperty("total_per_hour")
  public void setTotalPerHour(String totalPerHour) {
    this.totalPerHour = totalPerHour;
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
