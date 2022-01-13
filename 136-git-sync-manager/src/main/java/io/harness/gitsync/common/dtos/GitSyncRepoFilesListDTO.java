/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

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
