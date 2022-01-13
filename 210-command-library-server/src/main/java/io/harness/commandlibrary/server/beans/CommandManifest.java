/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.commandlibrary.server.beans;

import static org.apache.commons.lang3.StringUtils.strip;

import io.harness.commandlibrary.server.utils.JsonSerializable;

import java.util.Set;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;

@Data
@NoArgsConstructor
@FieldNameConstants(innerTypeName = "CommandManifestKeys")
public class CommandManifest implements JsonSerializable {
  String name;
  String version;
  String displayName;
  String description;
  String type;
  Set<String> tags;
  String repoUrl;

  @Builder
  public CommandManifest(String name, String version, String displayName, String description, String type,
      Set<String> tags, String repoUrl) {
    this.name = strip(name);
    this.version = strip(version);
    this.displayName = displayName;
    this.description = description;
    this.type = strip(type);
    this.tags = tags;
    this.repoUrl = repoUrl;
  }

  public void setName(String name) {
    this.name = strip(name);
  }

  public void setVersion(String version) {
    this.version = strip(version);
  }

  public void setType(String type) {
    this.type = strip(type);
  }
}
