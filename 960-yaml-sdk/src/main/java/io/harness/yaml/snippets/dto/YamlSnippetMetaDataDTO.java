/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.yaml.snippets.dto;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;

import io.swagger.annotations.ApiModel;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@ApiModel("YamlSnippetMetaData")
@OwnedBy(DX)
public class YamlSnippetMetaDataDTO {
  String name;
  String description;
  String version;
  /**
   * slug of name and version.
   */
  String identifier;
  List<String> tags;
  String iconTag;
}
