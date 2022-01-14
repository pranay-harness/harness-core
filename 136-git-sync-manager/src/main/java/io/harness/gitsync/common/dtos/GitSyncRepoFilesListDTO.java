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
@Schema(name = "GitSyncRepoFilesList", description = "This contains a list of repo files with a specific Module Type")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GitSyncRepoFilesListDTO {
  @Schema(description = "Module Type") private ModuleType moduleType;
  @Schema(description = "List of all the repo files specific to the given Module Type")
  private List<GitSyncRepoFilesDTO> gitSyncRepoFilesList;
}
