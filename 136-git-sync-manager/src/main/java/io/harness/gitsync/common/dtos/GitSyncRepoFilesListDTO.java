/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.gitsync.common.dtos;

import io.harness.ModuleType;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import io.swagger.annotations.ApiModel;
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
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GitSyncRepoFilesListDTO {
  private ModuleType moduleType;
  private List<GitSyncRepoFilesDTO> gitSyncRepoFilesList;
}
