package io.harness.gitsync.common.dtos;

import io.harness.ModuleType;
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
@ApiModel("GitSyncRepoFilesList")
@FieldNameConstants(innerTypeName = "GitSyncConfigFilesListDTOKeys")
@Schema(name = "GitSyncRepoFilesList", description = "This contains a list of Repo Files with a specific ModuleType")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GitSyncRepoFilesListDTO {
  private ModuleType moduleType;
  private List<GitSyncRepoFilesDTO> gitSyncRepoFilesList;
}
