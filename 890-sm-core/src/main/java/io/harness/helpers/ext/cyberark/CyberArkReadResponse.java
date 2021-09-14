/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.helpers.ext.cyberark;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

/**
 * @author marklu on 2019-08-01
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
@OwnedBy(PL)
public class CyberArkReadResponse {
  @JsonProperty("Name") private String name;
  @JsonProperty("UserName") private String userName;
  @JsonProperty("Content") private String content;
  @JsonProperty("Folder") private String folder;
  @JsonProperty("Safe") private String safe;
  @JsonProperty("Address") private String address;
  @JsonProperty("LogonDomain") private String logonDomain;
  @JsonProperty("DeviceType") private String deviceType;
  @JsonProperty("CreationMethod") private String creationMethod;
  @JsonProperty("PasswordChangeInProcess") private String passwordChangeInProcess;
}
