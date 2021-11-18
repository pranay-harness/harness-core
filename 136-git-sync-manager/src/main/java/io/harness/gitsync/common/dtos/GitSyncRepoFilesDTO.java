package io.harness.gitsync.common.dtos;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import io.swagger.annotations.ApiModel;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;

@OwnedBy(HarnessTeam.DX)
@Value
@Builder
@ApiModel("GitSyncRepoFiles")
@FieldNameConstants(innerTypeName = "GitSyncConfigFilesDTOKeys")
@Schema(name = "GitSyncRepoFiles", description = "This contains a list of Entities with specific Git Sync Config Id")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GitSyncRepoFilesDTO {
  String gitSyncConfigIdentifier;
  private List<GitSyncEntityListDTO> gitSyncEntityLists;
}
