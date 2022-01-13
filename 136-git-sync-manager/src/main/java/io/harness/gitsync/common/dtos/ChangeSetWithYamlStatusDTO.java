/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.gitsync.common.dtos;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.ChangeSet;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(DX)
public class ChangeSetWithYamlStatusDTO {
  ChangeSet changeSet;
  YamlInputErrorType yamlInputErrorType;

  public enum YamlInputErrorType {
    NIL("NIL"),
    PROJECT_ORG_IDENTIFIER_MISSING("Project or Org missing"),
    INVALID_ENTITY_TYPE("Invalid Entity Type"),
    YAML_FROM_NOT_GIT_SYNCED_PROJECT("YAML from not git synced project"),
    ENTITY_NOT_FOUND("Entity not found");

    private String value;

    YamlInputErrorType(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }

    public static YamlInputErrorType fromValue(String value) {
      for (YamlInputErrorType errorType : YamlInputErrorType.values()) {
        if (errorType.value.equals(value)) {
          return errorType;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }
}
