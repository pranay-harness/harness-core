/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
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
    NIL,
    WRONG_ENTITY_TYPE,
    PROJECT_ORG_IDENTIFIER_MISSING,
    INVALID_ENTITY_TYPE,
    YAML_FROM_NOT_GIT_SYNCED_PROJECT
  }
}
