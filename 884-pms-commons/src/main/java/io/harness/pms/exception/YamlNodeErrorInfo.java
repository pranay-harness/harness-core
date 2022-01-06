/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.pms.exception;

import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlNode;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class YamlNodeErrorInfo {
  String identifier;
  String name;
  String type;

  public static YamlNodeErrorInfo fromField(YamlField field) {
    YamlNode node = field.getNode();
    return new YamlNodeErrorInfo(
        node == null ? null : node.getIdentifier(), field.getName(), node == null ? null : node.getType());
  }
}
