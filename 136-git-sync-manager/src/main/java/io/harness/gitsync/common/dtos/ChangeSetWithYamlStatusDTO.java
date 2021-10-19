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
    PROJECT_ORG_IDENTIFIER_MISSING,
    INVALID_ENTITY_TYPE,
    YAML_FROM_NOT_GIT_SYNCED_PROJECT
  }
}
