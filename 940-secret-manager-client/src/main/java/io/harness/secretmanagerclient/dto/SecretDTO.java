/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.secretmanagerclient.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public abstract class SecretDTO {
  String account;
  String org;
  String project;
  String identifier;
  String secretManager;
  String name;
  List<String> tags;
  String description;

  public SecretDTO(String account, String org, String project, String identifier, String secretManager, String name,
      List<String> tags, String description) {
    this.account = account;
    this.org = org;
    this.project = project;
    this.identifier = identifier;
    this.secretManager = secretManager;
    this.name = name;
    this.tags = tags;
    this.description = description;
  }
}
